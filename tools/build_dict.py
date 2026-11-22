# -*- coding: utf-8 -*-
"""把 Rime 词库（雾凇拼音 / 白霜拼音）与小麥注音词库转换成输入法使用的二进制词库。

输出：
  pinyin.bin   全拼词库（section0 主词库，section1 简拼词库）
  zhuyin.bin   注音词库
  syllables.txt 出现的无声调音节表（供分词/模糊音使用）
  dict_stats.json 统计信息
"""
import json
import math
import os
import re
import struct

BASE = os.path.dirname(os.path.abspath(__file__))
SRC = os.environ.get("SY_DICT_SRC", "dicts")
OUT = os.path.join(BASE, "..", "app", "assets")
OUT = os.path.abspath(OUT)

CJK = re.compile(r"[\u3400-\u4dbf\u4e00-\u9fff\uf900-\ufaff\u3b00-\u3bff]")
BAD = re.compile(r"[A-Za-z0-9\s，。、；：？！“”‘’（）《》…—·,.;:?!()\[\]{}<>\"'`~@#$%^&*+=|\\/_-]")

TONE_MAP = {
    "ā": ("a", 1), "á": ("a", 2), "ǎ": ("a", 3), "à": ("a", 4),
    "ē": ("e", 1), "é": ("e", 2), "ě": ("e", 3), "è": ("e", 4),
    "ī": ("i", 1), "í": ("i", 2), "ǐ": ("i", 3), "ì": ("i", 4),
    "ō": ("o", 1), "ó": ("o", 2), "ǒ": ("o", 3), "ò": ("o", 4),
    "ū": ("u", 1), "ú": ("u", 2), "ǔ": ("u", 3), "ù": ("u", 4),
    "ǖ": ("v", 1), "ǘ": ("v", 2), "ǚ": ("v", 3), "ǜ": ("v", 4),
    "ü": ("v", 0), "ń": ("n", 2), "ň": ("n", 3), "ǹ": ("n", 4),
    "ḿ": ("m", 2),
}


def parse_syllable(raw):
    """返回 (无声调音节, 声调) ; 失败返回 None。"""
    tone = 0
    out = []
    for ch in raw:
        if ch in TONE_MAP:
            base, t = TONE_MAP[ch]
            out.append(base)
            if t:
                tone = t
        elif ch.isdigit():
            tone = int(ch)
        elif ch == "ˉ":
            tone = 1
        elif ch == "ˊ":
            tone = 2
        elif ch == "ˇ":
            tone = 3
        elif ch == "ˋ":
            tone = 4
        elif ch == "˙":
            tone = 5
        elif ch.isalpha() and ord(ch) < 128:
            out.append(ch.lower())
        elif ch in ("'", "-", " "):
            continue
        else:
            return None
    syl = "".join(out)
    if not syl or not re.match(r"^[a-z]+$", syl):
        return None
    return syl, tone


def read_rime_dict(path):
    """产出 (词, [(音节, 声调)] 或 None, 权重)。None 表示该行只有 text + weight，
    拼音需由单字表推导（如 tencent.dict.yaml）。"""
    started = False
    with open(path, "r", encoding="utf-8", errors="replace") as f:
        for line in f:
            line = line.rstrip("\r\n")
            if not started:
                if line.strip() == "...":
                    started = True
                continue
            if not line or line.startswith("#"):
                continue
            parts = line.split("\t")
            if len(parts) < 2:
                continue
            word = parts[0].strip()
            weight = 0.0
            pinyin = ""
            if len(parts) >= 3:
                pinyin = parts[1].strip()
                try:
                    weight = float(parts[2].strip())
                except ValueError:
                    weight = 0.0
            else:
                try:
                    weight = float(parts[1].strip())
                except ValueError:
                    pinyin = parts[1].strip()
            if not word:
                continue
            if BAD.search(word) or not CJK.search(word):
                continue
            if len(word) > 12:
                continue
            if not pinyin:
                yield word, None, weight
                continue
            syls = []
            for token in re.split(r"[\s']+", pinyin):
                if not token:
                    continue
                parsed = parse_syllable(token)
                if parsed is None:
                    syls = None
                    break
                syls.append(parsed)
            if not syls:
                continue
            yield word, syls, weight


def norm_score(weight, source, factors, per_file):
    """把各词典的原始权重折算到统一的 1-60000 分。"""
    scale, exp = factors.get(source, (0.0, 1.0))
    fmax = per_file.get(source + "#max", 0.0) or 1.0
    rel = min(1.0, float(weight) / fmax)
    if rel <= 0:
        return 1.0
    score = scale * (rel ** exp)
    if score < 1:
        score = 1.0
    elif score > 60000:
        score = 60000.0
    return score


def add_entry(table, key, tones, word, score, source, cat=None):
    if cat is None:
        cat = cat_for_word(word, source.rsplit("/", 1)[-1])
    old = table.get((key, word))
    if old is None:
        table[(key, word)] = (score, tones, source, cat)
        return
    old_cat = old[3]
    # 同一个词在多本词表里出现时只留一类：常用来源（主力）优先，不留混合位，
    # 这样设置页关掉某一类，真能干净地关掉
    if (old_cat & CAT_MAIN) == 0 and (cat & CAT_MAIN) != 0:
        new_cat = cat
    else:
        new_cat = old_cat
    if score > old[0]:
        table[(key, word)] = (score, tones, source, new_cat)
    else:
        table[(key, word)] = (old[0], old[1], old[2], new_cat)


# 词库分类：位掩码（一条词只落在一个类里），设置页可以逐类开关
# 1.0.48 起按「频率高低」划分：整张综合词库按原表综合权重从高到低排一次，
# 按名次分位切 5 档（高频 / 常用 / 一般 / 低频 / 长尾），分数也跟着名次重新分配。
CAT_HIGH = 1      # 高频：排名前 10%
CAT_COMMON = 2    # 常用：10%–30%
CAT_MID = 4       # 一般：30%–55%
CAT_LOW = 8       # 低频：55%–80%
CAT_TAIL = 16     # 长尾：80%–100%（成语和收尾的长尾词几乎都在这档）
CAT_ZHUYIN = 32   # 小麥注音
CAT_JP = 64       # Mozc 日语
CAT_ALL = 127
# 拼音各频级都算"主力"，拼音引擎统一加分（跟上一版相对排序保持一致）
CAT_MAIN = CAT_HIGH | CAT_COMMON | CAT_MID | CAT_LOW | CAT_TAIL

# 分档：分位上限 -> (分类位, 档首分数, 档尾分数)，档内按名次线性下降
FREQ_TIERS = (
    (0.10, CAT_HIGH, 60000.0, 40000.0),
    (0.30, CAT_COMMON, 40000.0, 26000.0),
    (0.55, CAT_MID, 26000.0, 15000.0),
    (0.80, CAT_LOW, 15000.0, 7000.0),
    (1.00, CAT_TAIL, 7000.0, 1000.0),
)


def freq_score(pos, total):
    """pos：按频率排好的名次（0 起）；total：词表总条数。返回 (分数, 分类位)。"""
    frac = (pos / float(total - 1)) if total > 1 else 0.0
    lo = 0.0
    for hi, cat, top, bottom in FREQ_TIERS:
        if frac <= hi or hi >= 1.0:
            inner = 0.0 if hi <= lo else (frac - lo) / (hi - lo)
            inner = min(1.0, max(0.0, inner))
            return top - (top - bottom) * inner, cat
        lo = hi
    return FREQ_TIERS[-1][3], FREQ_TIERS[-1][1]

# Mozc 全量 108 万条里只保留代价最低的常用部分（约前 30%）
JP_KEEP_RATIO = 0.30

CORE_SOURCES = {"8105.dict.yaml", "base.dict.yaml"}

# 《通用规范汉字表》8105 字补充层（tools/data/tghz2013.txt，由 gen_tghz.py 生成）
# 目的：白霜词库对个别规范字/读音有缺口（例如「呣」只落在生僻表里、被默认分类挡掉），
# 这一层把 8105 个规范字连同官方读音补齐，权重压得很低，不抢常用字的位次。
TGHZ_FILE = os.path.join(BASE, "data", "tghz2013.txt")
TGHZ_SCORE = {1: 500.0, 2: 360.0, 3: 250.0}
# 1.0.41 起全拼词库只剩《通用规范汉字表》，没有别的词表做权重参照，
# 于是用「字级 + 字序」给每个字一个连续分数：字级决定区间，字序在区间内递减，
# 保证同音字里字序靠前（笔画少、更基础）的字排在前面。
TGHZ_LEVEL = {1: (1000.0, 620.0, 0, 3500), 2: (520.0, 340.0, 3500, 3000), 3: (300.0, 160.0, 7000, 1105)}


def tghz_score(lvl, seq):
    hi, lo, off, total = TGHZ_LEVEL.get(lvl, TGHZ_LEVEL[3])
    idx = max(0, min(total - 1, seq - 1 - off))
    return hi - (hi - lo) * (idx / float(total - 1))


# 本地综合词库（TSV：词语 / 全拼 / 简拼 / 音节数 / 词长 / 综合权重 / 分类）
# 1.0.42 起它是全拼的主力词表：自带拼音与权重，直接入库，不做归一化。
USER_DICT = os.path.join(BASE, "data", "词库_输入法.tsv")


def cat_for_word(word, source_name):
    """没有频率名次可依时的兜底（rime 词表 / tghz 补充层）：一律算长尾。"""
    return CAT_TAIL


def file_factor(name, file_max):
    """不同词库权重口径差异很大：频率型（8105/base）是长尾计数分布，用平方根压缩，
    否则常用词全被挤到几百的低分；腾讯词向量是有规模但缺注音的补充词库，压到 0-8000，
    避免长尾词压过真正常用的词；覆盖型（ext/others/41448 等小权重表）压到 0-700。"""
    if file_max <= 0:
        return (0.0, 1.0)
    if name == "tencent.dict.yaml":
        return (8000.0 / file_max, 0.85)
    if name in ("8105.dict.yaml", "base.dict.yaml"):
        return (60000.0, 0.5)
    if name == "tghz2013":
        # 补充层：分数直接等于写入的原始分（rel = weight/60000，指数 1.0）
        return (60000.0, 1.0)
    if file_max > 500000:
        return (60000.0, 0.5)
    return (700.0 / file_max, 1.0)


def build_pinyin():
    files = []
    # 腾讯词向量（tencent）量最大也最杂，全是网络长尾，按需求从词库中剔除，只留日常词表
    # 1.0.39 起不再打包雾凇拼音（rime-ice，SRC/ice）
    # 1.0.41 起连白霜拼音（rime-frost）也不再打包，全拼词库只留《通用规范汉字表》8105 字
    for name in []:
        for src in []:
            p = os.path.join(SRC, src, name)
            if os.path.exists(p):
                files.append((src, name, p))

    table = {}
    total_rows = 0
    syllables = {}
    per_file = {}
    raw = []       # (key, tones, word, weight, source)
    pending = []   # 只有 text + weight 的词条，待单字表建立后补拼音
    for src, name, path in files:
        n = 0
        fmax = 0.0
        for word, syls, weight in read_rime_dict(path):
            if weight > fmax:
                fmax = weight
            n += 1
            if syls is None:
                pending.append((word, weight, f"{src}/{name}"))
                continue
            key = "".join(s for s, t in syls)
            tones = "".join(str(t if t else 1) for s, t in syls)
            raw.append((key, tones, word, weight, f"{src}/{name}"))
            for s, t in syls:
                syllables[s] = syllables.get(s, 0) + 1
        per_file[f"{src}/{name}"] = n
        total_rows += n
        per_file[f"{src}/{name}#max"] = fmax

    # 《通用规范汉字表》8105 字补充层放在词表合并之后做，只补空缺、只升分类，
    # 绝不参与权重比较——否则整层规范字会被同一个分数拉平，把「黄」这种高频字压下去
    tghz_added = 0
    tghz_promoted = 0

    # 单字读音表：取权重最高的读音
    char_map = {}
    char_best = {}
    for key, tones, word, weight, src in raw:
        if len(word) == 1 and len(tones) == 1:
            if weight > char_best.get(word, -1.0):
                char_best[word] = weight
                char_map[word] = (key, tones)

    resolved = 0
    for word, weight, src in pending:
        keys = []
        tos = []
        ok = True
        for ch in word:
            v = char_map.get(ch)
            if v is None:
                ok = False
                break
            keys.append(v[0])
            tos.append(v[1])
        if not ok:
            continue
        raw.append(("".join(keys), "".join(tos), word, weight, src))
        resolved += 1
    per_file["无注音词条解析"] = resolved

    # 每个文件各自的归一化系数（先算，词条合并时直接按归一化后的分数比大小）
    factors = {}
    for src, name, path in files:
        key = f"{src}/{name}"
        factors[key] = file_factor(name, per_file.get(key + "#max", 0.0))

    for key, tones, word, weight, source in raw:
        add_entry(table, key, tones, word, norm_score(weight, source, factors, per_file), source)
    del raw
    del pending

    # ---- 本地综合词库：全拼的主力词表 ----
    # 103,530 行（8105 单字 + 两字 / 三字 / 四字词语 + 成语 + 各行业小类），
    # 每行自带全拼与「综合权重」；1.0.48 起权重只用来排名次，分数与频级按名次重新分配。
    user_n = 0
    if os.path.exists(USER_DICT):
        tone_map = {}
        if os.path.exists(TGHZ_FILE):
            with open(TGHZ_FILE, "r", encoding="utf-8") as f:
                for line in f:
                    if not line.strip() or line.startswith("#"):
                        continue
                    parts = line.rstrip("\r\n").split("\t")
                    if len(parts) < 2:
                        continue
                    ch = parts[0].strip()
                    for token in parts[1].split(","):
                        token = token.strip()
                        if len(token) >= 2 and token[-1].isdigit():
                            tone_map.setdefault(ch, {})[token[:-1]] = token[-1]
        # 第一遍：把整张表读进来，按原「综合权重」排一次名次（同分按词长、词形定序，
        # 保证每次构建结果一致），名次分位决定分数和频级，原表分数本身不再直接用。
        rows = []
        with open(USER_DICT, "r", encoding="utf-8") as f:
            f.readline()   # 表头
            for line in f:
                parts = line.rstrip("\r\n").split("\t")
                if len(parts) < 6:
                    continue
                word = parts[0].strip()
                py = parts[1].strip()
                if not word or not py:
                    continue
                syls = [s for s in py.split(" ") if s]
                if not syls or not all(s.isalpha() for s in syls):
                    continue
                if len(syls) != len(word) and len(word) > 1:
                    continue
                try:
                    w = float(parts[5])
                except ValueError:
                    continue
                rows.append((word, syls, w))
        total = len(rows)
        order = sorted(range(total), key=lambda k: (-rows[k][2], len(rows[k][1]), rows[k][0], "".join(rows[k][1])))
        pos_of = [0] * total
        for rank, idx in enumerate(order):
            pos_of[idx] = rank
        for idx, (word, syls, _w) in enumerate(rows):
            score, cat = freq_score(pos_of[idx], total)
            key = "".join(syls)
            tones = "".join(tone_map.get(word[i], {}).get(syls[i], "1")
                            for i in range(len(syls)))
            add_entry(table, key, tones, word, score, "user/词库_输入法", cat)
            for s in syls:
                syllables[s] = syllables.get(s, 0) + 1
            user_n += 1
        per_file["词库_输入法.tsv"] = user_n
        total_rows += user_n
    # 《通用规范汉字表》8105 字补充层：只往里补，不跟已有词比权重
    # ① 词库里根本没有这个字/读音 -> 用低分补一条（归到「常用单字」，默认设置下可见）
    # ② 字/读音有，但被分到「生僻单字」（ext / 41448 来的）-> 只把分类提到「常用单字」，分数原样不动
    if os.path.exists(TGHZ_FILE):
        with open(TGHZ_FILE, "r", encoding="utf-8") as f:
            for line in f:
                if not line.strip() or line.startswith("#"):
                    continue
                parts = line.rstrip("\r\n").split("\t")
                if len(parts) < 3:
                    continue
                ch = parts[0].strip()
                lvl = int(parts[2]) if parts[2].strip().isdigit() else 3
                seq = int(parts[3]) if len(parts) > 3 and parts[3].strip().isdigit() else 1
                sc = tghz_score(lvl, seq)
                for token in parts[1].split(","):
                    token = token.strip()
                    if len(token) < 2 or not token[-1].isdigit():
                        continue
                    syl, tone = token[:-1], token[-1]
                    if not syl.isalpha():
                        continue
                    hit = table.get((syl, ch))
                    if hit is None:
                        # 词库里完全没有的字/读音：按最低一档补一条，默认（全开）下可见
                        table[(syl, ch)] = (sc, tone, "tghz2013", CAT_TAIL)
                        syllables[syl] = syllables.get(syl, 0) + 1
                        tghz_added += 1
        per_file["tghz2013"] = tghz_added
        per_file["tghz2013·升为常用"] = tghz_promoted
        total_rows += tghz_added

    main = {}
    for (key, word), (score, tones, source, cat) in table.items():
        main.setdefault(key, []).append((word, int(score), tones, cat))

    for k in [k for k in per_file if k.endswith("#max")]:
        del per_file[k]

    max_w = 60000
    return main, per_file, total_rows, syllables, max_w


def prepare_abbr_index(syllables):
    """仅用于简拼构建：用音节集合贪心切分连写拼音。"""
    global _SYL_SET
    _SYL_SET = set(syllables)
    return


def segment_key(key):
    parts = []
    i = 0
    while i < len(key):
        for ln in range(min(6, len(key) - i), 0, -1):
            c = key[i:i + ln]
            if c in _SYL_SET:
                parts.append(c)
                i += ln
                break
        else:
            return None
    return parts


def write_bin(path, sections):
    """sections: list of key_map ; key_map: key -> [(word, weight, tones, cat)]"""
    with open(path, "wb") as f:
        f.write(struct.pack("<iii", 0x53594431, 3, len(sections)))
        for key_map in sections:
            keys = sorted(key_map.keys(), key=lambda k: k.encode("utf-8"))
            key_block = bytearray()
            word_index = []
            word_block = bytearray()
            dir_entries = bytearray()
            for k in keys:
                entries = sorted(key_map[k], key=lambda e: -e[1])
                kb = k.encode("utf-8")
                dir_entries += struct.pack("<ihhi", len(key_block), len(kb), len(entries), len(word_index))
                key_block += kb
                for item in entries:
                    word, weight, tones, cat = item
                    wb = word.encode("utf-8")
                    tb = tones.encode("ascii")
                    word_index.append(len(word_block))
                    word_block += struct.pack("<IBBH", weight, len(tb), len(wb), cat & 0xFFFF) + wb + tb
            total_words = len(word_index)
            f.write(struct.pack("<iiii", len(keys), total_words, len(key_block), len(word_block)))
            f.write(dir_entries)
            f.write(key_block)
            for off in word_index:
                f.write(struct.pack("<i", off))
            f.write(word_block)
    return os.path.getsize(path)


BOPO = re.compile(r"^[\u3105-\u312f\u31a0-\u31bf]+$")
TONE_MARKS = {"ˉ": 1, "ˊ": 2, "ˇ": 3, "ˋ": 4, "˙": 5}


def parse_bpmf_token(tok):
    """注音音节 -> (无声调ㄅㄆㄇ串, 声调)；失败返回 None。"""
    tone = 0
    body = tok
    while body and body[-1] in TONE_MARKS:
        tone = TONE_MARKS[body[-1]]
        body = body[:-1]
    if not body or not BOPO.match(body):
        return None
    return body, tone or 1


def parse_bpmf(rest):
    """把注音音节序列解析成 [(无声调串, 声调)]，遇到非注音字段即停。"""
    syls = []
    for tok in rest:
        if tok in TONE_MARKS:
            if syls:
                syls[-1] = (syls[-1][0], TONE_MARKS[tok])
            continue
        parsed = parse_bpmf_token(tok)
        if parsed is None:
            break
        syls.append(parsed)
    return syls


def build_zhuyin():
    """读取小麥注音词库：BPMFMappings（词）+ BPMFBase（单字）+ phrase.occ（字频）。"""
    word_table = {}
    occ = {}
    occ_path = os.path.join(SRC, "mcbopomofo", "phrase.occ")
    if os.path.exists(occ_path):
        with open(occ_path, "r", encoding="utf-8", errors="replace") as f:
            for line in f:
                parts = line.rstrip("\r\n").split()
                if len(parts) < 2:
                    continue
                try:
                    occ[parts[0]] = float(parts[-1])
                except ValueError:
                    continue
    max_occ = max([v for v in occ.values()] or [1.0])

    def word_rel(word):
        logs = []
        for ch in word:
            r = occ.get(ch, 0.0) / max_occ
            logs.append(math.log(max(r, 0.012)))
        if not logs:
            return 0.0
        return math.exp(sum(logs) / len(logs))

    def load(path):
        n = 0
        with open(path, "r", encoding="utf-8", errors="replace") as f:
            for line in f:
                line = line.rstrip("\r\n")
                if not line or line.startswith("#"):
                    continue
                parts = line.split(" ")
                word = parts[0]
                rest = [p for p in parts[1:] if p]
                if not word or not rest:
                    continue
                if BAD.search(word) or not CJK.search(word) or len(word) > 12:
                    continue
                syls = parse_bpmf(rest)
                if not syls:
                    continue
                key = "".join(s for s, t in syls)
                tones = "".join(str(t) for s, t in syls)
                rel = word_rel(word)
                score = int(round(60000.0 * pow(rel, 0.85)))
                if len(word) > 1:
                    score = int(round(score * 0.8))
                if score < 1:
                    score = 1
                elif score > 60000:
                    score = 60000
                old = word_table.get((key, word))
                if old is None or score > old[0]:
                    word_table[(key, word)] = (score, tones)
                n += 1
        return n

    base_n = load(os.path.join(SRC, "mcbopomofo", "BPMFBase.txt"))
    map_n = load(os.path.join(SRC, "mcbopomofo", "BPMFMappings.txt"))

    main = {}
    for (key, word), (score, tones) in word_table.items():
        main.setdefault(key, []).append((word, score, tones, CAT_ZHUYIN))
    return main, {"BPMFBase.txt": base_n, "BPMFMappings.txt": map_n}, len(word_table)


KANA_RE = re.compile("^[\u3041-\u3096\u30fc]+$")


def kata_to_hira(s):
    return "".join(chr(ord(c) - 0x60) if "\u30a1" <= c <= "\u30f6" else c for c in s)


def build_japanese():
    """读取 Mozc 开源词典（google/mozc src/data/dictionary_oss/dictionary0*.txt）。

    每行 5 列：读音\\t左 ID\\t右 ID\\t代价\\t表记。代价越低越常用。
    全量 108 万条里大量是人名地名与复合词，这里只保留出现代价最低的常用部分
    （约前 30%），日常输入完全够用，体积也能压下来一大截。
    """
    src = os.path.join(SRC, "mozc")
    table = {}
    rows = 0
    files = []
    if os.path.isdir(src):
        for name in sorted(os.listdir(src)):
            if not name.startswith("dictionary") or not name.endswith(".txt"):
                continue
            files.append(name)
            with open(os.path.join(src, name), "r", encoding="utf-8", errors="replace") as f:
                for line in f:
                    line = line.rstrip("\r\n")
                    if not line:
                        continue
                    parts = line.split("\t")
                    if len(parts) < 5:
                        continue
                    reading = kata_to_hira(parts[0])
                    word = parts[-1]
                    if not word or not KANA_RE.match(reading):
                        continue
                    try:
                        cost = int(parts[-2])
                    except ValueError:
                        continue
                    rows += 1
                    weight = max(1, 60000 - cost * 3)
                    cur = table.get((reading, word))
                    if cur is None or weight > cur:
                        table[(reading, word)] = weight

    ranked = sorted(table.items(), key=lambda kv: kv[1], reverse=True)
    keep = max(1, int(len(ranked) * JP_KEEP_RATIO))
    if keep < len(ranked):
        # 同一代价的词条一起保留，避免在同一分数段里被随机截断
        floor = ranked[keep - 1][1]
        while keep < len(ranked) and ranked[keep][1] == floor:
            keep += 1

    main = {}
    for (key, word), weight in ranked[:keep]:
        main.setdefault(key, []).append((word, weight, "", CAT_JP))
    meta = {"rows_scanned": rows, "files": files, "total_entries": len(table),
            "kept_entries": keep, "keep_ratio": round(keep / float(len(table) or 1), 3)}
    return main, meta, keep


def build_assoc(main_map, per_char=6):
    """联想词索引：按首字分组，优先常用/基础/扩展词，其次权重，最后词长。"""
    groups = {}
    for key, entries in main_map.items():
        for word, score, tones, cat in entries:
            if len(word) < 2 or len(word) > 4:
                continue
            ch = word[0]
            cur = groups.get(ch)
            if cur is None:
                cur = {}
                groups[ch] = cur
            if score > cur.get(word, (0, 0))[0]:
                cur[word] = (score, cat)

    def rank(item):
        word, (score, cat) = item
        long_tail = 1 if (cat & CAT_TAIL) else 0
        return (long_tail, -(score - (len(word) - 2) * 3000))

    assoc = {}
    for ch, words in groups.items():
        best = sorted(words.items(), key=rank)[:per_char]
        assoc[ch] = [(w, s, "", CAT_ALL) for w, (s, c) in best]
    return assoc


def main():
    os.makedirs(OUT, exist_ok=True)
    main_map, per_file, rows, syllables, max_w = build_pinyin()
    prepare_abbr_index(syllables)
    # 重新构建简拼（依赖音节表切分）
    abbr = {}
    for key in main_map:
        parts = segment_key(key)
        if not parts or len(parts) < 2:
            continue
        ak = "".join(p[0] for p in parts)
        if ak == key:
            continue
        for word, score, tones, cat in main_map[key]:
            old = abbr.get((ak, word))
            if old is None or score > old[0]:
                abbr[(ak, word)] = (score, tones, key, cat)
    abbr_final = {}
    for (ak, word), (score, tones, key, cat) in abbr.items():
        abbr_final.setdefault(ak, []).append((word, score, tones, cat))

    assoc = build_assoc(main_map)
    pinyin_size = write_bin(os.path.join(OUT, "pinyin.bin"), [main_map, abbr_final, assoc])

    zy_main, zy_per_file, zy_rows = build_zhuyin()
    zhuyin_size = write_bin(os.path.join(OUT, "zhuyin.bin"), [zy_main])

    jp_main, jp_meta, jp_rows = build_japanese()
    jp_size = 0
    if jp_main:
        jp_size = write_bin(os.path.join(OUT, "japanese.bin"), [jp_main])

    with open(os.path.join(OUT, "syllables.txt"), "w", encoding="utf-8") as f:
        for s in sorted(syllables.keys()):
            f.write(s + "\n")

    cat_counts = {}
    cat_names = ((CAT_HIGH, "高频"), (CAT_COMMON, "常用"), (CAT_MID, "一般"),
                 (CAT_LOW, "低频"), (CAT_TAIL, "长尾"),
                 (CAT_ZHUYIN, "注音"), (CAT_JP, "日语"))
    for entries in main_map.values():
        for word, score, tones, cat in entries:
            for bit, name in cat_names:
                if cat & bit:
                    cat_counts[name] = cat_counts.get(name, 0) + 1

    stats = {
        "pinyin": {
            "entries_unique": sum(len(v) for v in main_map.values()),
            "rows_scanned": rows,
            "keys": len(main_map),
            "abbr_keys": len(abbr_final),
            "assoc_keys": len(assoc),
            "bytes": pinyin_size,
            "syllables": len(syllables),
            "per_file": per_file,
            "max_weight": max_w,
            "categories": cat_counts,
        },
        "zhuyin": {
            "entries_unique": zy_rows,
            "keys": len(zy_main),
            "bytes": zhuyin_size,
            "per_file": zy_per_file,
        },
    }
    if jp_main:
        stats["japanese"] = {
            "entries_unique": jp_rows,
            "keys": len(jp_main),
            "bytes": jp_size,
            "rows_scanned": jp_meta["rows_scanned"],
            "files": len(jp_meta["files"]),
            "total_entries": jp_meta["total_entries"],
            "keep_ratio": jp_meta["keep_ratio"],
        }
    with open(os.path.join(OUT, "dict_stats.json"), "w", encoding="utf-8") as f:
        json.dump(stats, f, ensure_ascii=False, indent=2)
    print(json.dumps(stats, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()

"""生成笔画输入词库 stroke.bin（逐笔前缀匹配，只收单字）。

笔画分五类：1 横、2 竖、3 撇、4 捺、5 折（竖弯钩一类）。
每个汉字取前 5 笔作为编码；笔画方案只收单字，逐笔点下去候选就是一个个汉字，
多字词交给联想与拼音方案，避免"三十""一二三四"这类词条混进笔画候选。
编码来源是 rime 五笔画字表（h 横 s 竖 p 撇 n 捺 z 折，源自 CNS11643 全字库），
几何推断仅作为字表缺失时的兜底。词表复用 pinyin.bin 的字频与分类位。
"""

import json
import math
import os
import struct
import sys

BASE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.abspath(os.path.join(BASE, "..", "app", "assets"))
GRAPHICS = os.environ.get("SY_STROKE_GRAPHICS", os.path.join("dicts", "stroke", "graphics.txt"))
STROKE_YAML = os.environ.get("SY_STROKE_YAML", os.path.join("dicts", "stroke", "stroke.dict.yaml"))

CAT_CHAR_COMMON = 1
CAT_WORD2 = 4
CAT_WORD3 = 8
CAT_WORD4 = 16
CAT_WORD_LONG = 32
CAT_EXT_WORD = 64
# 常用来源出的词（主力），笔画优先收这些
CAT_MAIN = CAT_CHAR_COMMON | CAT_WORD2 | CAT_WORD3 | CAT_WORD4 | CAT_WORD_LONG

PER_CHAR = 8           # 每字最多取几笔（8 笔足够区分常用字，再多收益很小）
MAX_WORD_LEN = 1       # 笔画方案只收单字：逐笔输入的候选就是一个个汉字
MAX_PHRASES = 0        # 不生成多字词条（多字词由联想/拼音方案负责）


def classify(median):
    """判定笔画类别。

    以起笔到收笔的连线为主轴：弧形的撇、捺始终偏向主轴一侧，属于直笔；
    横折、竖弯钩这类会在主轴两侧换向，或者沿主轴倒退回勾，才判为折。
    """
    if not median or len(median) < 2:
        return 5
    pts = median
    p0, p1 = pts[0], pts[-1]
    ax, ay = p1[0] - p0[0], p1[1] - p0[1]
    length = math.hypot(ax, ay)
    if length < 1e-6:
        return 5
    ux, uy = ax / length, ay / length
    nx, ny = -uy, ux

    side = 0
    back = 0.0
    prev_proj = -1e9
    for i in range(1, len(pts)):
        vx = pts[i][0] - p0[0]
        vy = pts[i][1] - p0[1]
        proj = vx * ux + vy * uy
        dev = vx * nx + vy * ny
        if abs(dev) > 3:
            s = 1 if dev > 0 else -1
            if side == 0:
                side = s
            elif s != side:
                side = 99
        if proj < prev_proj - 2:
            back += prev_proj - proj
        if proj > prev_proj:
            prev_proj = proj
    if side == 99 or back > 0.14 * length:
        return 5

    if abs(uy) < 0.34:
        return 1                     # 横
    if abs(ux) < 0.34:
        return 2                     # 竖
    if ux < 0 and uy > 0:
        return 3                     # 撇（向左下）
    if ux > 0 and uy > 0:
        return 4                     # 捺（向右下）
    return 5                          # 提、钩等


MAP = str.maketrans({"h": "1", "s": "2", "p": "3", "n": "4", "z": "5"})


def load_strokes():
    """读取 rime 五笔画字表（h 横 s 竖 p 撇 n 捺 z 折，源自 CNS11643 全字库）。"""
    table = {}
    if os.path.exists(STROKE_YAML):
        started = False
        with open(STROKE_YAML, "r", encoding="utf-8", errors="replace") as f:
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
                ch, code = parts[0], parts[1]
                if len(ch) != 1 or not code:
                    continue
                digits = code.translate(MAP)
                if digits.strip("12345"):
                    continue
                table[ch] = digits
        return table, len(table)
    return load_median_strokes()


def load_median_strokes():
    """兜底：从 makemeahanzi 中线几何推断笔画类别（精度较差）。"""
    table = {}
    total = 0
    with open(GRAPHICS, "r", encoding="utf-8", errors="replace") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            total += 1
            try:
                d = json.loads(line)
            except ValueError:
                continue
            ch = d.get("character")
            medians = d.get("medians") or []
            if not ch or len(ch) != 1:
                continue
            codes = "".join(str(classify(m)) for m in medians[:PER_CHAR])
            if codes:
                table[ch] = codes
    return table, total


def read_bin_section0(path):
    """解析 pinyin.bin 的第一个 section，返回 [(word, weight, cat)]。"""
    with open(path, "rb") as f:
        data = f.read()
    magic, ver, nsec = struct.unpack_from("<iii", data, 0)
    if magic != 0x53594431 or ver not in (2, 3):
        raise ValueError("bad dict: %s" % path)
    wide = ver >= 3
    pos = 12
    secs = []
    for _ in range(nsec):
        kc, wc, kbl, wbl = struct.unpack_from("<iiii", data, pos)
        pos += 16
        dir_off = pos
        pos += kc * 12
        kb_off = pos
        pos += kbl
        wi_off = pos
        pos += wc * 4
        wb_off = pos
        pos += wbl
        secs.append((kc, wc, dir_off, kb_off, wi_off, wb_off))
    kc, wc, dir_off, kb_off, wi_off, wb_off = secs[0]
    out = []
    for i in range(kc):
        _koff, _klen, nwords, widx = struct.unpack_from("<ihhi", data, dir_off + i * 12)
        for j in range(nwords):
            rel = struct.unpack_from("<i", data, wi_off + (widx + j) * 4)[0]
            at = wb_off + rel
            weight = struct.unpack_from("<I", data, at)[0]
            tl = data[at + 4]
            wl = data[at + 5]
            if wide:
                cat = struct.unpack_from("<H", data, at + 6)[0]
                word = data[at + 8:at + 8 + wl].decode("utf-8", "replace")
            else:
                cat = data[at + 6]
                word = data[at + 7:at + 7 + wl].decode("utf-8", "replace")
            out.append((word, weight, cat))
    return out


def write_bin(path, key_map):
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
        for word, weight, tones, cat in entries:
            wb = word.encode("utf-8")
            tb = tones.encode("ascii")
            word_index.append(len(word_block))
            word_block += struct.pack("<IBBH", weight, len(tb), len(wb), cat & 0xFFFF) + wb + tb
    with open(path, "wb") as f:
        f.write(struct.pack("<iii", 0x53594431, 3, 1))
        f.write(struct.pack("<iiii", len(keys), len(word_index), len(key_block), len(word_block)))
        f.write(dir_entries)
        f.write(key_block)
        for off in word_index:
            f.write(struct.pack("<i", off))
        f.write(word_block)
    return os.path.getsize(path)


def probe(table):
    samples = "一丨十人了口日田小大女山中月水火山石木九几乙儿心必永手用门问间"
    for ch in samples:
        print("%s:%s" % (ch, table.get(ch, "?")), end="  ")
    print()
    names = {"1": "横", "2": "竖", "3": "撇", "4": "捺", "5": "折"}
    for ch in "一十人口大木水手用门":
        c = table.get(ch, "")
        print(ch, c, [names[d] for d in c])


def main():
    table, total = load_strokes()
    print("stroke table lines=%d, usable chars=%d" % (total, len(table)))
    if "--probe" in sys.argv:
        probe(table)
        return

    pinyin = os.path.join(OUT, "pinyin.bin")
    entries = read_bin_section0(pinyin)
    print("pinyin entries=%d" % len(entries))

    if "--common" in sys.argv:
        # 常用字的笔顺全量打印，便于人工核对
        order = {}
        for word, weight, cat in entries:
            if len(word) != 1 or not (cat & CAT_CHAR_COMMON):
                continue
            if weight > order.get(word, -1):
                order[word] = weight
        ranked = sorted(order.items(), key=lambda kv: -kv[1])[:200]
        for word, weight in ranked:
            print("%s\t%s\t%d" % (word, table.get(word, "?"), weight))
        return

    best = {}
    for word, weight, cat in entries:
        if len(word) > MAX_WORD_LEN:
            continue
        old = best.get(word)
        if old is None or weight > old[0]:
            best[word] = (weight, cat)
    print("unique words=%d" % len(best))

    def code_of(word):
        parts = []
        for ch in word:
            c = table.get(ch)
            if not c:
                return None
            parts.append(c[:PER_CHAR])
        return "".join(parts)

    singles = []
    phrases = []
    missing = 0
    for word, (weight, cat) in best.items():
        code = code_of(word)
        if not code:
            missing += 1
            continue
        (singles if len(word) == 1 else phrases).append((code, word, weight, cat))
    print("singles=%d phrases=%d missing=%d" % (len(singles), len(phrases), missing))

    # 多字词只要常用/基础词，按权重取前 N 条
    phrases.sort(key=lambda e: -e[2])
    kept = [p for p in phrases if p[3] & CAT_MAIN]
    if len(kept) < MAX_PHRASES:
        kept += [p for p in phrases if not (p[3] & CAT_MAIN)]
    kept = kept[:MAX_PHRASES]

    key_map = {}
    for code, word, weight, cat in singles + kept:
        key_map.setdefault(code, []).append((word, weight, "", cat))

    # 短前缀物化：1~4 位前缀各写入该前缀下字频最高的 40 个字。
    # 逐笔输入刚开始几笔时直接命中这些键，不用靠字典序遍历，避免"点一撇全是金字旁"。
    by_prefix = {}
    for code, word, weight, cat in singles:
        for n in (1, 2, 3, 4):
            if len(code) >= n:
                by_prefix.setdefault(code[:n], []).append((weight, word, cat))
    for p, lst in by_prefix.items():
        lst.sort(key=lambda e: -e[0])
        for weight, word, cat in lst[:40]:
            key_map.setdefault(p, []).append((word, weight, "", cat))

    # 同一个键下同一个字只留一条
    for k in list(key_map.keys()):
        seen = set()
        dedup = []
        for e in sorted(key_map[k], key=lambda e: -e[1]):
            if e[0] in seen:
                continue
            seen.add(e[0])
            dedup.append(e)
        key_map[k] = dedup

    size = write_bin(os.path.join(OUT, "stroke.bin"), key_map)
    print("stroke.bin keys=%d words=%d size=%.2f MB" % (len(key_map), len(singles) + len(kept), size / 1048576.0))
    print("chars used=%d / %d" % (sum(1 for ch in table), len(table)))


if __name__ == "__main__":
    main()

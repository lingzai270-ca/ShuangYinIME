# -*- coding: utf-8 -*-
"""重写 zhuyin.bin 的词条权重与声调。

小麥注音原始数据里，同一音节下的单字权重完全一样（1398），排序只剩字典序，
于是「ㄏㄨㄤ」下「荒 慌 肓 衁」排在「黃」「黄」前面（第 31 / 77 位），引擎取 12 条就取不到。

这里用 pinyin.bin 的字频（reading_pin.txt 的行序即字频降序）重算权重，
并用带调拼音给出每个字的声调，让声调筛选真正起作用。

用法：python fix_zhuyin.py
输出：app/assets/zhuyin.bin（原地替换），tools/fix_zhuyin.log
"""

import io
import os
import struct
import sys

MAGIC = 0x53594431
# 注音词库的分类位（与 Prefs.CAT_ZHUYIN 一致）
ZHY_CAT = 128


def sections_of(data):
    magic, ver, n = struct.unpack_from("<iii", data, 0)
    if magic != MAGIC:
        raise SystemExit("bad magic")
    pos = 12
    secs = []
    for _ in range(n):
        kc, wc, kblen, wblen = struct.unpack_from("<iiii", data, pos)
        pos += 16
        dir_off = pos
        pos += kc * 12
        key_off = pos
        pos += kblen
        widx_off = pos
        pos += wc * 4
        wblk_off = pos
        pos += wblen
        secs.append(dict(kc=kc, wc=wc, dirOff=dir_off, keyOff=key_off,
                         widxOff=widx_off, wblkOff=wblk_off))
    return secs


def load_ranks(path):
    """字 -> (排名, 声调数字)。表是按字频降序写的，行号就是排名。"""
    ranks = {}
    rank = 0
    with io.open(path, encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\n")
            if "\t" not in line:
                continue
            ch, py = line.split("\t", 1)
            tone = 5
            if py and py[-1] in "1234":
                tone = int(py[-1])
            elif py:  # 带调符号的拼音，按符号反查
                for t, marks in enumerate("āáǎà", start=1):
                    if marks in py:
                        tone = t
                        break
                for t, marks in enumerate("ōóǒò", start=1):
                    if marks in py:
                        tone = t
                        break
                for t, marks in enumerate("ēéěè", start=1):
                    if marks in py:
                        tone = t
                        break
                for t, marks in enumerate("īíǐì", start=1):
                    if marks in py:
                        tone = t
                        break
                for t, marks in enumerate("ūúǔù", start=1):
                    if marks in py:
                        tone = t
                        break
                for t, marks in enumerate("ǖǘǚǜ", start=1):
                    if marks in py:
                        tone = t
                        break
            ranks[ch] = (rank, tone)
            rank += 1
    return ranks


def add_traditional(ranks):
    """繁体字继承简体字的字频：注音方案输出的是繁体字（黃色、學生），
    若不归一，『黃』会因字频低被『荒』『慌』压住。"""
    try:
        import opencc
        cc = opencc.OpenCC("t2s")
    except Exception:
        print("opencc 不可用，跳过简繁归一")
        return ranks
    added = 0
    for ch in list(ranks.keys()):
        r, tone = ranks[ch]
        try:
            simple = cc.convert(ch)
        except Exception:
            continue
        if len(simple) == 1 and simple != ch and simple in ranks:
            if ch not in ranks or ranks[ch][0] > ranks[simple][0]:
                ranks[ch] = (ranks[simple][0], ranks[simple][1])
                added += 1
    print("简繁归一 %d 字" % added)
    return ranks


def load_syllables():
    """规范注音音节集合（声母+韵母 / 整体认读音节），用于校验词库键拼写是否合法。"""
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    path = os.path.join(root, "app", "assets", "syllables.txt")
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    from build_readings import py_to_zhuyin
    out = set()
    with io.open(path, encoding="utf-8") as f:
        for line in f:
            py = line.strip()
            if not py:
                continue
            z = py_to_zhuyin(py + "5")
            if not z:
                continue
            for m in "ˉˊˇˋ˙":
                z = z.replace(m, "")
            out.add(z)
    return out


def split_syllables(key, syllables, max_len=3):
    """按最长匹配切分注音键，返回 (完整音节数, 是否有不完整尾巴)。"""
    i = 0
    n = len(key)
    count = 0
    while i < n:
        matched = 0
        for L in range(max_len, 0, -1):
            if i + L <= n and key[i:i + L] in syllables:
                matched = L
                break
        if matched == 0:
            return count, True
        count += 1
        i += matched
    return count, False


def weight_of(rank):
    """单字权重：1000~5000，按字频平滑递减。

    区间特意压窄，让引擎的音节数/声调加权能压过它；
    同时把生僻字（rank 上万）压到 1100 以下，绝不会挤掉常用字。
    """
    return 1000 + int(4000.0 * 300.0 / (rank + 300))


def phrase_weight(min_rank, length):
    """多字词权重：300~1200。字数越多越低——长词更少见，不该靠字数占便宜。"""
    base = 300 + int(900.0 * 2000.0 / (min_rank + 2000))
    return max(120, base - (length - 2) * 90)


def make_zhy_checker():
    """返回 ch -> {注音音节: 读音序号}。序号 0 是最常用读音，越大越生僻。
    多音字全部收录，但罕见异读（如「离」的 chī）要降权，否则会挤掉正音字。"""
    sys.path.insert(0, os.environ.get("SY_PKGS", "pkgs"))
    from pypinyin import pinyin, Style
    from build_readings import py_to_zhuyin

    cache = {}

    def check(ch):
        if ch in cache:
            return cache[ch]
        try:
            res = pinyin(ch, heteronym=True, style=Style.NORMAL)
            readings = res[0] if res else []
        except Exception:
            readings = []
        out = {}
        for idx, p in enumerate(readings):
            if not p:
                continue
            z = py_to_zhuyin(p + "5")
            if not z:
                continue
            for m in "ˉˊˇˋ˙":
                z = z.replace(m, "")
            if z not in out:
                out[z] = idx
        cache[ch] = out
        return out

    return check


def main():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    assets = os.path.join(root, "app", "assets")
    ranks = load_ranks(os.path.join(assets, "reading_pin.txt"))
    print("rank table=%d" % len(ranks))
    ranks = add_traditional(ranks)

    t2s = None
    try:
        import opencc
        t2s = opencc.OpenCC("t2s")
    except Exception:
        pass

    def lookup(ch):
        r = ranks.get(ch)
        if r is not None or t2s is None:
            return r
        try:
            simple = t2s.convert(ch)
        except Exception:
            return None
        if len(simple) == 1:
            return ranks.get(simple)
        return None

    with open(os.path.join(assets, "zhuyin.bin"), "rb") as f:
        data = f.read()
    ver = struct.unpack_from("<i", data, 4)[0]
    wide = ver >= 3
    s = sections_of(data)[0]

    syllables = load_syllables()
    print("规范音节=%d" % len(syllables))
    zhy_check = make_zhy_checker()

    # 读出全部 key -> 词条
    keys = []
    for i in range(s["kc"]):
        ko, klen, wcnt, widx = struct.unpack_from("<ihhi", data, s["dirOff"] + i * 12)
        key = data[s["keyOff"] + ko: s["keyOff"] + ko + klen].decode("utf-8")
        entries = []
        for j in range(wcnt):
            rel = struct.unpack_from("<i", data, s["widxOff"] + (widx + j) * 4)[0]
            a = s["wblkOff"] + rel
            if wide:
                weight, tlen, wlen, cat = struct.unpack_from("<IBBH", data, a)
                word = data[a + 8: a + 8 + wlen].decode("utf-8")
            else:
                weight, tlen, wlen, cat = struct.unpack_from("<IBBB", data, a)
                word = data[a + 7: a + 7 + wlen].decode("utf-8")
            entries.append([word, weight, cat])
        keys.append([key, entries])

    changed = 0
    missing = 0
    bad_key = 0
    bad_key_words = 0
    bad_tone = 0
    misplaced = 0
    redundant = 0
    misplaced_keys = 0

    # 先扫一遍：哪些 (键, 单字) 已经在库里（用来判断某处是多音字正条还是冗余错位）
    present = set()
    chars = set()
    for key, entries in keys:
        for e in entries:
            if len(e[0]) == 1:
                present.add((key, e[0]))
                chars.add(e[0])

    # 补齐缺失的正音条目：某字的规范首读音若在库里没有对应条目，
    # 说明小麥词库这条漏了（典型如「离」只有 ㄔ 的错位条目），按规则补一条。
    key_entries = {}
    for k, ents in keys:
        key_entries[k] = ents
    added = 0
    for ch in chars:
        scores = zhy_check(ch)
        if not scores:
            continue
        first_key = None
        for z, i2 in scores.items():
            if i2 == 0:
                first_key = z
                break
        if first_key is None or (first_key, ch) in present:
            continue
        r = lookup(ch)
        if r is None:
            continue
        ents = key_entries.get(first_key)
        if ents is None:
            ents = []
            keys.append([first_key, ents])
            key_entries[first_key] = ents
        ents.append([ch, weight_of(r[0]), ZHY_CAT])
        present.add((first_key, ch))
        added += 1

    for key, entries in keys:
        # 拼写规则校验：键必须能切成完整音节（声母+韵母 / 整体认读音节）
        syl, tail = split_syllables(key, syllables)
        legal = (syl > 0 and not tail)
        if not legal:
            bad_key += 1
            bad_key_words += len(entries)
        # 单字错位校验：字的规范读音必须能拼出这个键
        single = [e for e in entries if len(e[0]) == 1]
        single_ok = 0
        for word, weight, cat in single:
            zs = zhy_check(word)
            if not zs or key in zs:
                single_ok += 1
        trust = True if not single else (single_ok * 3 >= len(single))
        if not trust:
            misplaced_keys += 1
        out = []
        for word, weight, cat in entries:
            ranks_of_word = []
            tones = []
            for ch in word:
                r = lookup(ch)
                if r is None:
                    ranks_of_word = None
                    break
                ranks_of_word.append(r[0])
                tones.append(str(r[1]))
            if not ranks_of_word:
                missing += 1
                out.append([word, weight, cat, ""])
                continue
            tone_str = "".join(tones)
            # 声调必须一位一字且为 1~5，否则作废（引擎会按缺数据降权）
            if len(tone_str) != len(word) or any(t not in "12345" for t in tone_str):
                bad_tone += 1
                tone_str = ""
            w = weight_of(min(ranks_of_word)) if len(word) == 1 else phrase_weight(min(ranks_of_word), len(word))
            if not legal:
                w = max(120, int(w * 0.55))
            if trust and len(word) == 1:
                scores = zhy_check(word)
                if scores:
                    idx = scores.get(key)
                    if idx is None:
                        misplaced += 1
                        w = max(80, int(w * 0.30))    # 拼写对不上这个键，沉底
                    elif idx == 0:
                        pass                          # 正音
                    else:
                        first_key = None
                        for z, i2 in scores.items():
                            if i2 == 0:
                                first_key = z
                                break
                        if first_key is not None and (first_key, word) in present:
                            # 正音条目已经存在，这里是多音/错位冗余，重罚
                            redundant += 1
                            w = max(100, int(w * 0.32))
                        elif idx == 1:
                            w = int(w * 0.70)
                        else:
                            w = max(120, int(w * 0.45))
            out.append([word, w, cat, tone_str])
            if w != weight:
                changed += 1
        out.sort(key=lambda e: -e[1])
        entries[:] = out

    # 目录必须按 UTF-8 字节序升序（BinDict 用二分查找）
    keys.sort(key=lambda kv: kv[0].encode("utf-8"))

    # 按原格式重新写出
    with open(os.path.join(assets, "zhuyin.bin"), "wb") as f:
        f.write(struct.pack("<iii", MAGIC, 3, 1))
        key_block = bytearray()
        word_index = []
        word_block = bytearray()
        dir_entries = bytearray()
        for key, entries in keys:
            kb = key.encode("utf-8")
            dir_entries += struct.pack("<ihhi", len(key_block), len(kb), len(entries),
                                       len(word_index))
            key_block += kb
            for word, w, cat, tones in entries:
                wb = word.encode("utf-8")
                tb = tones.encode("ascii")
                word_index.append(len(word_block))
                word_block += struct.pack("<IBBH", w, len(tb), len(wb), cat & 0xFFFF) + wb + tb
        f.write(struct.pack("<iiii", len(keys), len(word_index), len(key_block),
                            len(word_block)))
        f.write(dir_entries)
        f.write(key_block)
        for off in word_index:
            f.write(struct.pack("<i", off))
        f.write(word_block)

    size = os.path.getsize(os.path.join(assets, "zhuyin.bin"))
    print("keys=%d words=%d changed=%d no_reading=%d bad_tone=%d" % (
        len(keys), len(word_index), changed, missing, bad_tone))
    print("非法拼写键=%d（%d 词已降权） size=%.2f MB" % (
        bad_key, bad_key_words, size / 1048576.0))
    print("单字错位=%d 冗余错位=%d 低可信键=%d 补正音条目=%d" % (
        misplaced, redundant, misplaced_keys, added))


if __name__ == "__main__":
    main()

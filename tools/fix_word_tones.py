# -*- coding: utf-8 -*-
"""按 pypinyin 的上下文读音校正 pinyin.bin 里多字词目的声调串。

背景：本地 TSV 词表只给无声调拼音，构建时按单字默认读音回填声调，
多音字（银行/音乐/长短…）和轻声（为了/看着/为什么…）会标错。
本脚本只改每条记录的声调字节，词条、键、权重、分类一律不动；
音节对不上的条目保留原声调。
"""
import os
import struct
import sys

from pypinyin import pinyin, Style

MAGIC = 0x53594431
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "app", "assets")


def sections_of(data):
    magic, ver, n = struct.unpack_from("<iii", data, 0)
    if magic != MAGIC:
        raise SystemExit("bad magic in dict")
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


def split_key(q, syls):
    parts, i, n = [], 0, len(q)
    while i < n:
        for ln in range(min(6, n - i), 0, -1):
            if q[i:i + ln] in syls:
                parts.append(q[i:i + ln])
                i += ln
                break
        else:
            return None
    return parts


def norm_base(s):
    return s.replace("ü", "v").replace("u:", "v").strip().lower()


def parse_token(tok):
    if tok and tok[-1] in "12345":
        return norm_base(tok[:-1]), tok[-1]
    return norm_base(tok), "5"


def main():
    syls = set()
    with open(os.path.join(ASSETS, "syllables.txt"), encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line:
                syls.add(line)

    path = os.path.join(ASSETS, "pinyin.bin")
    data = bytearray(open(path, "rb").read())
    wide = struct.unpack_from("<i", data, 4)[0] >= 3
    secs = sections_of(data)

    total = 0
    changed = 0
    samples = []
    for si, s in enumerate(secs):
        if si == 2:  # 联想层无声调串
            continue
        for i in range(s["kc"]):
            ko, klen, wcnt, widx = struct.unpack_from(
                "<ihhi", data, s["dirOff"] + i * 12)
            key = bytes(data[s["keyOff"] + ko: s["keyOff"] + ko + klen]).decode("utf-8")
            dict_syls = split_key(key, syls)
            for j in range(wcnt):
                rel = struct.unpack_from(
                    "<i", data, s["widxOff"] + (widx + j) * 4)[0]
                a = s["wblkOff"] + rel
                body = a + 8 if wide else a + 7
                weight, tlen, wlen = struct.unpack_from("<IBB", data, a)
                if tlen == 0:
                    continue
                word = bytes(data[body:body + wlen]).decode("utf-8")
                tones = bytes(data[body + wlen:body + wlen + tlen]).decode("ascii")
                if len(word) < 2 or len(tones) != len(word):
                    continue
                if not dict_syls or len(dict_syls) != len(word):
                    continue
                pys = [p[0] for p in pinyin(word, style=Style.TONE3,
                                            errors="ignore", strict=False)]
                if len(pys) != len(word):
                    continue
                new = list(tones)
                hit = False
                for k in range(len(word)):
                    base, tone = parse_token(pys[k])
                    if base == dict_syls[k] and tone != tones[k]:
                        new[k] = tone
                        hit = True
                if hit:
                    total += 1
                    old = tones
                    data[body + wlen:body + wlen + tlen] = "".join(new).encode("ascii")
                    changed += 1
                    if len(samples) < 30:
                        samples.append(f"{word} {old} -> {''.join(new)}")

    open(path, "wb").write(data)
    print(f"校正条目：{changed} / {total}")
    for line in samples:
        print(" ", line)


if __name__ == "__main__":
    main()

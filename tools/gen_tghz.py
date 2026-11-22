# -*- coding: utf-8 -*-
"""把《通用规范汉字表》（8105 字）整理成 build_dict.py 能直接吃的补充词库。

输入：tools/data/8105_chars.txt（一个字一行，按字序）
      tools/data/8105_pinyin.txt（一行一个读音，与上面逐行对应，多音字用空格分隔）
输出：tools/data/tghz2013.txt  格式：汉字<TAB>音1,音2,...<TAB>字级(1/2/3)<TAB>字序
      音节一律归一成「去掉声调字母 + 声调数字」，ü / üe 等写成 v / ve（和词库里的写法一致）
"""
import os
import re

BASE = os.path.dirname(os.path.abspath(__file__))
DATA = os.path.join(BASE, "data")

# 带调元音 -> (无调写法, 声调)
TONE = {
    "ā": ("a", 1), "á": ("a", 2), "ǎ": ("a", 3), "à": ("a", 4),
    "ē": ("e", 1), "é": ("e", 2), "ě": ("e", 3), "è": ("e", 4),
    "ī": ("i", 1), "í": ("i", 2), "ǐ": ("i", 3), "ì": ("i", 4),
    "ō": ("o", 1), "ó": ("o", 2), "ǒ": ("o", 3), "ò": ("o", 4),
    "ū": ("u", 1), "ú": ("u", 2), "ǔ": ("u", 3), "ù": ("u", 4),
    "ǖ": ("v", 1), "ǘ": ("v", 2), "ǚ": ("v", 3), "ǜ": ("v", 4),
    "ü": ("v", 0), "ǘ": ("v", 2),
    "ń": ("n", 2), "ň": ("n", 3), "ǹ": ("n", 4),
    "ḿ": ("m", 2), "m̀": ("m", 4), "ê": ("e", 0), "ế": ("e", 2), "ề": ("e", 4), "ễ": ("e", 3),
}
COMBINING = {"\u0300": 4, "\u0301": 2, "\u0303": 2, "\u0304": 1, "\u030c": 3}


def parse_one(raw):
    """一个读音 -> (音节, 声调)；失败返回 None。"""
    tone = 0
    out = []
    for ch in raw:
        if ch in TONE:
            base, t = TONE[ch]
            out.append(base)
            if t:
                tone = t
            continue
        if ch in COMBINING:
            if COMBINING[ch]:
                tone = COMBINING[ch]
            continue
        if ch.isdigit():
            tone = int(ch)
            continue
        if ("a" <= ch <= "z") or ("A" <= ch <= "Z"):
            out.append(ch.lower())
            continue
        if ch in ("'", "-", " "):
            continue
        return None
    syl = "".join(out)
    if not syl or not re.match(r"^[a-z]{1,6}$", syl):
        return None
    return syl, (tone or 1)


def main():
    chars = [l.strip() for l in open(os.path.join(DATA, "8105_chars.txt"), encoding="utf-8") if l.strip()]
    pys = [l.strip() for l in open(os.path.join(DATA, "8105_pinyin.txt"), encoding="utf-8") if l.strip()]
    if len(chars) != len(pys):
        raise SystemExit("字数与读音行数不一致：%d / %d" % (len(chars), len(pys)))

    out = []
    bad = []
    total_readings = 0
    skipped_rare = 0
    for i, (ch, py) in enumerate(zip(chars, pys)):
        level = 1 if i < 3500 else (2 if i < 6500 else 3)
        # 《通用规范汉字表》收字都在基本区 U+4E00–U+9FFF；数据源里混进来的
        # 扩展 A/B 等生僻码位（如 𨱑、㧐）直接丢掉，免得污染常用音节的候选
        if not ("\u4e00" <= ch <= "\u9fff"):
            skipped_rare += 1
            continue
        readings = []
        for one in py.split():
            got = parse_one(one)
            if got is None:
                bad.append((ch, one))
                continue
            syl, tone = got
            token = syl + str(tone)
            if token not in readings:
                readings.append(token)
        if not readings:
            continue
        total_readings += len(readings)
        out.append("%s\t%s\t%d\t%d" % (ch, ",".join(readings), level, i + 1))

    path = os.path.join(DATA, "tghz2013.txt")
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write("# 《通用规范汉字表》（2013，国务院公布 8105 字）\n")
        f.write("# 格式：汉字<TAB>音1,音2（无调字母+声调数字）<TAB>字级1/2/3<TAB>字序\n")
        for line in out:
            f.write(line + "\n")

    print("汉字 %d，写出 %d 行，读音 %d 条，解析失败 %d 条，跳过非基本区 %d 字"
          % (len(chars), len(out), total_readings, len(bad), skipped_rare))
    if bad:
        print("失败样例：", bad[:12])


if __name__ == "__main__":
    main()

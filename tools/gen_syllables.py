# -*- coding: utf-8 -*-
"""生成注音规范音节表（供 ZhuyinEngine 做音节切分/合法性判断）。

来源：普通话合法音节（pypinyin 的字表里去重），按拼音拼写规则转注音：
  声母 21（ㄅ…ㄙ） + 韵母（ㄚㄛㄜㄝㄞㄟㄠㄡㄢㄣㄤㄥㄦㄧㄨㄩ 组合）
  整体认读音节 zhi/chi/shi/ri/zi/ci/si → ㄓㄔㄕㄖㄗㄘㄙ（只写声母）
  零声母 y/w 系：yi/wu/yu… → ㄧㄨㄩ 开头
输出：app/java/com/shuangyin/ime/core/ZhuyinSyllables.java
"""

import io
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from build_readings import py_to_zhuyin

ASCII_PINYIN = re.compile(r"^[a-zü:]+$")


TONE_MARKS = "ˉˊˇˋ˙"


def strip_tone(z):
    """去掉调号：音节表只存声母+韵母本身。"""
    for m in TONE_MARKS:
        z = z.replace(m, "")
    return z


def collect():
    """优先用词库导出的完整音节表（assets/syllables.txt，425 个），
    pypinyin 的精简字表只有 130 个，不够用。"""
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    syl_file = os.path.join(root, "app", "assets", "syllables.txt")
    raw = set()
    if os.path.isfile(syl_file):
        with io.open(syl_file, encoding="utf-8") as f:
            for line in f:
                p = line.strip()
                if p and ASCII_PINYIN.match(p):
                    raw.add(p)
    if not raw:
        sys.path.insert(0, os.environ.get("SY_PKGS", "pkgs"))
        from pypinyin.pinyin_dict import pinyin_dict
        for v in pinyin_dict.values():
            for p in v.split(","):
                p = p.strip()
                if p and ASCII_PINYIN.match(p):
                    raw.add(p)
    zhy = set()
    bad = []
    for p in raw:
        z = py_to_zhuyin(p + "5")
        if not z:
            bad.append(p)
            continue
        zhy.add(strip_tone(z))
    return zhy, bad, len(raw)


def main():
    zhy, bad, total = collect()
    print("拼音音节=%d 注音音节=%d 无法转换=%d" % (total, len(zhy), len(bad)))
    if bad:
        print("未转换示例:", bad[:12])

    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    out = os.path.join(root, "app", "java", "com", "shuangyin", "ime", "core", "ZhuyinSyllables.java")
    items = sorted(zhy, key=lambda s: (len(s), s))
    lines = []
    lines.append("package com.shuangyin.ime.core;")
    lines.append("")
    lines.append("/** 注音规范音节表（声母+韵母 / 整体认读音节），由 tools/gen_syllables.py 生成。 */")
    lines.append("public final class ZhuyinSyllables {")
    lines.append("    private ZhuyinSyllables() {")
    lines.append("    }")
    lines.append("")
    lines.append("    /** 全部合法注音音节，按长度排序便于最长匹配。 */")
    lines.append("    private static final String RAW =")
    rows = []
    for i in range(0, len(items), 16):
        chunk = items[i:i + 16]
        rows.append('            "' + " ".join(chunk) + '"')
    lines.append(" +\n".join(rows) + ";")
    lines.append("")
    lines.append("    private static final String[] LIST = RAW.split(\" \");")
    lines.append("    private static final java.util.HashSet<String> SET =")
    lines.append("            new java.util.HashSet<String>(LIST.length * 2);")
    lines.append("")
    lines.append("    static {")
    lines.append("        for (int i = 0; i < LIST.length; i++) {")
    lines.append("            SET.add(LIST[i]);")
    lines.append("        }")
    lines.append("    }")
    lines.append("")
    lines.append("    /** 是否为完整的合法音节（含整体认读音节 ㄓㄔㄕㄖㄗㄘㄙ、ㄦ）。 */")
    lines.append("    public static boolean isSyllable(String s) {")
    lines.append("        return s != null && SET.contains(s);")
    lines.append("    }")
    lines.append("")
    lines.append("    public static int size() {")
    lines.append("        return LIST.length;")
    lines.append("    }")
    lines.append("}")
    with io.open(out, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines) + "\n")
    print("写出 %s，%d 个音节" % (out, len(items)))


if __name__ == "__main__":
    main()

# -*- coding: utf-8 -*-
"""生成 ZhuyinSyllables.java：注音音节表 + 注音→拼音对照 + 拼读结构分类。

五类结构（与键盘分区、排序权重一致）：
  1 声母+韵母（ㄅㄚ、ㄐㄧㄤ…）
  2 整体认读（ㄓ ㄔ ㄕ ㄖ ㄗ ㄘ ㄙ ㄧ ㄨ ㄩ ㄧㄝ ㄩㄝ ㄩㄢ ㄧㄣ ㄩㄣ ㄧㄥ）
  3 零声母韵母（ㄚ ㄛ ㄜ ㄞ ㄟ ㄠ ㄡ ㄢ ㄣ ㄤ ㄥ ㄦ）
  4 介母开头（ㄧㄚ ㄨㄛ ㄩㄝ…中除去整体认读的那些）
声调由 1~5 单独处理，不进音节表。
"""
import io
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from build_readings import py_to_zhuyin

TONE_MARKS = "ˉˊˇˋ˙"

INITIAL_BPMF = set("ㄅㄆㄇㄈㄉㄊㄋㄌㄍㄎㄏㄐㄑㄒㄓㄔㄕㄖㄗㄘㄙ")
MEDIAL_BPMF = set("ㄧㄨㄩ")
INTEGRAL_PY = {"zhi", "chi", "shi", "ri", "zi", "ci", "si",
               "yi", "wu", "yu", "ye", "yue", "yuan", "yin", "yun", "ying"}


def strip_tone(z):
    for m in TONE_MARKS:
        z = z.replace(m, "")
    return z


def main():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    syl_file = os.path.join(root, "app", "assets", "syllables.txt")
    py = []
    with io.open(syl_file, encoding="utf-8") as f:
        for line in f:
            s = line.strip()
            if s:
                py.append(s)
    py_set = sorted(set(py))
    bpmf = {}
    for p in py_set:
        z = strip_tone(py_to_zhuyin(p + "5"))
        if not z:
            continue
        if z in bpmf and bpmf[z] != p:
            print("冲突 %s: %s / %s" % (z, bpmf[z], p))
            continue
        bpmf[z] = p

    items = sorted(bpmf.items(), key=lambda kv: (len(kv[0]), kv[0]))

    def kind(z, p):
        if p in INTEGRAL_PY:
            return 2
        if z[0] in INITIAL_BPMF:
            return 1
        if z[0] in MEDIAL_BPMF:
            return 4
        return 3

    kinds = "".join(str(kind(z, p)) for z, p in items)
    pairs = " ".join("%s=%s" % (z, p) for z, p in items)

    out = os.path.join(root, "app", "java", "com", "shuangyin", "ime", "core",
                       "ZhuyinSyllables.java")
    lines = []
    lines.append("package com.shuangyin.ime.core;")
    lines.append("")
    lines.append("/** 注音规范音节表 + 注音→拼音对照 + 拼读结构分类。")
    lines.append(" *  结构：1 声母+韵母 / 2 整体认读 / 3 零声母韵母 / 4 介母开头。")
    lines.append(" *  由 tools/gen_zhuyin_table.py 生成，勿手改。 */")
    lines.append("public final class ZhuyinSyllables {")
    lines.append("    private ZhuyinSyllables() {")
    lines.append("    }")
    lines.append("")
    lines.append("    public static final int KIND_INITIAL_FINAL = 1;")
    lines.append("    public static final int KIND_INTEGRAL = 2;")
    lines.append("    public static final int KIND_ZERO_FINAL = 3;")
    lines.append("    public static final int KIND_MEDIAL_FINAL = 4;")
    lines.append("")
    lines.append('    private static final String RAW =')
    rows = []
    for i in range(0, len(items), 16):
        chunk = [z for z, _ in items[i:i + 16]]
        piece = " ".join(chunk)
        if i + 16 < len(items):
            piece += " "
        rows.append('            "' + piece + '"')
    lines.append(" +\n".join(rows) + ";")
    lines.append("")
    lines.append("    /** 每个音节的结构类别，字符顺序与 RAW 一致。 */")
    krows = []
    for i in range(0, len(kinds), 64):
        krows.append('            "' + kinds[i:i + 64] + '"')
    lines.append('    private static final String KINDRAW =')
    lines.append(" +\n".join(krows) + ";")
    lines.append("")
    lines.append("    /** 注音→拼音（不带调，ü 写作 v，j/q/x 后写作 u）。 */")
    prow = []
    chunk = []
    cur = 0
    pieces = ["%s=%s" % (z, p) for z, p in items]
    for piece in pieces:
        if cur + len(piece) + 1 > 78 and chunk:
            prow.append('            "' + " ".join(chunk) + ' "')
            chunk = []
            cur = 0
        chunk.append(piece)
        cur += len(piece) + 1
    if chunk:
        prow.append('            "' + " ".join(chunk) + '"')
    lines.append('    private static final String PINRAW =')
    lines.append(" +\n".join(prow) + ";")
    lines.append("")
    lines.append("    /** 合法拼音音节（用于把拼音键切音节）。 */")
    pyrows = []
    for i in range(0, len(py_set), 16):
        piece = " ".join(py_set[i:i + 16])
        if i + 16 < len(py_set):
            piece += " "
        pyrows.append('            "' + piece + '"')
    lines.append('    private static final String PYRAW =')
    lines.append(" +\n".join(pyrows) + ";")
    lines.append("")
    lines.append('    private static final java.util.HashMap<String, String> PIN =')
    lines.append("            new java.util.HashMap<String, String>(%d);" % (len(items) * 2))
    lines.append('    private static final java.util.HashMap<String, Integer> KIND =')
    lines.append("            new java.util.HashMap<String, Integer>(%d);" % (len(items) * 2))
    lines.append('    private static final java.util.HashSet<String> SET =')
    lines.append("            new java.util.HashSet<String>(%d);" % (len(items) * 2))
    lines.append('    private static final java.util.HashSet<String> PYSET =')
    lines.append("            new java.util.HashSet<String>(%d);" % (len(py_set) * 2))
    lines.append("")
    lines.append("    static {")
    lines.append('        String[] raw = RAW.split(" ");')
    lines.append('        String kinds = KINDRAW.replace(" ", "");')
    lines.append("        for (int i = 0; i < raw.length; i++) {")
    lines.append("            SET.add(raw[i]);")
    lines.append("            KIND.put(raw[i], kinds.charAt(i) - '0');")
    lines.append("        }")
    lines.append('        for (String p : PINRAW.split(" ")) {')
    lines.append('            int eq = p.indexOf(\'=\');')
    lines.append("            if (eq > 0) PIN.put(p.substring(0, eq), p.substring(eq + 1));")
    lines.append("        }")
    lines.append('        for (String p : PYRAW.split(" ")) {')
    lines.append("            PYSET.add(p);")
    lines.append("        }")
    lines.append("    }")
    lines.append("")
    lines.append("    /** 是否为完整的合法注音音节（含整体认读音节）。 */")
    lines.append("    public static boolean isSyllable(String s) {")
    lines.append("        return s != null && SET.contains(s);")
    lines.append("    }")
    lines.append("")
    lines.append("    /** 该注音音节的结构类别；未知返回 0。 */")
    lines.append("    public static int kind(String s) {")
    lines.append("        Integer k = s == null ? null : KIND.get(s);")
    lines.append("        return k == null ? 0 : k.intValue();")
    lines.append("    }")
    lines.append("")
    lines.append("    /** 注音音节对应的拼音（不带调）；未知返回 null。 */")
    lines.append("    public static String pinyin(String s) {")
    lines.append("        return s == null ? null : PIN.get(s);")
    lines.append("    }")
    lines.append("")
    lines.append("    /** 是否整体认读音节（ㄓㄔㄕㄖㄗㄘㄙㄧㄨㄩㄧㄝㄩㄝㄩㄢㄧㄣㄩㄣㄧㄥ）。 */")
    lines.append("    public static boolean isIntegral(String s) {")
    lines.append("        return kind(s) == KIND_INTEGRAL;")
    lines.append("    }")
    lines.append("")
    lines.append("    /** 是否为拼音的合法音节（用于切拼音键）。 */")
    lines.append("    public static boolean isPinyinSyllable(String s) {")
    lines.append("        return s != null && PYSET.contains(s);")
    lines.append("    }")
    lines.append("")
    lines.append("    /** 声母符号（21 个）。 */")
    lines.append("    public static boolean isInitial(char c) {")
    lines.append('        return "ㄅㄆㄇㄈㄉㄊㄋㄌㄍㄎㄏㄐㄑㄒㄓㄔㄕㄖㄗㄘㄙ".indexOf(c) >= 0;')
    lines.append("    }")
    lines.append("")
    lines.append("    /** 介母符号（ㄧ ㄨ ㄩ）。 */")
    lines.append("    public static boolean isMedial(char c) {")
    lines.append("        return c == 'ㄧ' || c == 'ㄨ' || c == 'ㄩ';")
    lines.append("    }")
    lines.append("")
    lines.append("    /** 声母符号对应的拼音字母；未知返回空串。 */")
    lines.append("    public static String initialLetters(char c) {")
    lines.append("        switch (c) {")
    for ch, pz in [("ㄅ", "b"), ("ㄆ", "p"), ("ㄇ", "m"), ("ㄈ", "f"), ("ㄉ", "d"),
                   ("ㄊ", "t"), ("ㄋ", "n"), ("ㄌ", "l"), ("ㄍ", "g"), ("ㄎ", "k"),
                   ("ㄏ", "h"), ("ㄐ", "j"), ("ㄑ", "q"), ("ㄒ", "x"), ("ㄓ", "zh"),
                   ("ㄔ", "ch"), ("ㄕ", "sh"), ("ㄖ", "r"), ("ㄗ", "z"), ("ㄘ", "c"),
                   ("ㄙ", "s")]:
        lines.append("            case '%s': return \"%s\";" % (ch, pz))
    lines.append('            default: return "";')
    lines.append("        }")
    lines.append("    }")
    lines.append("")
    lines.append("    /** 介母单独起头（零声母）时的拼音字母。 */")
    lines.append("    public static String medialLetters(char c) {")
    lines.append('        if (c == \'ㄧ\') return "y";')
    lines.append('        if (c == \'ㄨ\') return "w";')
    lines.append('        if (c == \'ㄩ\') return "yu";')
    lines.append('        return "";')
    lines.append("    }")
    lines.append("")
    lines.append("    public static int size() {")
    lines.append("        return SET.size();")
    lines.append("    }")
    lines.append("}")
    with io.open(out, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines) + "\n")
    print("音节=%d 写出=%s" % (len(items), out))


if __name__ == "__main__":
    main()

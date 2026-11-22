# -*- coding: utf-8 -*-
"""从已打包的 pinyin.bin / zhuyin.bin 里导出「单字 -> 读音」对照表。

词条记录里本来就带着声调串（tones），键就是无声调的音节串，
两者合起来就是完整的带调拼音 / 带调注音，所以不需要再读原始 yaml。

输出：
  app/assets/reading_pin.txt   每行「字<TAB>nǐ」
  app/assets/reading_zhy.txt   每行「字<TAB>ㄋㄧˇ」
多音字取词频最高的那个读音。
"""
import os
import struct
import sys

MAGIC = 0x53594431

TONE_MAP = {
    "a": "āáǎà",
    "o": "ōóǒò",
    "e": "ēéěè",
    "i": "īíǐì",
    "u": "ūúǔù",
    "ü": "ǖǘǚǜ",
}


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


def add_pinyin_tone(syl, tone):
    """给无声调音节加声调符号；tone 为 '1'~'4'，'5'/其它原样返回。"""
    if tone not in "1234":
        return syl
    syl = syl.replace("v", "ü").replace("u:", "ü")
    idx = -1
    if "a" in syl:
        idx = syl.index("a")
    elif "o" in syl:
        idx = syl.index("o")
    elif "e" in syl:
        idx = syl.index("e")
    elif "iu" in syl:
        idx = syl.index("u")
    elif "ui" in syl:
        idx = syl.index("i")
    else:
        for i, ch in enumerate(syl):
            if ch in "iuü":
                idx = i
    if idx < 0:
        return syl
    base = syl[idx]
    marks = TONE_MAP.get(base)
    if not marks:
        return syl
    return syl[:idx] + marks[int(tone) - 1] + syl[idx + 1:]


def add_zhuyin_tone(syl, tone):
    if tone == "1":
        return syl + "ˉ"
    if tone == "2":
        return syl + "ˊ"
    if tone == "3":
        return syl + "ˇ"
    if tone == "4":
        return syl + "ˋ"
    if tone == "5":
        return "˙" + syl
    return syl


INITIAL_MAP = {
    "b": "ㄅ", "p": "ㄆ", "m": "ㄇ", "f": "ㄈ", "d": "ㄉ", "t": "ㄊ", "n": "ㄋ", "l": "ㄌ",
    "g": "ㄍ", "k": "ㄎ", "h": "ㄏ", "j": "ㄐ", "q": "ㄑ", "x": "ㄒ",
    "zh": "ㄓ", "ch": "ㄔ", "sh": "ㄕ", "r": "ㄖ", "z": "ㄗ", "c": "ㄘ", "s": "ㄙ",
}

FINALS = {
    "a": "ㄚ", "o": "ㄛ", "e": "ㄜ", "ai": "ㄞ", "ei": "ㄟ", "ao": "ㄠ", "ou": "ㄡ",
    "an": "ㄢ", "en": "ㄣ", "ang": "ㄤ", "eng": "ㄥ", "er": "ㄦ",
    "i": "ㄧ", "ia": "ㄧㄚ", "ie": "ㄧㄝ", "iao": "ㄧㄠ", "iu": "ㄧㄡ", "ian": "ㄧㄢ",
    "in": "ㄧㄣ", "iang": "ㄧㄤ", "ing": "ㄧㄥ", "iong": "ㄩㄥ", "io": "ㄧㄛ",
    "u": "ㄨ", "ua": "ㄨㄚ", "uo": "ㄨㄛ", "uai": "ㄨㄞ", "ui": "ㄨㄟ", "uan": "ㄨㄢ",
    "un": "ㄨㄣ", "uang": "ㄨㄤ", "ong": "ㄨㄥ",
    "ü": "ㄩ", "üe": "ㄩㄝ", "üan": "ㄩㄢ", "ün": "ㄩㄣ",
}

ZERO = {
    "a": "ㄚ", "o": "ㄛ", "e": "ㄜ", "ai": "ㄞ", "ei": "ㄟ", "ao": "ㄠ", "ou": "ㄡ",
    "an": "ㄢ", "en": "ㄣ", "ang": "ㄤ", "eng": "ㄥ", "er": "ㄦ", "ê": "ㄝ",
    "yi": "ㄧ", "ya": "ㄧㄚ", "ye": "ㄧㄝ", "yao": "ㄧㄠ", "you": "ㄧㄡ", "yan": "ㄧㄢ",
    "yin": "ㄧㄣ", "yang": "ㄧㄤ", "ying": "ㄧㄥ", "yong": "ㄩㄥ", "yo": "ㄧㄛ",
    "wu": "ㄨ", "wa": "ㄨㄚ", "wo": "ㄨㄛ", "wai": "ㄨㄞ", "wei": "ㄨㄟ", "wan": "ㄨㄢ",
    "wen": "ㄨㄣ", "wang": "ㄨㄤ", "weng": "ㄨㄥ",
    "yu": "ㄩ", "yue": "ㄩㄝ", "yuan": "ㄩㄢ", "yun": "ㄩㄣ",
}


def py_to_zhuyin(py3):
    """带调号数字的拼音（如 xue2）转注音符号（ㄒㄩㄝˊ）。"""
    if not py3:
        return ""
    tone = py3[-1] if py3[-1] in "12345" else "5"
    body = (py3[:-1] if py3[-1] in "12345" else py3).replace("u:", "ü").replace("v", "ü")
    if body in ZERO:
        return add_zhuyin_tone(ZERO[body], tone)
    ini = ""
    for cand in ("zh", "ch", "sh", "b", "p", "m", "f", "d", "t", "n", "l", "g", "k",
                 "h", "j", "q", "x", "r", "z", "c", "s"):
        if body.startswith(cand):
            ini = cand
            break
    fin = body[len(ini):]
    if fin == "i" and ini in ("zh", "ch", "sh", "r", "z", "c", "s"):
        fin = ""  # 舌尖元音不写 ㄧ
    if ini in ("j", "q", "x") and fin.startswith("u"):
        fin = "ü" + fin[1:]
    if fin == "":
        zfin = ""  # 舌尖元音（zhi/chi/shi/ri/zi/ci/si）只写声母
    else:
        zfin = FINALS.get(fin)
    if zfin is None:
        return ""
    return add_zhuyin_tone(INITIAL_MAP.get(ini, "") + zfin, tone)


def export(bin_path, kind):
    """返回 {字: 最高词频的无声调拼音 / 注音 body}。"""
    with open(bin_path, "rb") as f:
        data = f.read()
    wide = struct.unpack_from("<i", data, 4)[0] >= 3
    s = sections_of(data)[0]
    best = {}
    for i in range(s["kc"]):
        ko, klen, wcnt, widx = struct.unpack_from("<ihhi", data, s["dirOff"] + i * 12)
        key = data[s["keyOff"] + ko: s["keyOff"] + ko + klen].decode("utf-8")
        for j in range(wcnt):
            rel = struct.unpack_from("<i", data, s["widxOff"] + (widx + j) * 4)[0]
            a = s["wblkOff"] + rel
            body = a + 8 if wide else a + 7
            weight, tlen, wlen = struct.unpack_from("<IBB", data, a)
            word = data[body: body + wlen].decode("utf-8")
            tones = data[body + wlen: body + wlen + tlen].decode("ascii")
            if len(word) != 1 or len(tones) != 1:
                continue
            old = best.get(word)
            if old is None or weight > old[0]:
                best[word] = (weight, key)
    return best


def tone_number(py3):
    """从 pypinyin TONE3 结果里取声调数字（1~4），轻声/无调返回 '5'。"""
    if py3 and py3[-1] in "12345":
        return py3[-1]
    return "5"


def build():
    from pypinyin import pinyin, Style
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    assets = os.path.join(root, "app", "assets")

    pin_src = export(os.path.join(assets, "pinyin.bin"), "pin")
    zhy_src = export(os.path.join(assets, "zhuyin.bin"), "zhy")

    # 拼音：pypinyin 给出的才是带调读音；生僻字查不到时退回词库的无声调拼音
    lines = []
    for ch, (_w, plain) in sorted(pin_src.items(), key=lambda kv: -kv[1][0]):
        try:
            reading = pinyin(ch, style=Style.TONE)[0][0]
        except Exception:
            reading = add_pinyin_tone(plain, "5")
        if not reading:
            reading = plain
        lines.append(ch + "\t" + reading + "\n")
    with open(os.path.join(assets, "reading_pin.txt"), "w", encoding="utf-8", newline="\n") as f:
        f.writelines(lines)
    print("reading_pin.txt chars=%d size=%.1f KB" % (len(lines),
          os.path.getsize(os.path.join(assets, "reading_pin.txt")) / 1024.0))

    # 注音：一律由带调拼音机械转换，避免小麥词库里的异读（例："学"被标成 ㄅㄛˊ）
    lines = []
    miss = 0
    for ch, (_w, plain) in sorted(pin_src.items(), key=lambda kv: -kv[1][0]):
        try:
            py3 = pinyin(ch, style=Style.TONE3)[0][0]
        except Exception:
            py3 = ""
        zhy = py_to_zhuyin(py3)
        if not zhy:
            miss += 1
            continue
        lines.append(ch + "\t" + zhy + "\n")
    with open(os.path.join(assets, "reading_zhy.txt"), "w", encoding="utf-8", newline="\n") as f:
        f.writelines(lines)
    print("reading_zhy.txt chars=%d size=%.1f KB miss=%d" % (len(lines),
          os.path.getsize(os.path.join(assets, "reading_zhy.txt")) / 1024.0, miss))


def main():
    build()


if __name__ == "__main__":
    main()

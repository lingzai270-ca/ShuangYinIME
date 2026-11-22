"""把 zhuyin.bin 里历史遗留的 cat=128 改成 32（CAT_ZHUYIN），保留原有词条与权重。

1.0.46 重排分类位后，注音从 128 挪到 32，但 zhuyin.bin 是旧构建回填的，
词条仍写 128；Prefs 的 CAT_ALL=127 会把它们全部过滤掉，表现为注音模式无候选。
"""
import os
import shutil
import struct
import sys

MAGIC = 0x53594431


def fix(path):
    with open(path, "rb") as f:
        data = bytearray(f.read())
    magic, ver, sc = struct.unpack_from("<iii", data, 0)
    if magic != MAGIC:
        raise SystemExit("bad magic")
    pos = 12
    changed = 0
    total = 0
    for _ in range(sc):
        kc, wc, kbl, wbl = struct.unpack_from("<iiii", data, pos)
        dir_off = pos + 16
        key_off = dir_off + kc * 12
        widx_off = key_off + kbl
        wblk_off = widx_off + wc * 4
        wide = ver >= 3
        for i in range(wc):
            rel = struct.unpack_from("<i", data, widx_off + i * 4)[0]
            ab = wblk_off + rel
            total += 1
            if wide:
                cat = struct.unpack_from("<H", data, ab + 6)[0]
                if cat == 128:
                    struct.pack_into("<H", data, ab + 6, 32)
                    changed += 1
            else:
                if data[ab + 6] == 128:
                    data[ab + 6] = 32
                    changed += 1
        pos = wblk_off + wbl
    return total, changed, bytes(data)


def main():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    path = os.path.join(root, "app", "assets", "zhuyin.bin")
    total, changed, out = fix(path)
    bak = path + ".cat128.bak"
    if not os.path.exists(bak):
        shutil.copy2(path, bak)
    with open(path, "wb") as f:
        f.write(out)
    print("records=%d changed=%d -> cat=32" % (total, changed))


if __name__ == "__main__":
    main()

import struct, sys, collections

def scan(path):
    data = open(path, 'rb').read()
    magic, ver, sc = struct.unpack_from('<iii', data, 0)
    pos = 12
    cats = collections.Counter()
    total = 0
    for s in range(sc):
        kc, wc, kbl, wbl = struct.unpack_from('<iiii', data, pos)
        dirOff = pos + 16
        keyBlkOff = dirOff + kc * 12
        wordIdxOff = keyBlkOff + kbl
        wordBlkOff = wordIdxOff + wc * 4
        wide = ver >= 3
        for i in range(wc):
            rel = struct.unpack_from('<i', data, wordIdxOff + i * 4)[0]
            ab = wordBlkOff + rel
            if wide:
                cat = struct.unpack_from('<H', data, ab + 6)[0]
            else:
                cat = data[ab + 6]
            cats[cat] += 1
            total += 1
        pos = wordBlkOff + wbl
    print('ver=%d sections=%d records=%d' % (ver, sc, total))
    for c, n in cats.most_common(20):
        print('  cat=%-4d (%s) %d' % (c, format(c, '016b'), n))

for p in sys.argv[1:]:
    print('==', p)
    scan(p)

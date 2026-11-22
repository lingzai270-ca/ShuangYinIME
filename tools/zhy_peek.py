import struct, sys, io

def load(path, n=40, sec=0):
    with open(path, 'rb') as f:
        data = f.read()
    magic, ver, sc = struct.unpack_from('<iii', data, 0)
    print('magic=%08x ver=%d sections=%d size=%d' % (magic, ver, sc, len(data)))
    pos = 12
    for s in range(sc):
        kc, wc, kbl, wbl = struct.unpack_from('<iiii', data, pos)
        print('section %d keys=%d words=%d keyBlk=%d wordBlk=%d' % (s, kc, wc, kbl, wbl))
        dirOff = pos + 16
        keyBlkOff = dirOff + kc * 12
        wordIdxOff = keyBlkOff + kbl
        wordBlkOff = wordIdxOff + wc * 4
        if s != sec:
            pos = wordBlkOff + wbl
            continue
        wide = ver >= 3
        shown = 0
        for i in range(min(kc, 200)):
            koff, klen, words, widx = struct.unpack_from('<ihhi', data, dirOff + i * 12)
            key = data[keyBlkOff + koff: keyBlkOff + koff + klen].decode('utf-8', 'replace')
            print('KEY[%d] %s words=%d' % (i, key, words))
            for j in range(min(words, 4)):
                rel = struct.unpack_from('<i', data, wordIdxOff + (widx + j) * 4)[0]
                ab = wordBlkOff + rel
                w, tl, wl = struct.unpack_from('<iBB', data, ab)
                if wide:
                    cat = struct.unpack_from('<H', data, ab + 6)[0]
                    bo = ab + 8
                else:
                    cat = data[ab + 6]
                    bo = ab + 7
                word = data[bo: bo + wl].decode('utf-8', 'replace')
                tones = data[bo + wl: bo + wl + tl].decode('utf-8', 'replace')
                print('   w=%d cat=%d word=%r tones=%r' % (w, cat, word, tones))
            shown += 1
            if shown >= n:
                break
        pos = wordBlkOff + wbl

if __name__ == '__main__':
    load(sys.argv[1], int(sys.argv[2]) if len(sys.argv) > 2 else 20)

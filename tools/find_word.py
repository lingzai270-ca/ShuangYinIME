import struct, sys

path, target = sys.argv[1], sys.argv[2]
data = open(path, 'rb').read()
magic, ver, secs = struct.unpack_from('<iii', data, 0)
print('ver', ver, 'sections', secs)
pos = 12
wide = ver >= 3
hdr = 8 if wide else 7
for si in range(secs):
    keyCount, wordCount, keyBlockLen, wordBlockLen = struct.unpack_from('<iiii', data, pos)
    pos += 16
    dirOff = pos
    pos += keyCount * 12
    keyBlockOff = pos
    pos += keyBlockLen
    wordIndexOff = pos
    pos += wordCount * 4
    wordBlockOff = pos
    pos += wordBlockLen
    print('sec', si, 'keys', keyCount, 'words', wordCount)
    if si != 0:
        continue
    for di in range(keyCount):
        keyOff, keyLen, nwords, widx = struct.unpack_from('<ihhi', data, dirOff + di * 12)
        key = data[keyBlockOff + keyOff: keyBlockOff + keyOff + keyLen].decode('utf-8', 'replace')
        if target not in key and 'zhonghuarenmin' not in key:
            continue
        print('key', key, 'words', nwords)
        for wi in range(nwords):
            woff = struct.unpack_from('<i', data, wordIndexOff + (widx + wi) * 4)[0]
            weight, tlen, wlen, cat = struct.unpack_from('<IBBH', data, woff)
            word = data[woff + hdr: woff + hdr + wlen].decode('utf-8', 'replace')
            tones = data[woff + hdr + wlen: woff + hdr + wlen + tlen].decode('utf-8', 'replace')
            print('   ', word, 'w=', weight, 'tones=', tones, 'cat=', cat)

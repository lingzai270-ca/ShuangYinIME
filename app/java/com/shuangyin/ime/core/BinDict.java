package com.shuangyin.ime.core;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 只读词库文件读取器。
 *
 * 文件格式（小端）：
 *   int32 magic 'SYD1' | int32 version | int32 sectionCount
 *   每个 section：
 *     int32 keyCount | int32 wordCount | int32 keyBlockLen | int32 wordBlockLen
 *     keyDir[keyCount]  每条 12 字节：int32 keyOff | int16 keyLen | int16 words | int32 wordIndex
 *     keyBlock[keyBlockLen]  UTF-8 键
 *     wordIndex[wordCount]   int32 词条记录的绝对偏移
 *     wordBlock[wordBlockLen] 每条记录：uint32 weight | uint8 toneLen | uint8 wordLen | uint16 cat | word | tones
 *   version 3 起 cat 用 16 位（分类变多）；仍兼容 version 2 的 8 位 cat 老词库。
 */
public final class BinDict {
    public static final int MAGIC = 0x53594431;
    private static final int DIR_ITEM = 12;

    private RandomAccessFile raf;
    private FileChannel channel;
    private ByteBuffer buf;
    private Section[] sections;
    /** version 3：词条头 8 字节（cat 16 位）；version 2：7 字节（cat 8 位）。 */
    private boolean wideCat;

    static final class Section {
        int keyCount;
        int wordCount;
        int dirOff;
        int keyBlockOff;
        int wordIndexOff;
        int wordBlockOff;
    }

    public BinDict(File file) throws IOException {
        raf = new RandomAccessFile(file, "r");
        channel = raf.getChannel();
        buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        buf.order(ByteOrder.LITTLE_ENDIAN);

        int magic = buf.getInt(0);
        if (magic != MAGIC) {
            throw new IOException("bad dict magic: " + magic);
        }
        int version = buf.getInt(4);
        if (version != 2 && version != 3) {
            throw new IOException("unsupported dict version: " + version);
        }
        wideCat = version >= 3;
        int sectionCount = buf.getInt(8);
        sections = new Section[sectionCount];
        int pos = 12;
        for (int i = 0; i < sectionCount; i++) {
            Section s = new Section();
            s.keyCount = buf.getInt(pos);
            s.wordCount = buf.getInt(pos + 4);
            int keyBlockLen = buf.getInt(pos + 8);
            int wordBlockLen = buf.getInt(pos + 12);
            pos += 16;
            s.dirOff = pos;
            pos += s.keyCount * DIR_ITEM;
            s.keyBlockOff = pos;
            pos += keyBlockLen;
            s.wordIndexOff = pos;
            pos += s.wordCount * 4;
            s.wordBlockOff = pos;
            pos += wordBlockLen;
            sections[i] = s;
        }
    }

    public int sectionCount() {
        return sections.length;
    }

    public int keyCount(int sec) {
        return sections[sec].keyCount;
    }

    public int wordCount(int sec) {
        return sections[sec].wordCount;
    }

    private int keyOffset(int sec, int dirIdx) {
        return sections[sec].keyBlockOff + buf.getInt(sections[sec].dirOff + dirIdx * DIR_ITEM);
    }

    private int keyLength(int sec, int dirIdx) {
        return buf.getShort(sections[sec].dirOff + dirIdx * DIR_ITEM + 4) & 0xFFFF;
    }

    private int wordsOfKey(int sec, int dirIdx) {
        return buf.getShort(sections[sec].dirOff + dirIdx * DIR_ITEM + 6) & 0xFFFF;
    }

    private int wordIndexOfKey(int sec, int dirIdx) {
        return buf.getInt(sections[sec].dirOff + dirIdx * DIR_ITEM + 8);
    }

    private int compareKey(int sec, int dirIdx, byte[] target) {
        int off = keyOffset(sec, dirIdx);
        int len = keyLength(sec, dirIdx);
        int n = len < target.length ? len : target.length;
        for (int i = 0; i < n; i++) {
            int a = buf.get(off + i) & 0xFF;
            int b = target[i] & 0xFF;
            if (a != b) {
                return a < b ? -1 : 1;
            }
        }
        if (len == target.length) {
            return 0;
        }
        return len < target.length ? -1 : 1;
    }

    /** 第一个 >= key 的目录下标。 */
    private int lowerBound(int sec, byte[] key) {
        int lo = 0;
        int hi = sections[sec].keyCount - 1;
        int ans = sections[sec].keyCount;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (compareKey(sec, mid, key) >= 0) {
                ans = mid;
                hi = mid - 1;
            } else {
                lo = mid + 1;
            }
        }
        return ans;
    }

    private boolean startsWith(int sec, int dirIdx, byte[] prefix) {
        int len = keyLength(sec, dirIdx);
        if (len < prefix.length) {
            return false;
        }
        int off = keyOffset(sec, dirIdx);
        for (int i = 0; i < prefix.length; i++) {
            if ((buf.get(off + i) & 0xFF) != (prefix[i] & 0xFF)) {
                return false;
            }
        }
        return true;
    }

    private void readWords(int sec, int dirIdx, int limit, float factor, int mask, List<Candidate> out) {
        String hitKey = null;
        int n = wordsOfKey(sec, dirIdx);
        if (n > limit) {
            n = limit;
        }
        int indexBase = sections[sec].wordIndexOff + wordIndexOfKey(sec, dirIdx) * 4;
        for (int i = 0; i < n; i++) {
            int rel = buf.getInt(indexBase + i * 4);
            int abs = sections[sec].wordBlockOff + rel;
            int weight = buf.getInt(abs);
            int toneLen = buf.get(abs + 4) & 0xFF;
            int wordLen = buf.get(abs + 5) & 0xFF;
            int cat;
            int bodyOff;
            if (wideCat) {
                cat = buf.getShort(abs + 6) & 0xFFFF;
                bodyOff = abs + 8;
            } else {
                cat = buf.get(abs + 6) & 0xFF;
                bodyOff = abs + 7;
            }
            if (mask >= 0 && (cat & mask) == 0) {
                continue;
            }
            byte[] tmp = new byte[wordLen];
            for (int k = 0; k < wordLen; k++) {
                tmp[k] = buf.get(bodyOff + k);
            }
            String word = new String(tmp, StandardCharsets.UTF_8);
            String tones = "";
            if (toneLen > 0) {
                byte[] tt = new byte[toneLen];
                for (int k = 0; k < toneLen; k++) {
                    tt[k] = buf.get(bodyOff + wordLen + k);
                }
                tones = new String(tt, StandardCharsets.UTF_8);
            }
            Candidate c = new Candidate(word, weight, tones, weight * factor);
            c.cat = cat;
            if (hitKey == null) {
                int koff = keyOffset(sec, dirIdx);
                int klen = keyLength(sec, dirIdx);
                byte[] kb = new byte[klen];
                for (int k = 0; k < klen; k++) {
                    kb[k] = buf.get(koff + k);
                }
                hitKey = new String(kb, StandardCharsets.UTF_8);
            }
            c.key = hitKey;
            out.add(c);
        }
    }

    /** 精确匹配一个键。 */
    public int exact(int sec, String key, int limit, float factor, List<Candidate> out) {
        return exact(sec, key, limit, factor, -1, out);
    }

    public int exact(int sec, String key, int limit, float factor, int mask, List<Candidate> out) {
        int idx = lowerBound(sec, key.getBytes(StandardCharsets.UTF_8));
        if (idx >= sections[sec].keyCount) {
            return 0;
        }
        if (compareKey(sec, idx, key.getBytes(StandardCharsets.UTF_8)) != 0) {
            return 0;
        }
        int before = out.size();
        readWords(sec, idx, limit, factor, mask, out);
        return out.size() - before;
    }

    /** 前缀匹配。返回命中的键数。 */
    public int prefix(int sec, String prefix, int maxKeys, int maxWords, float factor, List<Candidate> out) {        return prefix(sec, prefix, maxKeys, maxWords, factor, -1, out);
    }

    public int prefix(int sec, String prefix, int maxKeys, int maxWords, float factor, int mask,
                      List<Candidate> out) {
        return prefixDeep(sec, prefix, maxKeys, maxWords, 3, factor, mask, out);
    }

    /** 前缀匹配，每个命中的键最多取 wordsPerKey 个词（笔画方案同码字多，需要比拼音取得多）。 */
    public int prefixDeep(int sec, String prefix, int maxKeys, int maxWords, int wordsPerKey,
                          float factor, int mask, List<Candidate> out) {
        byte[] p = prefix.getBytes(StandardCharsets.UTF_8);
        int idx = lowerBound(sec, p);
        int before = out.size();
        int keys = 0;
        while (idx < sections[sec].keyCount && keys < maxKeys) {
            if (!startsWith(sec, idx, p)) {
                break;
            }
            readWords(sec, idx, wordsPerKey, factor, mask, out);
            keys++;
            idx++;
            if (out.size() - before >= maxWords) {
                break;
            }
        }
        return keys;
    }

    /**
     * 按「词条文字」前缀扫全表（联想续写用）：返回所有以 prefix 开头、且比 prefix 更长的词，
     * 同一个词只留权重最高的一条，按权重从高到低取前 limit 条。
     */
    public int textPrefix(int sec, String prefix, int limit, int mask, List<Candidate> out) {
        int before = out.size();
        if (prefix == null || prefix.isEmpty()) return 0;
        byte[] pb = prefix.getBytes(StandardCharsets.UTF_8);
        java.util.LinkedHashMap<String, Candidate> best = new java.util.LinkedHashMap<String, Candidate>();
        int idxBase = sections[sec].wordIndexOff;
        int blkBase = sections[sec].wordBlockOff;
        int total = sections[sec].wordCount;
        for (int i = 0; i < total; i++) {
            int abs = blkBase + buf.getInt(idxBase + i * 4);
            int toneLen = buf.get(abs + 4) & 0xFF;
            int wordLen = buf.get(abs + 5) & 0xFF;
            if (wordLen <= pb.length) continue;
            int cat = wideCat ? (buf.getShort(abs + 6) & 0xFFFF) : (buf.get(abs + 6) & 0xFF);
            int bodyOff = wideCat ? abs + 8 : abs + 7;
            int hit = 0;
            while (hit < pb.length && (buf.get(bodyOff + hit) & 0xFF) == (pb[hit] & 0xFF)) {
                hit++;
            }
            if (hit < pb.length) continue;
            if (mask > 0 && (cat & mask) == 0) continue;
            byte[] tmp = new byte[wordLen];
            for (int k = 0; k < wordLen; k++) {
                tmp[k] = buf.get(bodyOff + k);
            }
            String word = new String(tmp, StandardCharsets.UTF_8);
            int weight = buf.getInt(abs);
            Candidate old = best.get(word);
            if (old != null && old.weight >= weight) continue;
            String tones = "";
            if (toneLen > 0) {
                byte[] tt = new byte[toneLen];
                for (int k = 0; k < toneLen; k++) {
                    tt[k] = buf.get(bodyOff + wordLen + k);
                }
                tones = new String(tt, StandardCharsets.UTF_8);
            }
            Candidate c = new Candidate(word, weight, tones, weight);
            c.cat = cat;
            best.put(word, c);
        }
        List<Candidate> found = new java.util.ArrayList<Candidate>(best.values());
        java.util.Collections.sort(found, new java.util.Comparator<Candidate>() {
            public int compare(Candidate a, Candidate b) {
                if (a.weight != b.weight) return b.weight - a.weight;
                return a.word.length() - b.word.length();
            }
        });
        for (int i = 0; i < found.size() && i < limit; i++) {
            out.add(found.get(i));
        }
        return out.size() - before;
    }

    public void close() {
        try {
            if (channel != null) channel.close();
        } catch (IOException ignored) {
        }
        try {
            if (raf != null) raf.close();
        } catch (IOException ignored) {
        }
    }
}

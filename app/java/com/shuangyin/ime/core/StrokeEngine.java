package com.shuangyin.ime.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 笔画输入引擎。
 *
 * 查询串由数字组成：1 横、2 竖、3 撇、4 捺、5 折（竖弯钩一类），0 为通配。
 * 词库每个词条是「每字前若干笔」的拼接串，因此逐笔输入等价于前缀匹配。
 */
public final class StrokeEngine {

    /** 词库里每字最多 8 笔，用户多点的笔画按前 8 笔处理，避免候选凭空消失。 */
    private static final int MAX_CODE = 8;
    /** 1~4 位前缀在词库里已物化成"该前缀字频前 40 字"的键。 */
    private static final int MATERIALIZED = 4;
    /** 分词组词时每段取几个字、中途保留多少个组合。 */
    private static final int SEG_CANDIDATES = 6;
    private static final int COMBO_CAP = 400;
    private static final int KEEP_PER_STEP = 60;

    private final BinDict dict;
    private int catMask = -1;

    public StrokeEngine(BinDict dict) {
        this.dict = dict;
    }

    public void setCatMask(int mask) {
        this.catMask = mask;
    }

    public List<Candidate> query(String code, int limit) {
        if (dict == null || code == null || code.isEmpty()) {
            return new ArrayList<Candidate>();
        }
        if (code.indexOf('|') >= 0) {
            return querySegments(code, limit);
        }
        return querySingle(code, limit);
    }

    /**
     * 分词后的多段查询：每段仍按单字笔顺取候选，再按段做组合，
     * 组合得分取各段中最低的那个，于是「一丨丨一丨｜一丨」能给出「黄手」「黄牛」这类词。
     */
    private List<Candidate> querySegments(String code, int limit) {
        List<String> segs = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < code.length(); i++) {
            char ch = code.charAt(i);
            if (ch == '|') {
                if (sb.length() > 0) {
                    segs.add(sb.toString());
                    sb.setLength(0);
                }
            } else {
                sb.append(ch);
            }
        }
        if (sb.length() > 0) {
            segs.add(sb.toString());
        }
        if (segs.size() == 0) {
            return new ArrayList<Candidate>();
        }
        if (segs.size() == 1) {
            return querySingle(segs.get(0), limit);
        }
        List<Candidate> combos = null;
        for (int i = 0; i < segs.size(); i++) {
            List<Candidate> part = querySingle(segs.get(i), SEG_CANDIDATES);
            if (part.isEmpty()) {
                return new ArrayList<Candidate>();
            }
            List<Candidate> next = new ArrayList<Candidate>();
            int base = combos == null ? 1 : combos.size();
            for (int a = 0; a < base; a++) {
                String prefix = combos == null ? "" : combos.get(a).word;
                float pscore = combos == null ? Float.MAX_VALUE : combos.get(a).score;
                for (int b = 0; b < part.size(); b++) {
                    Candidate word = part.get(b);
                    float score = Math.min(pscore, word.score);
                    next.add(new Candidate(prefix + word.word, (int) score, "", score));
                    if (next.size() >= COMBO_CAP) {
                        break;
                    }
                }
                if (next.size() >= COMBO_CAP) {
                    break;
                }
            }
            Collections.sort(next, new Comparator<Candidate>() {
                @Override
                public int compare(Candidate x, Candidate y) {
                    if (y.score > x.score) return 1;
                    if (y.score < x.score) return -1;
                    return 0;
                }
            });
            if (next.size() > KEEP_PER_STEP) {
                next = new ArrayList<Candidate>(next.subList(0, KEEP_PER_STEP));
            }
            combos = next;
        }
        if (combos == null) {
            return new ArrayList<Candidate>();
        }
        if (combos.size() > limit) {
            return new ArrayList<Candidate>(combos.subList(0, limit));
        }
        return combos;
    }

    private List<Candidate> querySingle(String code, int limit) {
        List<Candidate> out = new ArrayList<Candidate>();
        String c = trimWildcards(code);
        if (c.length() > MAX_CODE) {
            c = c.substring(0, MAX_CODE);
        }
        Map<String, Candidate> seen = new LinkedHashMap<String, Candidate>();
        if (c.indexOf('0') < 0) {
            collect(c, seen);
        } else {
            expand(c, 0, seen);
        }
        for (Candidate cand : seen.values()) {
            out.add(cand);
        }
        Collections.sort(out, new Comparator<Candidate>() {
            @Override
            public int compare(Candidate a, Candidate b) {
                float d = b.score - a.score;
                if (d > 0) return 1;
                if (d < 0) return -1;
                return a.word.length() - b.word.length();
            }
        });
        if (out.size() > limit) {
            return new ArrayList<Candidate>(out.subList(0, limit));
        }
        return out;
    }

    /** 通配最多两个，多按的当作误触丢掉。 */
    private static String trimWildcards(String code) {
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (int i = 0; i < code.length(); i++) {
            char ch = code.charAt(i);
            if (ch == '0') {
                if (n >= 2) continue;
                n++;
            }
            sb.append(ch);
        }
        return sb.length() == 0 ? "1" : sb.toString();
    }

    private void collect(String code, Map<String, Candidate> seen) {
        List<Candidate> tmp = new ArrayList<Candidate>();
        if (code.length() <= MATERIALIZED) {
            // 短码走物化键：一次精确命中，取到的就是该前缀下字频最高的字
            dict.exact(0, code, 60, 1f, catMask, tmp);
        } else {
            dict.prefixDeep(0, code, 1200, 400, 10, 1f, catMask, tmp);
        }
        for (int i = 0; i < tmp.size(); i++) {
            Candidate cand = tmp.get(i);
            Candidate old = seen.get(cand.word);
            if (old == null) {
                seen.put(cand.word, cand);
            } else if (cand.score > old.score) {
                seen.put(cand.word, cand);
            }
        }
    }

    private void expand(String code, int from, Map<String, Candidate> seen) {
        int pos = code.indexOf('0', from);
        if (pos < 0) {
            collect(code, seen);
            return;
        }
        char[] arr = code.toCharArray();
        for (char d = '1'; d <= '5'; d++) {
            arr[pos] = d;
            expand(new String(arr), pos + 1, seen);
        }
    }
}

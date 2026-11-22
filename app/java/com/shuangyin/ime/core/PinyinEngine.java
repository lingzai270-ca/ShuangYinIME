package com.shuangyin.ime.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 全拼引擎：精确 / 前缀 / 简拼 / 模糊音 / 长句分词。 */
public final class PinyinEngine {
    private final BinDict dict;
    private final Set<String> syllables = new HashSet<String>();
    private static final int CAT_HIGH = 1;
    private static final int CAT_TAIL = 16;
    /** 5 个频级都算常用来源，统一加分（跟上一版按语义分类时的相对排序保持一致）。 */
    private static final int CAT_MAIN = 1 | 2 | 4 | 8 | 16;
    /** 启用的词库分类位（-1 表示不限）。 */
    private int catMask = -1;

    public PinyinEngine(BinDict dict, Set<String> syllableTable) {
        this.dict = dict;
        this.syllables.addAll(syllableTable);
    }

    public void setCatMask(int mask) {
        catMask = mask;
    }

    public static String normalize(String raw) {
        if (raw == null) return "";
        StringBuilder sb = new StringBuilder(raw.length());
        String s = raw.toLowerCase().replace("ü", "v").replace("u:", "v");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'a' && c <= 'z') {
                sb.append(c);
            } else if (c == '\'' || c == ' ') {
                continue;
            }
        }
        return sb.toString();
    }

    /**
     * 音节切分：按「音节个数最少」切，同分优先取长音节。
     * 老的贪心最长匹配会把 min+gong 切成 ming+o+ng（个数多一个），
     * 连打 zhonghuarenmingongheguo 就拼成「中华人民工和国」这种串。
     */
    private List<String> segment(String q) {
        return split(q, syllables);
    }

    /**
     * 音节切分：按「音节个数最少」切，同分优先取长音节。供引擎内部与读音重建共用。
     */
    public static List<String> split(String q, Set<String> syls) {
        if (q == null || q.isEmpty() || syls == null) return null;
        int n = q.length();
        int[] memo = new int[n + 1];
        java.util.Arrays.fill(memo, -2);
        int cnt = splitCount(q, 0, memo, syls);
        if (cnt <= 0) return null;
        ArrayList<String> parts = new ArrayList<String>(cnt);
        int i = 0;
        while (i < n) {
            int bestLen = 0;
            int bestRest = Integer.MAX_VALUE;
            for (int len = Math.min(6, n - i); len >= 1; len--) {
                String cand = q.substring(i, i + len);
                if (!syls.contains(cand)) continue;
                int rest = splitCount(q, i + len, memo, syls);
                if (rest < 0) continue;
                if (rest < bestRest) {
                    bestRest = rest;
                    bestLen = len;
                }
            }
            if (bestLen == 0) return null;
            parts.add(q.substring(i, i + bestLen));
            i += bestLen;
        }
        return parts;
    }

    /** 从 i 开始最少要几个音节；切不出返回 -1。 */
    private static int splitCount(String q, int i, int[] memo, Set<String> syls) {
        if (i == q.length()) return 0;
        if (memo[i] != -2) return memo[i];
        memo[i] = -1;
        int best = Integer.MAX_VALUE;
        for (int len = Math.min(6, q.length() - i); len >= 1; len--) {
            String cand = q.substring(i, i + len);
            if (!syls.contains(cand)) continue;
            int rest = splitCount(q, i + len, memo, syls);
            if (rest >= 0 && rest + 1 < best) best = rest + 1;
        }
        memo[i] = best == Integer.MAX_VALUE ? -1 : best;
        return memo[i];
    }

    /** 单音节模糊变体（只生成真实存在的音节）。 */
    private List<String> fuzzySyllable(String syl) {
        ArrayList<String> out = new ArrayList<String>();
        int i = 0;
        StringBuilder sb;
        if (syl.startsWith("zh")) {
            add(out, "z" + syl.substring(2));
        } else if (syl.startsWith("ch")) {
            add(out, "c" + syl.substring(2));
        } else if (syl.startsWith("sh")) {
            add(out, "s" + syl.substring(2));
        } else if (syl.startsWith("z")) {
            add(out, "zh" + syl.substring(1));
        } else if (syl.startsWith("c")) {
            add(out, "ch" + syl.substring(1));
        } else if (syl.startsWith("s")) {
            add(out, "sh" + syl.substring(1));
        } else if (syl.startsWith("n")) {
            add(out, "l" + syl.substring(1));
        } else if (syl.startsWith("l")) {
            add(out, "n" + syl.substring(1));
        } else if (syl.startsWith("f")) {
            add(out, "h" + syl.substring(1));
        } else if (syl.startsWith("h")) {
            add(out, "f" + syl.substring(1));
        }
        // 前后鼻音
        if (syl.endsWith("ang")) {
            add(out, syl.substring(0, syl.length() - 3) + "an");
        } else if (syl.endsWith("an")) {
            add(out, syl.substring(0, syl.length() - 2) + "ang");
        }
        if (syl.endsWith("eng")) {
            add(out, syl.substring(0, syl.length() - 3) + "en");
        } else if (syl.endsWith("en")) {
            add(out, syl.substring(0, syl.length() - 2) + "eng");
        }
        if (syl.endsWith("ing")) {
            add(out, syl.substring(0, syl.length() - 3) + "in");
        } else if (syl.endsWith("in")) {
            add(out, syl.substring(0, syl.length() - 2) + "ing");
        }
        return out;
    }

    private void add(List<String> out, String s) {
        if (syllables.contains(s) && !out.contains(s)) {
            out.add(s);
        }
    }

    private String[] fuzzyKeys(String q) {
        List<String> parts = segment(q);
        if (parts == null) return new String[0];
        ArrayList<String> combos = new ArrayList<String>();
        combos.add("");
        for (int i = 0; i < parts.size(); i++) {
            String p = parts.get(i);
            ArrayList<String> opts = new ArrayList<String>();
            opts.add(p);
            opts.addAll(fuzzySyllable(p));
            if (combos.size() * opts.size() > 12) {
                // 组合过多时只保留原始音节，保证响应速度
                for (int k = 0; k < combos.size(); k++) {
                    combos.set(k, combos.get(k) + p);
                }
                continue;
            }
            ArrayList<String> next = new ArrayList<String>(combos.size() * opts.size());
            for (int c = 0; c < combos.size(); c++) {
                for (int o = 0; o < opts.size(); o++) {
                    next.add(combos.get(c) + opts.get(o));
                }
            }
            combos = next;
        }
        combos.remove(q);
        return combos.toArray(new String[0]);
    }

    private static String abbrKey(String q) {
        return q.replace("zh", "z").replace("ch", "c").replace("sh", "s");
    }

    private void merge(Map<String, Candidate> best, List<Candidate> list) {
        for (int i = 0; i < list.size(); i++) {
            Candidate c = list.get(i);
            Candidate old = best.get(c.word);
            if (old == null || c.score > old.score) {
                best.put(c.word, c);
            }
        }
    }

    private Candidate sentenceCandidate(List<String> parts, String q) {
        if (parts == null || parts.size() < 2) return null;
        int n = parts.size();
        double[] best = new double[n + 1];
        String[] word = new String[n + 1];
        int[] prev = new int[n + 1];
        for (int i = 1; i <= n; i++) best[i] = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            if (best[i] == Double.NEGATIVE_INFINITY) continue;
            StringBuilder key = new StringBuilder();
            for (int len = 1; len <= 4 && i + len <= n; len++) {
                key.append(parts.get(i + len - 1));
                List<Candidate> tmp = new ArrayList<Candidate>();
                dict.exact(0, key.toString(), 2, 1.0f, catMask, tmp);
                if (tmp.isEmpty()) continue;
                Candidate top = tmp.get(0);
                // 以"概率"（权重/60000）取对数；多音节词给一点点音节长度补偿（0.45/音节）。
                // 补偿窗口很窄：够让「中华」「人民」这种常用词压过逐字拼，又不足以把
                // 「核果」「恭贺」这种低频词塞进句子，于是「中华人民共和国」是
                // 中华+人民+共+和+国 拼出来的，而不是 中华人民工核果 / 共核果
                double sc = Math.log(Math.max(1, top.weight) / 60000.0) + 0.45 * (len - 1);
                if (best[i] + sc > best[i + len]) {
                    best[i + len] = best[i] + sc;
                    word[i + len] = top.word;
                    prev[i + len] = i;
                }
            }
        }
        if (best[n] == Double.NEGATIVE_INFINITY) return null;
        StringBuilder sb = new StringBuilder();
        int p = n;
        while (p > 0) {
            sb.insert(0, word[p]);
            p = prev[p];
        }
        if (sb.length() == 0) return null;
        float geo = (float) Math.exp(best[n] / n);
        Candidate c = new Candidate(sb.toString(), (int) geo, "", geo);
        c.key = q;
        return c;
    }

    private static void clamp(List<Candidate> list, float cap) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).score > cap) list.get(i).score = cap;
        }
    }

    public List<Candidate> query(String raw, boolean fuzzy, boolean abbr, int limit) {
        return query(raw, fuzzy, abbr, limit, false);
    }

    /** 第一个完整音节的字母长度（单字模式逐字上屏时用）；切不出音节就返回整串长度。 */
    public int firstSyllableLength(String raw) {
        String q = normalize(raw);
        if (q.isEmpty()) return 0;
        List<String> segs = segment(q);
        if (segs == null || segs.isEmpty()) return q.length();
        return segs.get(0).length();
    }

    /**
     * @param singleOnly true 只出单字，且查词键只取第一个音节（单字高频优先）
     */
    public List<Candidate> query(String raw, boolean fuzzy, boolean abbr, int limit,
                                 boolean singleOnly) {
        String full = normalize(raw);
        if (full.isEmpty()) return Collections.emptyList();

        // 单字模式：只查第一个音节，候选全是单字，按字频（weight）排序
        String q = full;
        if (singleOnly) {
            List<String> segs = segment(full);
            if (segs != null && segs.size() > 1) {
                q = segs.get(0);
            }
        }

        Map<String, Candidate> best = new HashMap<String, Candidate>();
        List<Candidate> tmp = new ArrayList<Candidate>();
        List<String> parts = segment(q);
        int partsCount = parts == null ? 0 : parts.size();

        // 候选上限可达 100，精确键也要多取一些，否则光靠前缀扩展凑不满
        dict.exact(0, q, Math.min(40, Math.max(8, limit)), 1.55f, catMask, tmp);
        merge(best, tmp);
        float exactBest = 0f;
        for (int i = 0; i < tmp.size(); i++) {
            if (tmp.get(i).score > exactBest) exactBest = tmp.get(i).score;
        }
        tmp.clear();
        // 输入很短时，扩展长尾词会把常用词挤出候选，前缀扩展先屏蔽掉这一类；
        // 短拼音的 wo* 键特别多，键数上限也要放大，否则 wom(en) 之类根本扫不到
        int prefixMask = partsCount >= 3 ? catMask : (catMask & ~CAT_TAIL);
        int maxKeys = q.length() <= 3 ? 9000 : 1200;
        int maxWords = q.length() <= 3 ? 4000 : 600;
        dict.prefix(0, q, maxKeys, maxWords,
                partsCount == 1 ? 0.32f : 0.74f, prefixMask, tmp);
        if (exactBest > 0) clamp(tmp, exactBest * 0.9f);
        merge(best, tmp);

        // 单个音节输入时，前缀扫描会被 wo* 这类巨量键卡住，扫不到 women 之类的常用词；
        // 这里直接按「q + 一个音节」逐音节精确查一遍，代价是 400 多次二分，几毫秒。
        if (partsCount == 1) {
            float cap = exactBest > 0 ? exactBest * 0.95f : Float.MAX_VALUE;
            for (String s : syllables) {
                tmp.clear();
                dict.exact(0, q + s, 4, 0.8f, catMask, tmp);
                clamp(tmp, cap);
                merge(best, tmp);
            }
        }

        if (fuzzy) {
            float cap = exactBest > 0 ? exactBest * 0.42f : Float.MAX_VALUE;
            String[] keys = fuzzyKeys(q);
            for (int i = 0; i < keys.length; i++) {
                tmp.clear();
                dict.exact(0, keys[i], 6, 0.8f, catMask, tmp);
                clamp(tmp, cap);
                merge(best, tmp);
                if (best.size() < limit * 3) {
                    tmp.clear();
                    dict.prefix(0, keys[i], 120, 200, 0.6f, catMask, tmp);
                    clamp(tmp, cap);
                    merge(best, tmp);
                }
            }
        }

        if (abbr && q.length() >= 2) {
            float cap = exactBest > 0 ? exactBest * 0.8f : Float.MAX_VALUE;
            tmp.clear();
            dict.exact(1, abbrKey(q), 10, 0.58f, catMask, tmp);
            clamp(tmp, cap);
            merge(best, tmp);
            if (q.length() >= 3) {
                tmp.clear();
                dict.prefix(1, abbrKey(q), 40, 80, 0.5f, catMask, tmp);
                clamp(tmp, cap);
                merge(best, tmp);
            }
        }

        Candidate sc = sentenceCandidate(parts, q);
        if (sc != null) {
            if (exactBest > 0 && sc.score > exactBest * 0.55f) {
                sc = new Candidate(sc.word, (int) (exactBest * 0.55f), sc.tones,
                        exactBest * 0.55f);
            }
            Candidate old = best.get(sc.word);
            if (old == null || sc.score > old.score) best.put(sc.word, sc);
        }

        List<Candidate> out = new ArrayList<Candidate>(best.values());
        for (int i = 0; i < out.size(); i++) {
            Candidate c = out.get(i);
            // 常用来源（8105 / base）那一层是"真正常用"的，权重口径比长尾词低，
            // 这里补一个倍率，避免常用词被扩展长尾词压下去
            if ((c.cat & CAT_MAIN) != 0) {
                c.score *= 2.2f;
            }
            int len = c.word.codePointCount(0, c.word.length());
            if (partsCount > 0) {
                if (len == partsCount) {
                    c.score *= 1.22f;
                } else if (len > partsCount) {
                    // 多出来的每个字都按指数衰减，压制"输入两三个音节却给出长短语"的噪声
                    c.score *= (float) Math.pow(0.70, len - partsCount);
                }
            }
            if (partsCount == 1 && len > 1) {
                c.score *= 0.7f;
            }
            if (q.length() >= 5 && len == 1) {
                c.score *= 0.42f;
            }
            if (q.length() == 1 && len > 2) {
                c.score *= 0.5f;
            }
        }

        // 单字模式：只留单字，并且按字频（weight）从高到低排
        if (singleOnly) {
            Iterator<Candidate> it = out.iterator();
            while (it.hasNext()) {
                Candidate c = it.next();
                if (c.word.codePointCount(0, c.word.length()) != 1) {
                    it.remove();
                    continue;
                }
                c.score = c.weight;
            }
        }

        Collections.sort(out, new Comparator<Candidate>() {
            @Override
            public int compare(Candidate a, Candidate b) {
                if (a.score != b.score) return a.score < b.score ? 1 : -1;
                int la = a.word.codePointCount(0, a.word.length());
                int lb = b.word.codePointCount(0, b.word.length());
                if (la != lb) return la < lb ? -1 : 1;
                return a.weight < b.weight ? 1 : (a.weight == b.weight ? 0 : -1);
            }
        });
        // 多音节输入：补几个首音节的单字，方便逐字挑（要找的字往往就在这一批里）
        if (!singleOnly && partsCount >= 2 && parts != null && parts.get(0).length() >= 2) {
            tmp.clear();
            dict.exact(0, parts.get(0), 6, 1.0f, catMask, tmp);
            float base = out.isEmpty() ? 1f : Math.max(1f, out.get(0).weight * 0.6f);
            int inserted = 0;
            for (int i = 0; i < tmp.size(); i++) {
                Candidate c = tmp.get(i);
                if (c.word.codePointCount(0, c.word.length()) != 1) continue;
                boolean dup = false;
                for (int j = 0; j < out.size(); j++) {
                    if (out.get(j).word.equals(c.word)) { dup = true; break; }
                }
                if (dup) continue;
                c.score = base * 0.55f;
                // 插在第一条之后：整句/整词仍占首位，首音节的单字紧跟着，方便逐字挑
                out.add(Math.min(1 + inserted, out.size()), c);
                inserted++;
            }
        }

        if (out.size() > limit) {
            return new ArrayList<Candidate>(out.subList(0, limit));
        }
        return out;
    }

    public List<String> segmentForDisplay(String raw) {
        return segment(normalize(raw));
    }
}

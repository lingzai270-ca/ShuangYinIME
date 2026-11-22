package com.shuangyin.ime.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * 注音引擎：ㄅㄆㄇ 输入。
 *
 * 两条取词路径合并：
 *   1. 注音词库（zhuyin.bin）：键是注音串，直接出字词；
 *   2. 拼音词库（pinyin.bin）：把已打完的音节按「声母+介母+韵母」的拼写规则转成拼音键再查，
 *      这样注音能用上主词库的十万多条简体词条，读音拼音也不再缺。
 *
 * 权重按汉语拼音的拼读结构分层（五类）：
 *   声母：输入开头必须是同一组声母（前缀匹配天然保证），按最长匹配切分；
 *   介母：ㄧ ㄨ ㄩ，声母后紧跟介母时按「声母+介母」累计，转拼音时 ㄐㄑㄒ 后的 ㄩ 写作 u；
 *   韵母：与介母合起来占满一个音节，音节打完才按「单字对应单音节」的比例加权；
 *   整体认读：ㄓ ㄔ ㄕ ㄖ ㄗ ㄘ ㄙ（zhi/chi/shi/ri/zi/ci/si）与 ㄧ ㄨ ㄩ ㄧㄝ ㄩㄝ ㄩㄢ ㄧㄣ ㄩㄣ ㄧㄥ，
 *             单符号即一整字，命中给额外权重，避免被「声母+韵母」的长音节盖住；
 *   声调：1~5（5 为轻声），按较短长度对齐逐音节比较，命中强提权、不命中强降权。
 */
public final class ZhuyinEngine {
    private final BinDict zhy;
    /** 拼音词库，可为 null（只查注音库）。 */
    private final BinDict pin;

    public ZhuyinEngine(BinDict zhy) {
        this(zhy, null);
    }

    public ZhuyinEngine(BinDict zhy, BinDict pin) {
        this.zhy = zhy;
        this.pin = pin;
    }

    /** 启用的词库分类位（-1 表示不限）。 */
    private int catMask = -1;

    public void setCatMask(int mask) {
        catMask = mask;
    }

    public static boolean isBopomofo(char c) {
        return (c >= '\u3105' && c <= '\u3129');
    }

    private static boolean isTone(char c) {
        return c >= '1' && c <= '5';
    }

    private static boolean isJQX(char c) {
        return c == 'ㄐ' || c == 'ㄑ' || c == 'ㄒ';
    }

    /** 音节切分结果。 */
    private static final class Shape {
        final List<String> syls = new ArrayList<String>();
        String res = "";
        /** 0 无残留；1 只打了声母；2 声母+介母；3 只打了介母；4 其它。 */
        int resKind;
    }

    private static int resKindOf(String res) {
        int n = res.length();
        if (n == 1) {
            char c = res.charAt(0);
            if (ZhuyinSyllables.isInitial(c)) return 1;
            if (ZhuyinSyllables.isMedial(c)) return 3;
            return 4;
        }
        if (n == 2) {
            char c0 = res.charAt(0);
            char c1 = res.charAt(1);
            if (ZhuyinSyllables.isInitial(c0) && ZhuyinSyllables.isMedial(c1)) return 2;
        }
        return 4;
    }

    private static Shape analyze(String key) {
        Shape s = new Shape();
        int i = 0;
        int n = key.length();
        while (i < n) {
            int len = 0;
            int max = Math.min(4, n - i);
            for (int L = max; L >= 1; L--) {
                if (ZhuyinSyllables.isSyllable(key.substring(i, i + L))) {
                    len = L;
                    break;
                }
            }
            if (len == 0) {
                break;
            }
            s.syls.add(key.substring(i, i + len));
            i += len;
        }
        if (i < n) {
            s.res = key.substring(i);
            s.resKind = resKindOf(s.res);
        }
        return s;
    }

    /** 未打完的音节按拼音拼写规则转成前缀（用于查拼音词库）。 */
    private static String resPinyin(String res, int kind) {
        if (res == null || res.length() == 0) {
            return "";
        }
        char c0 = res.charAt(0);
        if (kind == 1) {
            return ZhuyinSyllables.initialLetters(c0);
        }
        if (kind == 3) {
            return ZhuyinSyllables.medialLetters(c0);
        }
        if (kind == 2 && res.length() >= 2) {
            String ini = ZhuyinSyllables.initialLetters(c0);
            char m = res.charAt(1);
            if (m == 'ㄧ') return ini + "i";
            if (m == 'ㄨ') return ini + "u";
            if (m == 'ㄩ') return ini + (isJQX(c0) ? "u" : "v");
        }
        return "";
    }

    /** 注音音节串转拼音键；有无法转换的音节时返回空串。 */
    private static String pinKeyOf(Shape s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.syls.size(); i++) {
            String p = ZhuyinSyllables.pinyin(s.syls.get(i));
            if (p == null) {
                return "";
            }
            sb.append(p);
        }
        sb.append(resPinyin(s.res, s.resKind));
        return sb.toString();
    }

    /** 把一个注音键切成音节（用于和候选键对齐结构）。 */
    private static List<String> splitBpmf(String key) {
        List<String> out = new ArrayList<String>();
        int i = 0;
        while (i < key.length()) {
            int len = 0;
            int max = Math.min(4, key.length() - i);
            for (int L = max; L >= 1; L--) {
                if (ZhuyinSyllables.isSyllable(key.substring(i, i + L))) {
                    len = L;
                    break;
                }
            }
            if (len == 0) {
                break;
            }
            out.add(key.substring(i, i + len));
            i += len;
        }
        return out;
    }

    /** 拼音键切片（拼音词库的键没有分隔符，按合法音节最长匹配）。 */
    private static List<String> splitPinyin(String key) {
        List<String> out = new ArrayList<String>();
        int i = 0;
        while (i < key.length()) {
            int len = 0;
            int max = Math.min(6, key.length() - i);
            for (int L = max; L >= 1; L--) {
                if (ZhuyinSyllables.isPinyinSyllable(key.substring(i, i + L))) {
                    len = L;
                    break;
                }
            }
            if (len == 0) {
                break;
            }
            out.add(key.substring(i, i + len));
            i += len;
        }
        return out;
    }

    /** 补全候选的拼音读数：注音键按对照表转，拼音键原样。 */
    private static void fillPinyin(Candidate c) {
        if (c.key == null || c.key.length() == 0) {
            return;
        }
        if (!isBopomofo(c.key.charAt(0))) {
            c.pinyin = c.key;
            return;
        }
        StringBuilder sb = new StringBuilder();
        List<String> syls = splitBpmf(c.key);
        for (int i = 0; i < syls.size(); i++) {
            String p = ZhuyinSyllables.pinyin(syls.get(i));
            if (p == null) {
                return;
            }
            sb.append(p);
        }
        c.pinyin = sb.toString();
    }

    /** 声调数据是否可信：长度等于字数且每位都是 1~5。 */
    private static boolean toneConfirmed(Candidate c) {
        if (c.tones == null || c.tones.length() == 0) {
            return false;
        }
        int n = c.word.codePointCount(0, c.word.length());
        if (c.tones.length() != n) {
            return false;
        }
        for (int i = 0; i < n; i++) {
            char t = c.tones.charAt(i);
            if (t < '1' || t > '5') {
                return false;
            }
        }
        return true;
    }

    /** 该候选的声调是否命中用户按下的声调（只比能对齐的前几位）。 */
    private static boolean toneHit(Candidate c, String userTones) {
        if (userTones == null || userTones.length() == 0 || !toneConfirmed(c)) {
            return false;
        }
        int n = Math.min(userTones.length(), c.tones.length());
        for (int t = 0; t < n; t++) {
            if (userTones.charAt(t) != c.tones.charAt(t)) {
                return false;
            }
        }
        return n > 0;
    }

    /**
     * 两本词库同名候选合并：按分数留最高的一条；同时记录该词是否存在声调命中的读音
     * （多音字同一字会在词库里分成多条，声调命中那条分数低，但声调要算它命中）。
     */
    private void merge(Map<String, Candidate> best, List<Candidate> list, String userTones,
            Map<String, String> hits) {
        for (int i = 0; i < list.size(); i++) {
            Candidate c = list.get(i);
            Candidate old = best.get(c.word);
            if (old == null || c.score > old.score) {
                best.put(c.word, c);
            }
            if (!hits.containsKey(c.word) && toneHit(c, userTones)) {
                hits.put(c.word, c.tones);
            }
        }
    }

    /** 结构分：只对注音键候选做，中文候选键与输入音节对齐比较声母/介母/韵母/整体认读。 */
    private static void structureScore(Candidate c, Shape sh, int want) {
        if (sh.syls.isEmpty()) {
            return;
        }
        int idx = sh.syls.size() - 1;
        String inS = sh.syls.get(idx);
        int inKind = ZhuyinSyllables.kind(inS);

        if (c.key != null && c.key.length() > 0 && isBopomofo(c.key.charAt(0))) {
            List<String> cs = splitBpmf(c.key);
            if (idx < cs.size()) {
                String cS = cs.get(idx);
                if (inS.equals(cS)) {
                    c.score *= 1.10f;                       // 声母+介母+韵母 全对
                    if (hasMedial(inS)) {
                        c.score *= 1.05f;                   // 介母也打全了
                    }
                    if (inKind == ZhuyinSyllables.KIND_INTEGRAL) {
                        c.score *= 1.12f;                   // 整体认读音节完全命中
                    }
                } else if (cS.startsWith(inS)) {
                    c.score *= 0.82f;                       // 音节被拉长（韵母没打完就接下一字）
                } else {
                    c.score *= 0.85f;
                }
            }
        } else if (c.key != null && c.key.length() > 0) {
            List<String> cs = splitPinyin(c.key);
            String inPin = ZhuyinSyllables.pinyin(inS);
            if (idx < cs.size() && inPin != null) {
                if (inPin.equals(cs.get(idx))) {
                    c.score *= 1.05f;
                } else if (cs.get(idx).startsWith(inPin)) {
                    c.score *= 0.85f;
                } else {
                    c.score *= 0.9f;
                }
            }
        }

        // 单音节时的长词抑制、三音节以上时的单字抑制
        int len = c.word.codePointCount(0, c.word.length());
        if (want == 1 && len >= 3) {
            c.score *= 0.55f;
        }
        if (want >= 3 && len == 1) {
            c.score *= 0.5f;
        }
    }

    /** 音节里是否带介母（ㄧㄨㄩ 出现在声母之后，或本身就是介母开头）。 */
    private static boolean hasMedial(String syl) {
        if (syl.length() == 0) {
            return false;
        }
        if (ZhuyinSyllables.isMedial(syl.charAt(0))) {
            return true;
        }
        return syl.length() >= 2 && ZhuyinSyllables.isMedial(syl.charAt(1));
    }

    public List<Candidate> query(String buffer, int limit) {
        StringBuilder key = new StringBuilder();
        StringBuilder tones = new StringBuilder();
        for (int i = 0; i < buffer.length(); i++) {
            char c = buffer.charAt(i);
            if (isTone(c)) {
                tones.append(c);
            } else if (isBopomofo(c)) {
                key.append(c);
            }
        }
        if (key.length() == 0) {
            return Collections.emptyList();
        }

        String k = key.toString();
        String userTones = tones.toString();
        Shape sh = analyze(k);
        String pinKey = pinKeyOf(sh);
        int want = sh.syls.size() + (sh.res.length() > 0 ? 1 : 0);

        Map<String, Candidate> best = new HashMap<String, Candidate>();
        Map<String, String> hits = new HashMap<String, String>();
        List<Candidate> tmp = new ArrayList<Candidate>();

        zhy.exact(0, k, 24, 1.55f, catMask, tmp);
        merge(best, tmp, userTones, hits);
        tmp.clear();
        zhy.prefix(0, k, 240, 420, 0.62f, catMask, tmp);
        merge(best, tmp, userTones, hits);

        if (pin != null && pinKey.length() > 0) {
            tmp.clear();
            pin.exact(0, pinKey, 24, 1.6f, catMask, tmp);
            merge(best, tmp, userTones, hits);
            tmp.clear();
            pin.prefix(0, pinKey, 120, 240, 0.6f, catMask, tmp);
            merge(best, tmp, userTones, hits);
        }

        List<Candidate> out = new ArrayList<Candidate>(best.values());
        for (int i = 0; i < out.size(); i++) {
            Candidate c = out.get(i);
            int len = c.word.codePointCount(0, c.word.length());
            fillPinyin(c);

            // 1) 声调：该词任一声调命中的读音都算命中，命中强提权、不命中强降权
            if (userTones.length() > 0) {
                String hitTones = hits.get(c.word);
                if (hitTones != null) {
                    c.score *= 2.2f;
                    c.tones = hitTones;
                } else if (toneConfirmed(c)) {
                    c.score *= 0.30f;
                } else {
                    c.score *= 0.72f;   // 声调数据缺失/不可信，别让坏数据置顶
                }
            }

            // 2) 音节数：占满全部已输入音节（单音节对单字、多音节对成词）
            if (want > 0 && len == want) {
                c.score *= (want >= 2 ? 2.4f : 1.6f);
            } else if (want > 0 && len == want + 1) {
                c.score *= 0.85f;
            } else if (want > 0 && len >= want + 2) {
                c.score *= 0.6f;
            }

            // 3) 整体认读：单符号即一整字
            boolean lastIntegral = !sh.syls.isEmpty()
                    && ZhuyinSyllables.isIntegral(sh.syls.get(sh.syls.size() - 1));
            if (sh.res.length() == 0 && lastIntegral && len == want) {
                c.score *= 1.15f;
            }

            // 4) 声母 / 介母 / 韵母 / 整体认读的结构对齐
            structureScore(c, sh, want);
        }
        Collections.sort(out, new Comparator<Candidate>() {
            @Override
            public int compare(Candidate a, Candidate b) {
                if (a.score != b.score) {
                    return a.score < b.score ? 1 : -1;
                }
                return a.weight < b.weight ? 1 : (a.weight == b.weight ? 0 : -1);
            }
        });
        if (out.size() > limit) {
            return new ArrayList<Candidate>(out.subList(0, limit));
        }
        return out;
    }
}

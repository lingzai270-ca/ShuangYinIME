package com.shuangyin.ime.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * 日语输入：罗马字 → 平假名 → 片假名，并用 Mozc 词典查汉字候选。
 */
public final class JapaneseEngine {

    private static final Map<String, String> TBL = new HashMap<String, String>();

    static {
        String[][] rows = {
                {"a", "あ"}, {"i", "い"}, {"u", "う"}, {"e", "え"}, {"o", "お"},
                {"ka", "か"}, {"ki", "き"}, {"ku", "く"}, {"ke", "け"}, {"ko", "こ"},
                {"kya", "きゃ"}, {"kyu", "きゅ"}, {"kyo", "きょ"},
                {"sa", "さ"}, {"si", "し"}, {"shi", "し"}, {"su", "す"}, {"se", "せ"}, {"so", "そ"},
                {"sya", "しゃ"}, {"sha", "しゃ"}, {"syu", "しゅ"}, {"shu", "しゅ"},
                {"syo", "しょ"}, {"sho", "しょ"},
                {"ta", "た"}, {"ti", "ち"}, {"chi", "ち"}, {"tu", "つ"}, {"tsu", "つ"},
                {"te", "て"}, {"to", "と"},
                {"tya", "ちゃ"}, {"cha", "ちゃ"}, {"tyu", "ちゅ"}, {"chu", "ちゅ"},
                {"tyo", "ちょ"}, {"cho", "ちょ"},
                {"na", "な"}, {"ni", "に"}, {"nu", "ぬ"}, {"ne", "ね"}, {"no", "の"},
                {"nya", "にゃ"}, {"nyu", "にゅ"}, {"nyo", "にょ"},
                {"ha", "は"}, {"hi", "ひ"}, {"hu", "ふ"}, {"fu", "ふ"}, {"he", "へ"}, {"ho", "ほ"},
                {"hya", "ひゃ"}, {"hyu", "ひゅ"}, {"hyo", "ひょ"},
                {"ma", "ま"}, {"mi", "み"}, {"mu", "む"}, {"me", "め"}, {"mo", "も"},
                {"mya", "みゃ"}, {"myu", "みゅ"}, {"myo", "みょ"},
                {"ya", "や"}, {"yu", "ゆ"}, {"yo", "よ"},
                {"ra", "ら"}, {"ri", "り"}, {"ru", "る"}, {"re", "れ"}, {"ro", "ろ"},
                {"rya", "りゃ"}, {"ryu", "りゅ"}, {"ryo", "りょ"},
                {"wa", "わ"}, {"wi", "うぃ"}, {"we", "うぇ"}, {"wo", "を"},
                {"ga", "が"}, {"gi", "ぎ"}, {"gu", "ぐ"}, {"ge", "げ"}, {"go", "ご"},
                {"gya", "ぎゃ"}, {"gyu", "ぎゅ"}, {"gyo", "ぎょ"},
                {"za", "ざ"}, {"zi", "じ"}, {"ji", "じ"}, {"zu", "ず"}, {"ze", "ぜ"}, {"zo", "ぞ"},
                {"zya", "じゃ"}, {"ja", "じゃ"}, {"zyu", "じゅ"}, {"ju", "じゅ"},
                {"zyo", "じょ"}, {"jo", "じょ"},
                {"da", "だ"}, {"di", "ぢ"}, {"du", "づ"}, {"de", "で"}, {"do", "ど"},
                {"dya", "ぢゃ"}, {"dyu", "ぢゅ"}, {"dyo", "ぢょ"},
                {"ba", "ば"}, {"bi", "び"}, {"bu", "ぶ"}, {"be", "べ"}, {"bo", "ぼ"},
                {"bya", "びゃ"}, {"byu", "びゅ"}, {"byo", "びょ"},
                {"pa", "ぱ"}, {"pi", "ぴ"}, {"pu", "ぷ"}, {"pe", "ぺ"}, {"po", "ぽ"},
                {"pya", "ぴゃ"}, {"pyu", "ぴゅ"}, {"pyo", "ぴょ"},
                {"va", "ゔぁ"}, {"vi", "ゔぃ"}, {"vu", "ゔ"}, {"ve", "ゔぇ"}, {"vo", "ゔぉ"},
                {"fa", "ふぁ"}, {"fi", "ふぃ"}, {"fe", "ふぇ"}, {"fo", "ふぉ"},
                {"xa", "ぁ"}, {"xi", "ぃ"}, {"xu", "ぅ"}, {"xe", "ぇ"}, {"xo", "ぉ"},
                {"xtu", "っ"}, {"ltu", "っ"}, {"xtsu", "っ"},
                {"xya", "ゃ"}, {"xyu", "ゅ"}, {"xyo", "ょ"}, {"xwa", "ゎ"},
                {"n'", "ん"}, {"-", "ー"},
        };
        for (int i = 0; i < rows.length; i++) {
            TBL.put(rows[i][0], rows[i][1]);
        }
    }

    private static boolean isVowel(char c) {
        return c == 'a' || c == 'i' || c == 'u' || c == 'e' || c == 'o';
    }

    private static boolean isPrefixOfKey(String s) {
        if (s.isEmpty()) return true;
        for (String k : TBL.keySet()) {
            if (k.startsWith(s)) return true;
        }
        return false;
    }

    /** 把罗马字前缀尽可能转换进 out，返回还不能确定、需要留待下一键的尾部。 */
    public static String feed(String romaji, StringBuilder out) {
        int i = 0;
        int n = romaji.length();
        while (i < n) {
            char c = romaji.charAt(i);
            // n 的特殊处理：n + 辅音 → ん；n + n + 元音 → ん + な行
            if (c == 'n' && i + 1 < n) {
                char nx = romaji.charAt(i + 1);
                if (nx == 'n') {
                    boolean nextIsVowel = (i + 2 < n)
                            && (isVowel(romaji.charAt(i + 2)) || romaji.charAt(i + 2) == 'y');
                    if (nextIsVowel) {
                        out.append('ん');
                        i += 1;
                    } else {
                        out.append('ん');
                        i += 2;
                    }
                    continue;
                }
                if (!isVowel(nx) && nx != 'y' && nx != '\'') {
                    out.append('ん');
                    i += 1;
                    continue;
                }
            }
            // 促音：相同辅音连写
            if (i + 1 < n && c == romaji.charAt(i + 1) && !isVowel(c) && c != 'n' && Character.isLetter(c)) {
                out.append('っ');
                i += 1;
                continue;
            }
            boolean matched = false;
            for (int len = Math.min(4, n - i); len >= 2; len--) {
                String sub = romaji.substring(i, i + len);
                String kana = TBL.get(sub);
                if (kana != null) {
                    out.append(kana);
                    i += len;
                    matched = true;
                    break;
                }
            }
            if (matched) continue;
            String single = TBL.get(String.valueOf(c));
            if (single != null) {
                out.append(single);
                i += 1;
                continue;
            }
            if (isPrefixOfKey(romaji.substring(i))) {
                break;
            }
            // 不认识的字符原样带出（例如标点、非拉丁字母）
            out.append(c);
            i += 1;
        }
        return romaji.substring(i);
    }

    /** 整串罗马字直接转假名（用于无效输入兜底）。 */
    public static String toHiragana(String romaji) {
        StringBuilder sb = new StringBuilder();
        String rest = feed(romaji, sb);
        sb.append(tail(rest));
        return sb.toString();
    }

    /** 结尾残留的罗马字：单独的 n 收成 ん，其它原样保留。 */
    public static String tail(String rest) {
        if (rest == null || rest.isEmpty()) return "";
        if (rest.equals("n")) return "ん";
        return rest;
    }

    public static boolean isHiragana(char c) {
        return (c >= '\u3041' && c <= '\u3096') || c == '\u30FC';
    }

    public static String toKatakana(String hira) {
        StringBuilder sb = new StringBuilder(hira.length());
        for (int i = 0; i < hira.length(); i++) {
            char c = hira.charAt(i);
            if (c >= '\u3041' && c <= '\u3096') {
                sb.append((char) (c + 0x60));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String toHiraganaFromKata(String kata) {
        StringBuilder sb = new StringBuilder(kata.length());
        for (int i = 0; i < kata.length(); i++) {
            char c = kata.charAt(i);
            if (c >= '\u30A1' && c <= '\u30F6') {
                sb.append((char) (c - 0x60));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String convertScript(String text, boolean katakana) {
        return katakana ? toKatakana(toHiraganaFromKata(text)) : toHiraganaFromKata(text);
    }

    /** 假名 -> 罗马音表（平假名，含拗音、促音、长音）。 */
    private static final Map<String, String> REV = new HashMap<String, String>();

    static {
        String[][] r = {
                {"あ", "a"}, {"い", "i"}, {"う", "u"}, {"え", "e"}, {"お", "o"},
                {"か", "ka"}, {"き", "ki"}, {"く", "ku"}, {"け", "ke"}, {"こ", "ko"},
                {"きゃ", "kya"}, {"きゅ", "kyu"}, {"きょ", "kyo"},
                {"さ", "sa"}, {"し", "shi"}, {"す", "su"}, {"せ", "se"}, {"そ", "so"},
                {"しゃ", "sha"}, {"しゅ", "shu"}, {"しょ", "sho"},
                {"た", "ta"}, {"ち", "chi"}, {"つ", "tsu"}, {"て", "te"}, {"と", "to"},
                {"ちゃ", "cha"}, {"ちゅ", "chu"}, {"ちょ", "cho"},
                {"な", "na"}, {"に", "ni"}, {"ぬ", "nu"}, {"ね", "ne"}, {"の", "no"},
                {"にゃ", "nya"}, {"にゅ", "nyu"}, {"にょ", "nyo"},
                {"は", "ha"}, {"ひ", "hi"}, {"ふ", "fu"}, {"へ", "he"}, {"ほ", "ho"},
                {"ひゃ", "hya"}, {"ひゅ", "hyu"}, {"ひょ", "hyo"},
                {"ま", "ma"}, {"み", "mi"}, {"む", "mu"}, {"め", "me"}, {"も", "mo"},
                {"みゃ", "mya"}, {"みゅ", "myu"}, {"みょ", "myo"},
                {"や", "ya"}, {"ゆ", "yu"}, {"よ", "yo"},
                {"ら", "ra"}, {"り", "ri"}, {"る", "ru"}, {"れ", "re"}, {"ろ", "ro"},
                {"りゃ", "rya"}, {"りゅ", "ryu"}, {"りょ", "ryo"},
                {"わ", "wa"}, {"を", "wo"}, {"ん", "n"}, {"ゐ", "i"}, {"ゑ", "e"},
                {"が", "ga"}, {"ぎ", "gi"}, {"ぐ", "gu"}, {"げ", "ge"}, {"ご", "go"},
                {"ぎゃ", "gya"}, {"ぎゅ", "gyu"}, {"ぎょ", "gyo"},
                {"ざ", "za"}, {"じ", "ji"}, {"ず", "zu"}, {"ぜ", "ze"}, {"ぞ", "zo"},
                {"じゃ", "ja"}, {"じゅ", "ju"}, {"じょ", "jo"},
                {"だ", "da"}, {"ぢ", "ji"}, {"づ", "zu"}, {"で", "de"}, {"ど", "do"},
                {"ば", "ba"}, {"び", "bi"}, {"ぶ", "bu"}, {"べ", "be"}, {"ぼ", "bo"},
                {"びゃ", "bya"}, {"びゅ", "byu"}, {"びょ", "byo"},
                {"ぱ", "pa"}, {"ぴ", "pi"}, {"ぷ", "pu"}, {"ぺ", "pe"}, {"ぽ", "po"},
                {"ぴゃ", "pya"}, {"ぴゅ", "pyu"}, {"ぴょ", "pyo"},
                {"ぁ", "a"}, {"ぃ", "i"}, {"ぅ", "u"}, {"ぇ", "e"}, {"ぉ", "o"},
                {"ゃ", "ya"}, {"ゅ", "yu"}, {"ょ", "yo"},
                {"ゔ", "vu"}, {"ー", "-"}, {"\u3099", ""}, {"\u309A", ""},
        };
        for (int i = 0; i < r.length; i++) {
            REV.put(r[i][0], r[i][1]);
        }
    }

    /**
     * 假名串转罗马音，用于候选栏上方的小字注释。
     * 促音双写后一个辅音，长音符输出「-」，非假名字符原样保留。
     */
    public static String toRomaji(String kana) {
        if (kana == null || kana.isEmpty()) return "";
        String hira = toHiraganaFromKata(kana);
        StringBuilder sb = new StringBuilder();
        boolean sokuon = false;
        int i = 0;
        while (i < hira.length()) {
            String two = i + 1 < hira.length() ? hira.substring(i, i + 2) : null;
            String romaji = two == null ? null : REV.get(two);
            if (romaji != null) {
                if (sokuon) {
                    sb.append(romaji.charAt(0));
                    sokuon = false;
                }
                sb.append(romaji);
                i += 2;
                continue;
            }
            char c = hira.charAt(i);
            if (c == '\u3063') { // っ
                sokuon = true;
                i++;
                continue;
            }
            String one = REV.get(String.valueOf(c));
            if (one != null) {
                if (sokuon) {
                    sb.append(one.charAt(0));
                    sokuon = false;
                }
                sb.append(one);
            } else {
                sokuon = false;
                sb.append(c);
            }
            i++;
        }
        return sb.toString();
    }

    public static String normalizeKey(String text) {
        if (text == null) return "";
        return toHiraganaFromKata(text);
    }

    private final BinDict dict;

    /** 启用的词库分类位（-1 表示不限）。 */
    private int catMask = -1;

    public JapaneseEngine(BinDict dict) {
        this.dict = dict;
    }

    public void setCatMask(int mask) {
        catMask = mask;
    }

    public int keyCount() {
        return dict == null ? 0 : dict.keyCount(0);
    }

    /**
     * @param key      已转换完成的假名串（平假名形式）
     * @param katakana 是否以片假名输出假名候选
     */
    public List<Candidate> query(String key, boolean katakana, int limit) {
        List<Candidate> out = new ArrayList<Candidate>();
        if (key == null || key.isEmpty()) return out;
        String norm = normalizeKey(key);
        String kanaWord = convertScript(norm, katakana);
        String otherWord = convertScript(norm, !katakana);

        List<Candidate> hits = new ArrayList<Candidate>();
        if (dict != null) {
            dict.exact(0, norm, limit * 3, 1f, catMask, hits);
        }

        boolean kanaFirst = norm.codePointCount(0, norm.length()) <= 1;
        if (kanaFirst) {
            out.add(new Candidate(kanaWord, 62000, norm, 62000f));
            if (!otherWord.equals(kanaWord)) {
                out.add(new Candidate(otherWord, 59000, norm, 59000f));
            }
        }
        HashSet<String> seen = new HashSet<String>();
        seen.add(kanaWord);
        seen.add(otherWord);
        for (int i = 0; i < hits.size() && out.size() < limit; i++) {
            Candidate c = hits.get(i);
            if (!seen.add(c.word)) continue;
            c.tones = norm;   // 读音（假名），候选栏小字注罗马音时用
            out.add(c);
        }
        if (!kanaFirst && out.size() < limit) {
            out.add(new Candidate(kanaWord, 30000, norm, 30000f));
            if (!otherWord.equals(kanaWord) && out.size() < limit) {
                out.add(new Candidate(otherWord, 29000, norm, 29000f));
            }
        }
        if (dict != null && out.size() < limit && norm.length() >= 2) {
            List<Candidate> pre = new ArrayList<Candidate>();
            dict.prefix(0, norm, 60, limit * 3, 0.5f, catMask, pre);
            for (int i = 0; i < pre.size() && out.size() < limit; i++) {
                Candidate c = pre.get(i);
                if (!seen.add(c.word)) continue;
                c.tones = norm;
                out.add(c);
            }
        }
        if (out.size() > limit) out = new ArrayList<Candidate>(out.subList(0, limit));
        return out;
    }
}

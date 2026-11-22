package com.shuangyin.ime;

import java.util.ArrayList;
import java.util.List;

/** 键盘布局定义。 */
final class Keyboard {

    static final int A_CHAR = 0;
    static final int A_BACKSPACE = 1;
    static final int A_ENTER = 2;
    static final int A_SPACE = 3;
    static final int A_LANG = 4;
    static final int A_SYMBOL = 5;
    static final int A_ALPHA = 6;
    static final int A_SCHEME = 7;
    static final int A_SETTINGS = 8;
    static final int A_SHIFT = 9;
    static final int A_KANA = 10;
    static final int A_SCRIPT = 11;
    static final int A_COPY = 12;
    static final int A_PASTE = 13;
    static final int A_CLIP = 14;
    static final int A_JP_MODE = 15;
    /** 进入笔画输入（只在符号页出现）。 */
    static final int A_STROKE = 16;
    /** 笔画 ⇄ 拼音 切换（笔画键盘中格）。 */
    static final int A_STROKE_PINYIN = 17;
    /** 收起输入法（候选栏最右边的叉）。 */
    static final int A_CLOSE_IME = 18;
    /** 笔画键盘的分词键（插入字间分隔，视觉上标出分词位置）。 */
    static final int A_SPLIT = 19;
    /** 删除键上横向一划：清空当前正在输入的整串内容。 */
    static final int A_CLEAR_ALL = 20;
    /** 日语假名键：给上一个假名加浊点。 */
    static final int A_DAKU = 21;
    /** 日语假名键：给上一个假名加半浊点。 */
    static final int A_HANDAKU = 22;
    /** 全选输入框里的文字。 */
    static final int A_SELECT_ALL = 23;
    /** 短按 123 进数字键盘。 */
    static final int A_NUMBER = 24;
    /** 日语假名键盘的「小假名」开关。 */
    static final int A_SMALL_KANA = 25;
    /** 数字键盘的「＝」：把输入框里刚打的算式算出来并替换成结果。 */
    static final int A_CALC = 26;
    /** 数字键盘底行的「返回」：退出数字页，回到进入前那个输入模式。 */
    static final int A_BACK_MODE = 27;
    /** 符号页翻页（左右横向滑动触发，dir=+1 下一屏 / -1 上一屏）。 */
    static final int A_SYMBOL_FLIP = 28;
    /** 候选栏动作键：全选删除（清空整串输入）。 */
    static final int A_SELECT_ALL_DELETE = 29;
    /** 候选栏动作键：声调筛选（1–4），extra 里的调值由 ImeView 直接带上。 */
    static final int A_TONE = 30;
    /** 什么都不做的占位键（数字页左列留给可滑动的运算符条）。 */
    static final int A_NONE = -1;

    /** 注音分区键盘的区域标记：0 普通键，1 声母，2 韵母，3 整体认读，4 声调。 */
    static final int ZONE_NONE = 0;
    static final int ZONE_INITIAL = 1;
    static final int ZONE_FINAL = 2;
    static final int ZONE_WHOLE = 3;
    static final int ZONE_TONE = 4;

    static final class Key {
        final String label;
        final String insert;
        final int action;
        final float weight;
        /** 右下角小字提示（红色），用于注音键标注对应拼音字母。 */
        final String hint;
        /** 长按插入的文字，没有长按行为时为 null。 */
        final String longInsert;
        /** 长按触发的动作（0 表示没有），例如英文键盘 123 键长按切大小写。 */
        final int longAction;
        /** 注音分区键盘里的归属区域，0 表示不分区的普通键。 */
        int zone;
        /** 置灰且不可按（小假名模式下没有小写形式的假名键）。 */
        boolean disabled;

        Key(String label, String insert, int action, float weight, String hint) {
            this(label, insert, action, weight, hint, null, 0);
        }

        Key(String label, String insert, int action, float weight, String hint, String longInsert) {
            this(label, insert, action, weight, hint, longInsert, 0);
        }

        Key(String label, String insert, int action, float weight, String hint, String longInsert,
                int longAction) {
            this.label = label;
            this.insert = insert;
            this.action = action;
            this.weight = weight;
            this.hint = hint;
            this.longInsert = longInsert;
            this.longAction = longAction;
        }

        static Key ch(String label) {
            return new Key(label, label, A_CHAR, 1f, null);
        }

        static Key ch(String label, String insert) {
            return new Key(label, insert, A_CHAR, 1f, null);
        }

        static Key ch(String label, String insert, String hint) {
            return new Key(label, insert, A_CHAR, 1f, hint);
        }

        static Key ch(String label, String insert, String hint, float weight) {
            return new Key(label, insert, A_CHAR, weight, hint);
        }

        /** 数字键：轻点出数字，长按出常用符号（长按判定很短，见 ImeView.LONG_PRESS_MS）。 */
        static Key digit(String label, String longInsert) {
            return new Key(label, label, A_CHAR, 1f, null, longInsert);
        }

        /** 逗号句号共用一个键：轻点逗号，长按句号。 */
        static Key punct(float weight) {
            return new Key("，。", "，", A_CHAR, weight, null, "。");
        }

        static Key act(String label, int action) {
            return new Key(label, null, action, 1f, null);
        }

        static Key act(String label, int action, float weight) {
            return new Key(label, null, action, weight, null);
        }
    }

    final String name;
    final List<List<Key>> rows = new ArrayList<List<Key>>();
    /** 英文键盘：底行 123 的长按动作改为切大小写（其余键盘是进符号页）。 */
    boolean english;
    /** 数字键盘左列那串可上下滑动的运算符（＋ － × ÷ ％ ＝），null 表示没有。 */
    List<Key> opStrip;

    Keyboard(String name) {
        this.name = name;
    }

    void row(Key... keys) {
        ArrayList<Key> r = new ArrayList<Key>(keys.length);
        for (int i = 0; i < keys.length; i++) {
            r.add(keys[i]);
        }
        rows.add(r);
    }

    int rowCount() {
        return rows.size();
    }

    private static void letters(Keyboard kb, String letters) {
        for (int i = 0; i < letters.length(); i++) {
            kb.rows.get(kb.rows.size() - 1).add(
                    Key.ch(String.valueOf(letters.charAt(i)), String.valueOf(letters.charAt(i))));
        }
    }

    /**
     * 底行（所有键盘共用）：左「拼注日」+「123」，中间空格，右「中/英」+「Enter」，
     * 左右两侧权重相等，空格正好落在整行中点。
     * 日语三态（罗马字 / 平假名 / 片假名）不再单独占一个键，改成长按「拼注日」循环。
     */
    private static void bottomRow(Keyboard kb) {
        kb.row(
                // 长按「拼注日」= 日语三态循环（罗马字 / 平假名 / 片假名），右上角小字显示当前态
                new Key("拼注日", null, A_SCHEME, 1.25f, null, null, A_JP_MODE),
                // 长按 123 进符号页（右上角标「符」）：中英文都一样，大小写改由 ⇧ 键负责
                new Key("123", null, A_NUMBER, 0.95f, null, null, A_SYMBOL),
                Key.act("空格", A_SPACE, 2.4f),
                Key.act("中/英", A_LANG, 1.15f),
                Key.act("Enter", A_ENTER, 1.05f));
    }

    /** 假名键盘用同一套底行（逗号句号已经搬进假名格子里）。 */
    private static void bottomRowKana(Keyboard kb) {
        bottomRow(kb);
    }

    /**
     * 空位补键（拼音 / 英文 / 日语）：
     *   拼音 / 英文：左下 ←「，。」，原来的「，。」（第三行最左）换成 ⇧ 上档键；
     *   日语：开了假名键盘时左下给一个「日語」键（点一下循环 羅 → あ → ア，方便切罗马字 / 假名），
     *         没开假名键盘就还是「、。」标点键。
     *
     * @param scheme 0 拼音 / 英文，2 日语
     */
    void fillHiddenSchemeSlot(int scheme, boolean kanaEnabled) {
        if (rows.isEmpty()) return;
        int last = rows.size() - 1;
        List<Key> bottom = rows.get(last);
        if (bottom.isEmpty()) return;
        float slotW = bottom.get(0).weight;
        if (scheme == 2) {
            if (kanaEnabled) {
                bottom.set(0, new Key("日語", null, A_JP_MODE, slotW, null));
            } else {
                bottom.set(0, new Key("、。", "、", A_CHAR, slotW, null, "。"));
            }
            return;
        }
        bottom.set(0, Key.punct(slotW));
        for (int r = 0; r < last; r++) {
            List<Key> row = rows.get(r);
            for (int i = 0; i < row.size(); i++) {
                Key k = row.get(i);
                if (k.action == A_CHAR && "，。".equals(k.label)) {
                    row.set(i, new Key("", null, A_SHIFT, k.weight, null));
                    return;
                }
            }
        }
    }

    /** 底部那个宽的「一声」键：轻点一声、长按输空格；底色跟其它声调走同一套分区色。 */
    private static Key tone1Key(float weight) {
        Key k = new Key("一声", "1", A_CHAR, weight, null, null, A_SPACE);
        k.zone = ZONE_TONE;
        return k;
    }

    /**
     * 注音上半部分重排（单模式 / 多模式共用）：
     *   1) 第二行末尾重复的「ˉ 1声」去掉；
     *   2) 退格从第三行挪到第四行末尾（原「，。」位），第三行那个「，。」不再保留，
     *      单模式的标点在底部栏，多模式的标点挂在二声 / 轻声上长按出。
     */
    void zhuyinCompact() {
        int last = rows.size() - 1;
        int bkRow = -1, bkCol = -1, ptRow = -1, ptCol = -1;
        for (int r = 0; r < last; r++) {
            List<Key> row = rows.get(r);
            for (int i = 0; i < row.size(); i++) {
                Key k = row.get(i);
                if (k.action == A_BACKSPACE) {
                    bkRow = r;
                    bkCol = i;
                }
                if ("，。".equals(k.label)) {
                    ptRow = r;
                    ptCol = i;
                }
            }
        }
        if (ptRow >= 0) {
            rows.get(ptRow).set(ptCol, Key.act("⌫", A_BACKSPACE, 1f));
        }
        if (bkRow >= 0 && bkRow != ptRow) {
            rows.get(bkRow).remove(bkCol);
        }
        for (int r = 0; r < last; r++) {
            List<Key> row = rows.get(r);
            for (int i = row.size() - 1; i >= 0; i--) {
                if ("ˉ".equals(row.get(i).label)) {
                    row.remove(i);
                }
            }
        }
    }

    /** 单模式注音的底行：123 / ，。 / 一声（长按空格）/ 中·英 / Enter，整张键盘没有独立空格键。 */
    void zhuyinSingle() {
        zhuyinCompact();
        int last = rows.size() - 1;
        List<Key> nb = new ArrayList<Key>();
        nb.add(new Key("123", null, A_NUMBER, 0.95f, null, null, A_SYMBOL));
        nb.add(Key.punct(1.25f));
        nb.add(tone1Key(2.4f));
        nb.add(Key.act("中/英", A_LANG, 1.15f));
        nb.add(Key.act("Enter", A_ENTER, 1.05f));
        rows.set(last, nb);
    }

    /**
     * 多模式注音的底行保持原样，中间那个空格键换成和单模式一样的「一声」；
     * 键盘上没有单独的标点键，逗号挂到二声「ˊ」、句号挂到轻声「˙」，长按输出（键面右上角标出来）。
     */
    void zhuyinMulti() {
        zhuyinCompact();
        int last = rows.size() - 1;
        List<Key> bottom = rows.get(last);
        for (int i = 0; i < bottom.size(); i++) {
            if (bottom.get(i).action == A_SPACE) {
                bottom.set(i, tone1Key(bottom.get(i).weight));
            }
        }
        for (int r = 0; r < last; r++) {
            List<Key> row = rows.get(r);
            for (int i = 0; i < row.size(); i++) {
                Key k = row.get(i);
                Key nk = null;
                if ("ˊ".equals(k.label)) {
                    nk = new Key(k.label, "2", A_CHAR, k.weight, k.hint, "，");
                } else if ("˙".equals(k.label)) {
                    nk = new Key(k.label, "˙", A_CHAR, k.weight, k.hint, "。");
                }
                if (nk != null) {
                    nk.zone = k.zone;          // 声调配色跟着原键
                    row.set(i, nk);
                }
            }
        }
    }

    static Keyboard pinyin(boolean english) {
        return pinyin(english, false);
    }

    /**
     * 全拼 / 英文键盘。英文时 123 键带长按动作（切大小写），upper 为真则字母键直接显示大写。
     */
    static Keyboard pinyin(boolean english, boolean upper) {
        Keyboard kb = new Keyboard(english ? "英文" : "全拼");
        kb.english = english;
        String row1 = "qwertyuiop";
        String row2 = "asdfghjkl";
        String row3 = "zxcvbnm";
        if (english && upper) {
            row1 = row1.toUpperCase();
            row2 = row2.toUpperCase();
            row3 = row3.toUpperCase();
        }
        kb.row();
        letters(kb, row1);
        kb.row();
        letters(kb, row2);
        kb.row();
        // 原来 123 的位置换成逗号句号键；123 已移到「拼注日」右边
        kb.rows.get(kb.rows.size() - 1).add(Key.punct(1.35f));
        letters(kb, row3);
        kb.rows.get(kb.rows.size() - 1).add(Key.act("⌫", A_BACKSPACE, 1.35f));
        bottomRow(kb);
        return kb;
    }

    /** 注音键盘：每个注音符号右下角用红色标出对应拼音字母。 */
    static Keyboard zhuyin() {
        return zhuyinLayout(0);
    }

    /** 注音布局选择：0 当前方案（标准大千排列），1 大千式，2 倚天式，3 精业式，4 IBM 式。 */
    static Keyboard zhuyinLayout(int id) {
        if (id == 1) return zhuyinStandard("大千式");
        if (id == 2) return zhuyinClassic("倚天式", 2);
        if (id == 3) return zhuyinClassic("精业式", 3);
        if (id == 4) return zhuyinClassic("IBM式", 4);
        return zhuyinDefault();
    }

    /** 当前注音布局：按标准大千式排列，四行各 10~11 键，声母韵母声调混排在常用位置上。 */
    private static Keyboard zhuyinDefault() {
        return zhuyinStandard("注音");
    }

    /**
     * 标准大千式排列（参考图）：
     * 第一行 ㄅ ㄉ ˇ ˋ ㄓ ˊ ˙ ㄚ ㄞ ㄢ ㄦ
     * 第二行 ㄆ ㄊ ㄍ ㄐ ㄔ ㄗ ㄧ ㄛ ㄟ ㄣ ˉ
     * 第三行 ㄇ ㄋ ㄎ ㄑ ㄕ ㄘ ㄨ ㄜ ㄠ ㄤ ⌫
     * 第四行 ㄈ ㄌ ㄏ ㄒ ㄖ ㄙ ㄩ ㄝ ㄡ ㄥ ，。
     */
    private static Keyboard zhuyinStandard(String name) {
        Keyboard kb = new Keyboard(name);
        kb.row(
                Key.ch("ㄅ", "ㄅ", "b"), Key.ch("ㄉ", "ㄉ", "d"),
                Key.ch("ˇ", "3", "3声"), Key.ch("ˋ", "4", "4声"),
                Key.ch("ㄓ", "ㄓ", "zh"), Key.ch("ˊ", "2", "2声"), Key.ch("˙", "5", "轻声"),
                Key.ch("ㄚ", "ㄚ", "a"), Key.ch("ㄞ", "ㄞ", "ai"), Key.ch("ㄢ", "ㄢ", "an"),
                Key.ch("ㄦ", "ㄦ", "er"));
        kb.row(
                Key.ch("ㄆ", "ㄆ", "p"), Key.ch("ㄊ", "ㄊ", "t"),
                Key.ch("ㄍ", "ㄍ", "g"), Key.ch("ㄐ", "ㄐ", "j"),
                Key.ch("ㄔ", "ㄔ", "ch"), Key.ch("ㄗ", "ㄗ", "z"),
                Key.ch("ㄧ", "ㄧ", "i"), Key.ch("ㄛ", "ㄛ", "o"),
                Key.ch("ㄟ", "ㄟ", "ei"), Key.ch("ㄣ", "ㄣ", "en"),
                Key.ch("ˉ", "1", "1声"));
        kb.row(
                Key.ch("ㄇ", "ㄇ", "m"), Key.ch("ㄋ", "ㄋ", "n"),
                Key.ch("ㄎ", "ㄎ", "k"), Key.ch("ㄑ", "ㄑ", "q"),
                Key.ch("ㄕ", "ㄕ", "sh"), Key.ch("ㄘ", "ㄘ", "c"),
                Key.ch("ㄨ", "ㄨ", "u"), Key.ch("ㄜ", "ㄜ", "e"),
                Key.ch("ㄠ", "ㄠ", "ao"), Key.ch("ㄤ", "ㄤ", "ang"),
                Key.act("⌫", A_BACKSPACE, 1f));
        kb.row(
                Key.ch("ㄈ", "ㄈ", "f"), Key.ch("ㄌ", "ㄌ", "l"),
                Key.ch("ㄏ", "ㄏ", "h"), Key.ch("ㄒ", "ㄒ", "x"),
                Key.ch("ㄖ", "ㄖ", "r"), Key.ch("ㄙ", "ㄙ", "s"),
                Key.ch("ㄩ", "ㄩ", "ü"), Key.ch("ㄝ", "ㄝ", "ê"),
                Key.ch("ㄡ", "ㄡ", "ou"), Key.ch("ㄥ", "ㄥ", "eng"),
                Key.punct(1f));
        // 给每个注音键标上所属区域：声母 / 韵母 / 声调（分区底色与分区边框都按这个走）
        for (int r = 0; r < kb.rows.size(); r++) {
            List<Key> row = kb.rows.get(r);
            for (int i = 0; i < row.size(); i++) {
                Key k = row.get(i);
                if (k.action != A_CHAR) continue;
                k.zone = zyZoneOf(k.label);
            }
        }
        bottomRow(kb);
        return kb;
    }

    /** 注音符号属于哪个区域：声母 / 韵母 / 声调。 */
    static int zyZoneOf(String label) {
        if (label == null || label.isEmpty()) return ZONE_NONE;
        char c = label.charAt(0);
        if (c == 'ˉ' || c == 'ˊ' || c == 'ˇ' || c == 'ˋ' || c == '˙') return ZONE_TONE;
        if ("ㄅㄆㄇㄈㄉㄊㄋㄌㄍㄎㄏㄐㄑㄒㄓㄔㄕㄖㄗㄘㄙ".indexOf(c) >= 0) return ZONE_INITIAL;
        if ("ㄧㄨㄩㄚㄛㄜㄝㄞㄟㄠㄡㄢㄣㄤㄥㄦ".indexOf(c) >= 0) return ZONE_FINAL;
        return ZONE_NONE;
    }

    /**
     * 四套传统注音布局（大千 / 倚天 / 精业 / IBM）：都是「声母两行 + 韵母 + 声调」，
     * 差别在于每行装多少个符号、ㄧㄨㄩ 与 ㄢㄣㄤㄥㄦ 落在第几行。
     */
    private static Keyboard zhuyinClassic(String name, int id) {
        Keyboard kb = new Keyboard(name);
        String[] rows;
        if (id == 1) {
            // 大千式：11 / 10 / 11 / 声调
            rows = new String[]{
                    "ㄅㄆㄇㄈㄉㄊㄋㄌㄍㄎㄏ",
                    "ㄐㄑㄒㄓㄔㄕㄖㄗㄘㄙ",
                    "ㄧㄨㄩㄚㄛㄜㄝㄞㄟㄠㄡ",
                    "ㄢㄣㄤㄥㄦ"};
        } else if (id == 2) {
            // 倚天式：12 / 12 / 12 / 声调
            rows = new String[]{
                    "ㄅㄆㄇㄈㄉㄊㄋㄌㄍㄎㄏㄐ",
                    "ㄑㄒㄓㄔㄕㄖㄗㄘㄙㄧㄨㄩ",
                    "ㄚㄛㄜㄝㄞㄟㄠㄡㄢㄣㄤㄥ",
                    "ㄦ"};
        } else if (id == 3) {
            // 精业式：10 / 11 / 11 / 声调
            rows = new String[]{
                    "ㄅㄆㄇㄈㄉㄊㄋㄌㄍㄎ",
                    "ㄏㄐㄑㄒㄓㄔㄕㄖㄗㄘㄙ",
                    "ㄧㄨㄩㄚㄛㄜㄝㄞㄟㄠㄡ",
                    "ㄢㄣㄤㄥㄦ"};
        } else {
            // IBM 式：10 / 10 / 11 / 声调
            rows = new String[]{
                    "ㄅㄆㄇㄈㄉㄊㄋㄌㄍㄎ",
                    "ㄏㄐㄑㄒㄓㄔㄕㄖㄗㄘ",
                    "ㄙㄧㄨㄩㄚㄛㄜㄝㄞㄟㄠ",
                    "ㄡㄢㄣㄤㄥㄦ"};
        }
        for (int r = 0; r < rows.length; r++) {
            List<Key> row = new ArrayList<Key>();
            for (int i = 0; i < rows[r].length(); i++) {
                String s = String.valueOf(rows[r].charAt(i));
                row.add(Key.ch(s, s, zyHint(s)));
            }
            if (r == rows.length - 1) {
                row.add(Key.ch("ˉ", "1", "1声"));
                row.add(Key.ch("ˊ", "2", "2声"));
                row.add(Key.ch("ˇ", "3", "3声"));
                row.add(Key.ch("ˋ", "4", "4声"));
                row.add(Key.ch("˙", "5", "轻声"));
                row.add(0, Key.punct(1.15f));
                row.add(Key.act("⌫", A_BACKSPACE, 1.15f));
            }
            kb.rows.add(row);
        }
        bottomRow(kb);
        return kb;
    }

    /** 注音符号对应的拼音字母，画在键右下角。 */
    private static String zyHint(String s) {
        char c = s.charAt(0);
        switch (c) {
            case 'ㄅ': return "b";
            case 'ㄆ': return "p";
            case 'ㄇ': return "m";
            case 'ㄈ': return "f";
            case 'ㄉ': return "d";
            case 'ㄊ': return "t";
            case 'ㄋ': return "n";
            case 'ㄌ': return "l";
            case 'ㄍ': return "g";
            case 'ㄎ': return "k";
            case 'ㄏ': return "h";
            case 'ㄐ': return "j";
            case 'ㄑ': return "q";
            case 'ㄒ': return "x";
            case 'ㄓ': return "zh";
            case 'ㄔ': return "ch";
            case 'ㄕ': return "sh";
            case 'ㄖ': return "r";
            case 'ㄗ': return "z";
            case 'ㄘ': return "c";
            case 'ㄙ': return "s";
            case 'ㄧ': return "i";
            case 'ㄨ': return "u";
            case 'ㄩ': return "ü";
            case 'ㄚ': return "a";
            case 'ㄛ': return "o";
            case 'ㄜ': return "e";
            case 'ㄝ': return "ê";
            case 'ㄞ': return "ai";
            case 'ㄟ': return "ei";
            case 'ㄠ': return "ao";
            case 'ㄡ': return "ou";
            case 'ㄢ': return "an";
            case 'ㄣ': return "en";
            case 'ㄤ': return "ang";
            case 'ㄥ': return "eng";
            case 'ㄦ': return "er";
            default: return null;
        }
    }

    /**
     * 注音分区键盘：按「声母 / 韵母 / 整体认读 / 声调」四块摆键并着色，
     * 声母两行、韵母两行、整体认读与声调各占一块，找键比传统键盘快。
     */
    static Keyboard zhuyinGrouped() {
        return zhuyinGrouped(true);
    }

    /**
     * @param wholeRow 是否保留「整体认读」这一整行；关掉时把 123 /⌫收进韵母声调行，
     *                 键盘少一行，高度矮一格。
     */
    static Keyboard zhuyinGrouped(boolean wholeRow) {
        Keyboard kb = new Keyboard("注音");
        // 声母：21 个
        kb.row(
                Key.ch("ㄅ", "ㄅ", "b"), Key.ch("ㄆ", "ㄆ", "p"), Key.ch("ㄇ", "ㄇ", "m"),
                Key.ch("ㄈ", "ㄈ", "f"), Key.ch("ㄉ", "ㄉ", "d"), Key.ch("ㄊ", "ㄊ", "t"),
                Key.ch("ㄋ", "ㄋ", "n"), Key.ch("ㄌ", "ㄌ", "l"), Key.ch("ㄍ", "ㄍ", "g"),
                Key.ch("ㄎ", "ㄎ", "k"), Key.ch("ㄏ", "ㄏ", "h"));
        kb.row(
                Key.ch("ㄐ", "ㄐ", "j"), Key.ch("ㄑ", "ㄑ", "q"), Key.ch("ㄒ", "ㄒ", "x"),
                Key.ch("ㄓ", "ㄓ", "zh"), Key.ch("ㄔ", "ㄔ", "ch"), Key.ch("ㄕ", "ㄕ", "sh"),
                Key.ch("ㄖ", "ㄖ", "r"), Key.ch("ㄗ", "ㄗ", "z"), Key.ch("ㄘ", "ㄘ", "c"),
                Key.ch("ㄙ", "ㄙ", "s"));
        // 韵母：16 个（介音 ㄧㄨㄩ 也算韵母）
        kb.row(
                Key.ch("ㄧ", "ㄧ", "i"), Key.ch("ㄨ", "ㄨ", "u"), Key.ch("ㄩ", "ㄩ", "ü"),
                Key.ch("ㄚ", "ㄚ", "a"), Key.ch("ㄛ", "ㄛ", "o"), Key.ch("ㄜ", "ㄜ", "e"),
                Key.ch("ㄝ", "ㄝ", "ê"), Key.ch("ㄞ", "ㄞ", "ai"), Key.ch("ㄟ", "ㄟ", "ei"),
                Key.ch("ㄠ", "ㄠ", "ao"));
        kb.row(
                Key.ch("ㄡ", "ㄡ", "ou"), Key.ch("ㄢ", "ㄢ", "an"), Key.ch("ㄣ", "ㄣ", "en"),
                Key.ch("ㄤ", "ㄤ", "ang"), Key.ch("ㄥ", "ㄥ", "eng"), Key.ch("ㄦ", "ㄦ", "er"),
                Key.ch("ˉ", "1", "1声"), Key.ch("ˊ", "2", "2声"), Key.ch("ˇ", "3", "3声"),
                Key.ch("ˋ", "4", "4声"), Key.ch("˙", "5", "轻声"));
        // 整体认读音节（ㄓㄔㄕㄖㄗㄘㄙ 可单独成音节）+ 123 + 退格
        if (wholeRow) {
            kb.row(
                    Key.ch("ㄓ", "ㄓ", "zhi"), Key.ch("ㄔ", "ㄔ", "chi"), Key.ch("ㄕ", "ㄕ", "shi"),
                    Key.ch("ㄖ", "ㄖ", "ri"), Key.ch("ㄗ", "ㄗ", "zi"), Key.ch("ㄘ", "ㄘ", "ci"),
                    Key.ch("ㄙ", "ㄙ", "si"),
                    Key.act("⌫", A_BACKSPACE, 1.15f));
        } else {
            // 不要整体认读行：把退格塞进韵母 / 声调那一行的右端
            List<Key> r3 = kb.rows.get(3);
            r3.add(Key.act("⌫", A_BACKSPACE, 1.15f));
        }
        bottomRow(kb);

        // 标区域：行 0-1 声母；行 2 韵母；行 3 韵母 / 声调；行 4 整体认读（只有保留整行时才算）
        int contentRows = wholeRow ? 4 : 3;
        for (int r = 0; r <= contentRows; r++) {
            List<Key> row = kb.rows.get(r);
            for (int i = 0; i < row.size(); i++) {
                Key k = row.get(i);
                if (k.action != A_CHAR) continue;      // 123 / ⌫ / 模式键不标区
                if (r == 0 || r == 1) {
                    k.zone = ZONE_INITIAL;
                } else if (r == 2) {
                    k.zone = ZONE_FINAL;
                } else if (r == 3) {
                    char c = k.label.charAt(0);
                    k.zone = (c == 'ˉ' || c == 'ˊ' || c == 'ˇ' || c == 'ˋ' || c == '˙')
                            ? ZONE_TONE : ZONE_FINAL;
                } else if (r == 4) {
                    k.zone = ZONE_WHOLE;
                }
            }
        }
        return kb;
    }

    /** 五十音格子的罗马字标注，日语假名键盘上方用。 */
    static String romajiOf(String kana) {
        if (kana == null || kana.length() != 1) return null;
        char c = kana.charAt(0);
        switch (c) {
            case 'あ': return "a";
            case 'い': return "i";
            case 'う': return "u";
            case 'え': return "e";
            case 'お': return "o";
            case 'か': return "ka";
            case 'き': return "ki";
            case 'く': return "ku";
            case 'け': return "ke";
            case 'こ': return "ko";
            case 'さ': return "sa";
            case 'し': return "shi";
            case 'す': return "su";
            case 'せ': return "se";
            case 'そ': return "so";
            case 'た': return "ta";
            case 'ち': return "chi";
            case 'つ': return "tsu";
            case 'て': return "te";
            case 'と': return "to";
            case 'な': return "na";
            case 'に': return "ni";
            case 'ぬ': return "nu";
            case 'ね': return "ne";
            case 'の': return "no";
            case 'は': return "ha";
            case 'ひ': return "hi";
            case 'ふ': return "fu";
            case 'へ': return "he";
            case 'ほ': return "ho";
            case 'ま': return "ma";
            case 'み': return "mi";
            case 'む': return "mu";
            case 'め': return "me";
            case 'も': return "mo";
            case 'や': return "ya";
            case 'ゆ': return "yu";
            case 'よ': return "yo";
            case 'ら': return "ra";
            case 'り': return "ri";
            case 'る': return "ru";
            case 'れ': return "re";
            case 'ろ': return "ro";
            case 'わ': return "wa";
            case 'を': return "wo";
            case 'ん': return "n";
            case 'っ': return "xtsu";
            case 'ー': return "-";
            default: return null;
        }
    }

    /** 日语罗马字键盘：键入罗马字，实时转成假名。罗马 / 平假名 / 片假名三态改成长按「拼注日」循环。 */
    static Keyboard jpRomaji() {
        Keyboard kb = new Keyboard("日语");
        kb.row();
        letters(kb, "qwertyuiop");
        kb.row();
        letters(kb, "asdfghjkl");
        kb.row();
        kb.rows.get(kb.rows.size() - 1).add(Key.punct(1.3f));
        letters(kb, "zxcvbnm");
        kb.rows.get(kb.rows.size() - 1).add(Key.act("⌫", A_BACKSPACE, 1.3f));
        bottomRow(kb);
        return kb;
    }

    /** 假名对应的小写假名，没有小写形式的返回 null。 */
    static String smallKanaOf(String kana) {
        if (kana == null || kana.length() != 1) return null;
        switch (kana.charAt(0)) {
            case 'あ': return "ぁ";
            case 'い': return "ぃ";
            case 'う': return "ぅ";
            case 'え': return "ぇ";
            case 'お': return "ぉ";
            case 'や': return "ゃ";
            case 'ゆ': return "ゅ";
            case 'よ': return "ょ";
            case 'つ': return "っ";
            case 'わ': return "ゎ";
            default: return null;
        }
    }

    static Keyboard jpKana() {
        return jpKana(false);
    }

    /**
     * 日语五十音键盘，六行：
     * 四行五十音 → や行（や ゆ よ わ を + 小假名开关 ぁ）→ ん っ ー ゛ ゜ 、 。 ⌫ → 底行。
     * small 为真时（按下小假名开关）：能出小写假名的键换成小字并高亮，出不了的键置灰锁死。
     */
    static Keyboard jpKana(boolean small) {
        Keyboard kb = new Keyboard("日语");
        String[][] grid = {
                {"あ", "い", "う", "え", "お", "か", "き", "く", "け", "こ"},
                {"さ", "し", "す", "せ", "そ", "た", "ち", "つ", "て", "と"},
                {"な", "に", "ぬ", "ね", "の", "は", "ひ", "ふ", "へ", "ほ"},
                {"ま", "み", "む", "め", "も", "ら", "り", "る", "れ", "ろ"},
        };
        for (int r = 0; r < grid.length; r++) {
            List<Key> keys = new ArrayList<Key>();
            for (int i = 0; i < grid[r].length; i++) {
                String hira = grid[r][i];
                String small1 = small ? smallKanaOf(hira) : null;
                if (small1 != null) {
                    keys.add(Key.ch(small1, small1));
                } else {
                    Key k = Key.ch(hira, hira);
                    k.disabled = small;
                    keys.add(k);
                }
            }
            kb.rows.add(keys);
        }
        // や行：や ゆ よ わ を + 小假名开关（开关键面用小号的「ぁ」）
        List<Key> yaRow = new ArrayList<Key>();
        String[] ya = {"や", "ゆ", "よ", "わ", "を"};
        for (int i = 0; i < ya.length; i++) {
            String small1 = small ? smallKanaOf(ya[i]) : null;
            if (small1 != null) {
                yaRow.add(Key.ch(small1, small1));
            } else {
                Key k = Key.ch(ya[i], ya[i]);
                k.disabled = small;
                yaRow.add(k);
            }
        }
        yaRow.add(Key.act("ぁ", A_SMALL_KANA, 0.95f));
        kb.rows.add(yaRow);

        // 发音符号（ん っ ー ゛ ゜）+ 逗号句号 + 退格：音标与标点单独一行
        List<Key> symRow = new ArrayList<Key>();
        String[] syms = {"ん", "っ", "ー"};
        for (int i = 0; i < syms.length; i++) {
            String small1 = small ? smallKanaOf(syms[i]) : null;
            if (small1 != null) {
                symRow.add(Key.ch(small1, small1));
            } else {
                Key k = Key.ch(syms[i], syms[i]);
                k.disabled = small;
                symRow.add(k);
            }
        }
        symRow.add(Key.act("゛", A_DAKU, 0.95f));
        symRow.add(Key.act("゜", A_HANDAKU, 0.95f));
        symRow.add(Key.ch("、", "、"));
        symRow.add(Key.ch("。", "。"));
        symRow.add(Key.act("⌫", A_BACKSPACE, 1.35f));
        kb.rows.add(symRow);
        bottomRowKana(kb);
        return kb;
    }

    /**
     * 数字键盘（按参考图）：左列一块可上下滑动的运算符条（＋ － × ÷ ％ ＝，一屏露 4 个，滑一下看另外两个），
     * 中间九宫 1-9，右列 ⌫ / 0 / ABC；底行「当前方案」「字符」「空格」「返回」「Enter」。
     * modeLabel 是进入数字页之前那个模式名，显示在底行第一个键上，按一下就回到它。
     */
    static Keyboard numbers(String modeLabel) {
        Keyboard kb = new Keyboard("数字");
        kb.opStrip = new ArrayList<Key>();
        kb.opStrip.add(Key.ch("＋", "+"));
        kb.opStrip.add(Key.ch("－", "-"));
        kb.opStrip.add(Key.ch("×", "*"));
        kb.opStrip.add(Key.ch("÷", "/"));
        kb.opStrip.add(Key.ch("％", "%"));
        // 「＝」按下去直接把输入框里那串算式算成结果（和候选里的 1＋1＝2 一致）
        kb.opStrip.add(Key.act("＝", A_CALC, 1f));
        // 左列留白（A_NONE 占位，绘制与点按都由运算符条接管）
        Key pad = new Key("", null, A_NONE, 1f, null);
        kb.row(
                pad,
                Key.digit("1", "."), Key.digit("2", "，"), Key.digit("3", "。"),
                Key.act("⌫", A_BACKSPACE, 1f));
        kb.row(
                new Key("", null, A_NONE, 1f, null),
                Key.digit("4", "？"), Key.digit("5", "！"), Key.digit("6", "："),
                Key.digit("0", "#"));
        kb.row(
                new Key("", null, A_NONE, 1f, null),
                Key.digit("7", "；"), Key.digit("8", "、"), Key.digit("9", "@"),
                Key.act("ABC", A_ALPHA, 1f));
        kb.row(
                Key.act(modeLabel == null || modeLabel.isEmpty() ? "拼音" : modeLabel, A_BACK_MODE, 1.05f),
                Key.act("字符", A_SYMBOL, 1.05f),
                Key.act("空格", A_SPACE, 2.4f),
                Key.act("返回", A_BACK_MODE, 1.05f),
                Key.act("Enter", A_ENTER, 1.05f));
        return kb;
    }

    static Keyboard numbers() {
        return numbers("拼音");
    }

    /** 五十音格子在绘制时按平/片假名状态换字形，这里只做转换备用。 */
    static String kata(String hira, boolean katakana) {
        if (!katakana) return hira;
        StringBuilder sb = new StringBuilder();
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

    static Keyboard symbols() {
        return symbols(true);
    }

    /** 符号页的总屏数：常用 / 数学箭头 / 序号与进阶符号。 */
    static final int SYMBOL_PAGES = 3;

    /** 符号页；stroke 为假时不再出现「笔画」键（笔画输入法在设置里被关掉了）。 */
    static Keyboard symbols(boolean stroke) {
        return symbols(stroke, 0);
    }

    /**
     * 符号页三屏，左右横向滑动切换（不占键位）：
     *   1 屏最常敲的（数字、四则、中文标点、括号书名号）；
     *   2 屏半角编程标点、数学符号、箭头与图形标记；
     *   3 屏带圈序号、集合/几何符号、直角引号与方头括号。
     * 三屏都是 4 行符号 + 1 行动作键 + 1 行底排，键盘高度不变。
     */
    static Keyboard symbols(boolean stroke, int page) {
        if (page < 0) page = 0;
        if (page >= SYMBOL_PAGES) page = SYMBOL_PAGES - 1;
        Keyboard kb = new Keyboard("符号 " + (page + 1) + "/" + SYMBOL_PAGES);
        if (page == 1) {
            kb.row(
                    Key.ch("<"), Key.ch(">"), Key.ch("\""), Key.ch("'"), Key.ch("\\"),
                    Key.ch("|"), Key.ch("_"), Key.ch("^"), Key.ch("~"), Key.ch("`"));
            kb.row(
                    Key.ch("{"), Key.ch("}"), Key.ch("["), Key.ch("]"), Key.ch("("),
                    Key.ch(")"), Key.ch("×"), Key.ch("÷"), Key.ch("±"), Key.ch("√"));
            kb.row(
                    Key.ch("≈"), Key.ch("≠"), Key.ch("≤"), Key.ch("≥"), Key.ch("∞"),
                    Key.ch("°"), Key.ch("‰"), Key.ch("€"), Key.ch("£"), Key.ch("¥"));
            kb.row(
                    Key.ch("→"), Key.ch("←"), Key.ch("↑"), Key.ch("↓"), Key.ch("•"),
                    Key.ch("★"), Key.ch("☆"), Key.ch("●"), Key.ch("○"), Key.ch("©"));
        } else if (page == 2) {
            kb.row(
                    Key.ch("①"), Key.ch("②"), Key.ch("③"), Key.ch("④"), Key.ch("⑤"),
                    Key.ch("⑥"), Key.ch("⑦"), Key.ch("⑧"), Key.ch("⑨"), Key.ch("⑩"));
            kb.row(
                    Key.ch("∑"), Key.ch("∏"), Key.ch("∫"), Key.ch("∮"), Key.ch("∈"),
                    Key.ch("∉"), Key.ch("⊂"), Key.ch("⊃"), Key.ch("∪"), Key.ch("∩"));
            kb.row(
                    Key.ch("∅"), Key.ch("∠"), Key.ch("⊥"), Key.ch("∥"), Key.ch("△"),
                    Key.ch("▽"), Key.ch("◇"), Key.ch("□"), Key.ch("※"), Key.ch("§"));
            kb.row(
                    Key.ch("「"), Key.ch("」"), Key.ch("『"), Key.ch("』"), Key.ch("〈"),
                    Key.ch("〉"), Key.ch("〔"), Key.ch("〕"), Key.ch("〖"), Key.ch("〗"));
        } else {
            kb.row(
                    Key.ch("1"), Key.ch("2"), Key.ch("3"), Key.ch("4"), Key.ch("5"),
                    Key.ch("6"), Key.ch("7"), Key.ch("8"), Key.ch("9"), Key.ch("0"));
            kb.row(
                    Key.ch("+"), Key.ch("-"), Key.ch("*"), Key.ch("/"), Key.ch("="),
                    Key.ch("%"), Key.ch("（"), Key.ch("）"), Key.ch("￥"), Key.ch("$"));
            kb.row(
                    Key.ch("，"), Key.ch("。"), Key.ch("？"), Key.ch("！"), Key.ch("："),
                    Key.ch("；"), Key.ch("、"), Key.ch("“"), Key.ch("”"), Key.ch("…"));
            kb.row(
                    Key.ch("【"), Key.ch("】"), Key.ch("《"), Key.ch("》"), Key.ch("—"),
                    Key.ch("～"), Key.ch("@"), Key.ch("#"), Key.ch("&"), Key.ch("·"));
        }
        if (stroke) {
            kb.row(
                    Key.act("笔画", A_STROKE, 1.1f),
                    Key.act("复制", A_COPY, 1f),
                    Key.act("全选复制", A_SELECT_ALL, 1.4f),
                    Key.act("粘贴", A_PASTE, 1f),
                    Key.act("剪贴板", A_CLIP, 1.5f));
        } else {
            kb.row(
                    Key.act("复制", A_COPY, 1.2f),
                    Key.act("全选复制", A_SELECT_ALL, 1.5f),
                    Key.act("粘贴", A_PASTE, 1.2f),
                    Key.act("剪贴板", A_CLIP, 1.6f));
        }
        kb.row(
                Key.act("ABC", A_ALPHA, 1.1f),
                // 符号页里空格左边留一个 123，点一下回数字键盘
                Key.act("123", A_NUMBER, 1.1f),
                Key.act("空格", A_SPACE, 2.4f),
                Key.act("⌫", A_BACKSPACE, 1.2f),
                Key.act("Enter", A_ENTER, 1.2f));
        return kb;
    }

    /**
     * 笔画键盘，四块区域：
     * 左列常用符号（，。123），中格九宫（一/丨/丿/丶/折/通配/笔拼/空格/中英），
     * 右列删除、分词、Enter。
     * 折笔用「ㄥ」表示——它本身就是横折的形状，与五笔画里的折笔对得上。
     */
    static Keyboard stroke() {
        Keyboard kb = new Keyboard("笔画");
        kb.row(
                Key.ch("，"),
                Key.ch("一", "1"),
                Key.ch("丨", "2"),
                Key.ch("丿", "3"),
                Key.act("⌫", A_BACKSPACE, 1.15f));
        kb.row(
                Key.ch("。"),
                Key.ch("丶", "4"),
                Key.ch("ㄥ", "5"),
                Key.ch("通配", "0"),
                Key.act("分词", A_SPLIT, 1.15f));
        kb.row(
                // 笔画页的 123 也支持长按进符号页（右上角标「符」）
                new Key("123", null, A_NUMBER, 1f, null, null, A_SYMBOL),
                Key.act("笔拼", A_STROKE_PINYIN, 1f),
                Key.act("空格", A_SPACE, 1.15f),
                Key.act("中/英", A_LANG, 1f),
                Key.act("Enter", A_ENTER, 1.15f));
        return kb;
    }
}

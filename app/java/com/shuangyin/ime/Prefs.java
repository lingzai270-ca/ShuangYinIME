package com.shuangyin.ime;

import android.content.Context;
import android.content.SharedPreferences;

final class Prefs {
    static final String NAME = "shuangyin";
    static final String KEY_SCHEME = "scheme";       // 0 拼音 1 注音 2 日语
    static final String KEY_FUZZY = "fuzzy";
    static final String KEY_ABBR = "abbr";
    static final String KEY_COUNT = "count";
    static final String KEY_VIBRATE = "vibrate";
    static final String KEY_ENGLISH_DEFAULT = "english_default";
    static final String KEY_KATAKANA = "katakana";   // 日语是否输出片假名
    static final String KEY_KANA_LAYOUT = "kana_layout"; // 日语是否用五十音键盘
    static final String KEY_ASSOC = "assoc";         // 联想词开关
    static final String KEY_CAT_MASK = "cat_mask";   // 词库分类位掩码
    static final String KEY_READING = "reading";     // 候选字上方注音开关
    static final String KEY_ZY_READ_MODE = "zy_read_mode"; // 注音候选小字：0 拼音 1 注音符号
    static final String KEY_SINGLE = "single_only";  // 展开面板筛选：只出单字
    static final String KEY_USERDICT = "userdict";   // 用户词库开关
    static final String KEY_VISUAL = "visual_feedback"; // 按键按下蓝底开关
    static final String KEY_KEY_BOUNCE = "key_bounce";   // 机械键盘回弹开关（与蓝底分开）
    static final String KEY_HEIGHT = "key_height";      // 旧的统一高度（不再使用，仅保留键名）
    static final String KEY_HEIGHT_PINYIN = "h_pinyin"; // 拼音（含中/英）键盘高度
    static final String KEY_HEIGHT_ZHUYIN = "h_zhuyin"; // 注音键盘高度
    static final String KEY_HEIGHT_JP = "h_jp";         // 日语键盘高度
    static final String KEY_HEIGHT_STROKE = "h_stroke"; // 笔画键盘高度
    static final String KEY_ZY_GROUP = "zhuyin_group";  // 注音分区键盘开关
    static final String KEY_KANA_ROMAJI = "kana_romaji"; // 假名键上方罗马音开关
    static final String KEY_ZY_LAYOUT = "zy_layout";    // 注音布局 0 当前 1 大千 2 倚天 3 精业 4 IBM
    static final String KEY_ZY_TINT = "zy_tint";        // 注音分区：区域底色
    static final String KEY_ZY_BORDER = "zy_border";    // 注音分区：区域边框
    static final String KEY_ZY_WHOLE = "zy_whole";      // 注音分区：整体认读整行
    static final String KEY_ROMAJI_COLOR = "romaji_color"; // 假名上方罗马音颜色
    static final String KEY_HINT_COLOR = "hint_color";     // 注音右下角小字颜色
    static final String KEY_HL_COLOR = "hl_color";         // 候选小字：已打出的那一截的高亮色
    static final String KEY_ST_READ_COLOR = "st_read_color"; // 笔画候选上方的拼音颜色
    static final String KEY_CAND_SEL_BG = "cand_sel_bg";   // 选中候选的底色
    static final String KEY_CAND_SEL_FG = "cand_sel_fg";   // 选中候选的文字颜色
    static final String KEY_TIME_CAND = "time_cand";       // 打出「时间 / 日期」时补上系统时间日期
    /** 候选上方小字（拼音 / 注音 / 罗马音）的百分比大小。 */
    static final String KEY_READ_SCALE = "read_scale";
    /** 键面右下角小字（注音对应拼音 / 假名罗马音）的百分比大小。 */
    static final String KEY_HINT_SCALE = "hint_scale";
    // 注音分区配色：四块区域的底色、三块区域的边框（声调不描边），都可以自己调（色值已含透明度）
    static final String KEY_ZY_TINT_INITIAL = "zy_tint_initial";
    static final String KEY_ZY_TINT_FINAL = "zy_tint_final";
    static final String KEY_ZY_TINT_WHOLE = "zy_tint_whole";
    static final String KEY_ZY_TINT_TONE = "zy_tint_tone";
    static final String KEY_ZY_BORDER_INITIAL = "zy_bd_initial";
    static final String KEY_ZY_BORDER_FINAL = "zy_bd_final";
    static final String KEY_ZY_BORDER_WHOLE = "zy_bd_whole";

    /** 注音分区默认配色（和 1.0.24 起的固定配色一致）。 */
    static final int[] ZY_TINT_DEFAULT = {0xFFEAF2FB, 0xFFEBF5EC, 0xFFFCF3E4, 0xFFFBEDF0};
    static final int[] ZY_BORDER_DEFAULT = {0xFF7FA9CE, 0xFF6FAF7C, 0xFFC79C4E};
    static final String KEY_ON_PINYIN = "on_pinyin";       // 拼音输入法开关
    static final String KEY_ON_ZHUYIN = "on_zhuyin";       // 注音输入法开关
    static final String KEY_ON_STROKE = "on_stroke";       // 笔画输入法开关
    static final String KEY_ON_JP = "on_jp";               // 日语输入法开关

    /** 注音布局名，下标即取值。 */
    static final String[] ZY_LAYOUT_NAMES = {"当前方案", "大千式", "倚天式", "精业式", "IBM式"};

    /** 颜色可选项：名称 + 色值。 */
    static final String[] COLOR_NAMES = {"灰蓝", "红", "橙", "棕", "绿", "蓝", "紫", "洋红", "青", "黑"};
    static final int[] COLOR_VALUES = {
            0xFF8FA3B3, 0xFFC62828, 0xFFD97706, 0xFF8D5B33, 0xFF2E7D5B,
            0xFF1565C0, 0xFF6A3FA0, 0xFFC2185B, 0xFF00796B, 0xFF22384D};

    /** 键盘高度百分比范围与默认值。 */
    static final int HEIGHT_MIN = 70;
    static final int HEIGHT_MAX = 150;
    static final int HEIGHT_DEFAULT = 100;

    /** 小字大小：候选栏上方那行注音 / 罗马音的百分比（100% 起，只能放大）。 */
    static final int READ_SCALE_MIN = 100;
    static final int READ_SCALE_MAX = 180;
    static final int READ_SCALE_DEFAULT = 100;
    /** 小字大小：按键右下角（注音符号对应拼音 / 假名罗马音）的百分比，同样 100% 起。 */
    static final int HINT_SCALE_MIN = 100;
    static final int HINT_SCALE_MAX = 160;
    static final int HINT_SCALE_DEFAULT = 100;

    /**
     * 分类位（1.0.48 起按「频率高低」划分，一条词只落在一个类里）：
     * 1 高频（前 10%）2 常用（10–30%）4 一般（30–55%）8 低频（55–80%）16 长尾（80–100%）
     * 32 注音 64 日语。
     */
    static final int CAT_HIGH = 1;
    static final int CAT_COMMON = 2;
    static final int CAT_MID = 4;
    static final int CAT_LOW = 8;
    static final int CAT_TAIL = 16;
    static final int CAT_ZHUYIN = 32;
    static final int CAT_JP = 64;
    static final int CAT_ALL = 127;
    /** 默认全开：5 个频级 + 注音 + 日语，跟上一版能打出来的词一样多。 */
    static final int CAT_DEFAULT = CAT_ALL;

    /** 候选词数量范围与默认值。 */
    static final int COUNT_MIN = 5;
    static final int COUNT_MAX = 100;
    static final int COUNT_DEFAULT = 50;

    /** 1.0.21 起整套默认值改版，升级时按新默认来一次。 */
    private static final String KEY_MIGRATED = "migrated_1_0_21";
    /** 1.0.23 起词库分类位重新划分（按单字 / 词长细分），旧掩码整体重置一次。 */
    private static final String KEY_MIGRATED_CAT = "migrated_1_0_23_cat";
    /** 1.0.32 起默认只留常用单字 + 双字词（按用户截图那套），分类掩码再归位一次。 */
    private static final String KEY_MIGRATED_CAT2 = "migrated_1_0_32_cat";
    /** 1.0.46 起分类位改成「语义大类」，旧的按词长的掩码全部作废，升级时重置成新默认。 */
    private static final String KEY_MIGRATED_CAT3 = "migrated_1_0_46_cat";
    /** 1.0.48 起分类位改成「频率频级」，语义大类的掩码同样作废，再归位一次。 */
    private static final String KEY_MIGRATED_CAT4 = "migrated_1_0_48_cat";
    /** 1.0.34 起出厂那套又变了（笔画开 + 四处候选小字红），升级时按新默认归位一次。 */
    private static final String KEY_MIGRATED_134 = "migrated_1_0_34";

    static SharedPreferences get(Context ctx) {
        return ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    /**
     * 1.0.21 起默认值整套改版：只有拼音开着，注音 / 笔画 / 日语 / 假名键盘关；模糊音、简拼、震动、视觉反馈、候选字注音、
     * 用户词库、联想词关闭与否等全部按用户指定。老版本升级时按新默认来一次，之后用户的改动不再覆盖。
     */
    static void migrate(Context ctx) {
        SharedPreferences sp = get(ctx);
        if (!sp.getBoolean(KEY_MIGRATED, false)) {
            SharedPreferences.Editor ed = sp.edit();
            applyDefaults(ed);
            ed.putBoolean(KEY_MIGRATED, true).apply();
        }
        // 1.0.23：分类位含义全换了（1 不再是「常用字表」而是「常用单字」…），
        // 旧掩码直接重置成新默认，免得升级后开关状态与词库对不上
        if (!sp.getBoolean(KEY_MIGRATED_CAT, false)) {
            SharedPreferences.Editor ed = sp.edit();
            ed.putInt(KEY_CAT_MASK, CAT_DEFAULT);
            ed.putBoolean(KEY_MIGRATED_CAT, true).apply();
        }
        // 1.0.32：默认改成只留「常用单字 + 双字词」，老装机也按这套归位一次
        if (!sp.getBoolean(KEY_MIGRATED_CAT2, false)) {
            SharedPreferences.Editor ed = sp.edit();
            ed.putInt(KEY_CAT_MASK, CAT_DEFAULT);
            ed.putBoolean(KEY_MIGRATED_CAT2, true).apply();
        }
        // 1.0.34：出厂那套换成了「笔画开 + 四处小字红」，老装机整体归位一次
        if (!sp.getBoolean(KEY_MIGRATED_134, false)) {
            SharedPreferences.Editor ed = sp.edit();
            applyDefaults(ed);
            ed.putBoolean(KEY_MIGRATED_134, true).apply();
        }
        // 1.0.46：分类位从「按词长」改成「按语义大类」，旧掩码的位含义全变了，直接按新默认归位
        if (!sp.getBoolean(KEY_MIGRATED_CAT3, false)) {
            SharedPreferences.Editor ed = sp.edit();
            ed.putInt(KEY_CAT_MASK, CAT_DEFAULT);
            ed.putBoolean(KEY_MIGRATED_CAT3, true).apply();
        }
        // 1.0.48：分类位改成「频率频级」（5 档 + 注音 + 日语），语义大类的掩码同样作废
        if (!sp.getBoolean(KEY_MIGRATED_CAT4, false)) {
            SharedPreferences.Editor ed = sp.edit();
            ed.putInt(KEY_CAT_MASK, CAT_DEFAULT);
            ed.putBoolean(KEY_MIGRATED_CAT4, true).apply();
        }
    }

    /** 把用户指定的那套默认值写进 editor（恢复默认设置与首次升级共用）。 */
    static SharedPreferences.Editor applyDefaults(SharedPreferences.Editor ed) {
        ed.putInt(KEY_SCHEME, 0);                    // 默认全拼
        ed.putBoolean(KEY_ON_PINYIN, true);
        ed.putBoolean(KEY_ON_ZHUYIN, false);
        ed.putBoolean(KEY_ON_STROKE, true);          // 笔画开（默认「拼音 + 笔画」两套）
        ed.putBoolean(KEY_ON_JP, false);
        ed.putBoolean(KEY_KANA_LAYOUT, false);       // 日语假名键盘关
        ed.putBoolean(KEY_KATAKANA, false);
        ed.putBoolean(KEY_FUZZY, true);              // 模糊音开
        ed.putBoolean(KEY_ABBR, true);               // 简拼开
        ed.putBoolean(KEY_VIBRATE, true);            // 震动开
        ed.putBoolean(KEY_VISUAL, true);             // 按下蓝底开
        ed.putBoolean(KEY_KEY_BOUNCE, false);        // 机械回弹默认关
        ed.putBoolean(KEY_ASSOC, false);             // 联想词关
        ed.putBoolean(KEY_TIME_CAND, false);         // 时间日期候选关（要用户在设置里自己开）
        ed.putInt(KEY_READ_SCALE, READ_SCALE_DEFAULT); // 候选上方小字 100%
        ed.putInt(KEY_HINT_SCALE, HINT_SCALE_DEFAULT); // 键面小字 100%
        ed.putBoolean(KEY_READING, true);            // 候选字注音开
        ed.putInt(KEY_ZY_READ_MODE, 0);              // 注音候选小字：拼音
        ed.putBoolean(KEY_SINGLE, false);            // 展开面板筛选：词（非单字）
        ed.putBoolean(KEY_USERDICT, true);           // 用户词库开
        ed.putBoolean(KEY_KANA_ROMAJI, true);        // 假名上方罗马音开
        ed.putInt(KEY_COUNT, COUNT_DEFAULT);         // 50
        ed.putInt(KEY_HEIGHT_PINYIN, HEIGHT_DEFAULT);
        ed.putInt(KEY_HEIGHT_ZHUYIN, HEIGHT_DEFAULT);
        ed.putInt(KEY_HEIGHT_JP, HEIGHT_DEFAULT);
        ed.putInt(KEY_HEIGHT_STROKE, HEIGHT_DEFAULT);
        ed.putBoolean(KEY_ZY_GROUP, false);          // 注音分区关
        ed.putBoolean(KEY_ZY_TINT, false);           // 分区底色关
        ed.putBoolean(KEY_ZY_BORDER, false);         // 分区边框关
        ed.putBoolean(KEY_ZY_WHOLE, false);          // 整体认读整行关
        ed.putInt(KEY_ZY_LAYOUT, 1);                 // 注音布局：大千式
        ed.putInt(KEY_HINT_COLOR, COLOR_VALUES[1]);  // 注音右下角：红
        ed.putInt(KEY_HL_COLOR, COLOR_VALUES[1]);    // 候选小字高亮：红
        ed.putInt(KEY_ST_READ_COLOR, COLOR_VALUES[1]); // 笔画候选上方拼音：红
        ed.putInt(KEY_CAND_SEL_BG, 0xFF3E7CB1);      // 选中候选：蓝底
        ed.putInt(KEY_CAND_SEL_FG, 0xFFFFFFFF);      // 选中候选：白字
        ed.putInt(KEY_ROMAJI_COLOR, COLOR_VALUES[1]);// 假名罗马音：红
        ed.putInt(KEY_ZY_TINT_INITIAL, ZY_TINT_DEFAULT[0]);
        ed.putInt(KEY_ZY_TINT_FINAL, ZY_TINT_DEFAULT[1]);
        ed.putInt(KEY_ZY_TINT_WHOLE, ZY_TINT_DEFAULT[2]);
        ed.putInt(KEY_ZY_TINT_TONE, ZY_TINT_DEFAULT[3]);
        ed.putInt(KEY_ZY_BORDER_INITIAL, ZY_BORDER_DEFAULT[0]);
        ed.putInt(KEY_ZY_BORDER_FINAL, ZY_BORDER_DEFAULT[1]);
        ed.putInt(KEY_ZY_BORDER_WHOLE, ZY_BORDER_DEFAULT[2]);
        ed.putInt(KEY_CAT_MASK, CAT_DEFAULT);        // 单字 + 通用词语 + 口语地名 + 注音 + 日语
        return ed;
    }

    /** 设置页「恢复默认设置」：清空所有偏好后写回默认值。 */
    static void resetToDefaults(Context ctx) {
        SharedPreferences.Editor ed = get(ctx).edit();
        ed.clear();
        applyDefaults(ed);
        ed.putBoolean(KEY_MIGRATED, true).apply();
    }

    static int scheme(Context ctx) {
        return get(ctx).getInt(KEY_SCHEME, 0);
    }

    static boolean katakana(Context ctx) {
        return get(ctx).getBoolean(KEY_KATAKANA, false);
    }

    static boolean kanaLayout(Context ctx) {
        return get(ctx).getBoolean(KEY_KANA_LAYOUT, false);
    }

    /** 四套输入法各自的开关：关掉就把它从方案循环里摘掉，相关按键也不再出现。 */
    static boolean pinyinOn(Context ctx) {
        return get(ctx).getBoolean(KEY_ON_PINYIN, true);
    }

    static boolean zhuyinOn(Context ctx) {
        return get(ctx).getBoolean(KEY_ON_ZHUYIN, false);
    }

    static boolean strokeOn(Context ctx) {
        return get(ctx).getBoolean(KEY_ON_STROKE, false);
    }

    static boolean japaneseOn(Context ctx) {
        return get(ctx).getBoolean(KEY_ON_JP, false);
    }

    /** 按 0 拼音 / 1 注音 / 2 日语 的顺序找第一个开着的方案，全关时退回拼音。 */
    static int firstEnabledScheme(Context ctx) {
        if (pinyinOn(ctx)) return 0;
        if (zhuyinOn(ctx)) return 1;
        if (japaneseOn(ctx)) return 2;
        return 0;
    }

    /** 「拼 / 注 / 日」这一圈里还开着几个方案：只开一个时底行的模式切换键就不需要了。 */
    static int cycleSchemeCount(Context ctx) {
        int n = 0;
        if (pinyinOn(ctx)) n++;
        if (zhuyinOn(ctx)) n++;
        if (japaneseOn(ctx)) n++;
        return n;
    }

    static boolean schemeEnabled(Context ctx, int scheme) {
        if (scheme == 1) return zhuyinOn(ctx);
        if (scheme == 2) return japaneseOn(ctx);
        return pinyinOn(ctx);
    }

    static boolean fuzzy(Context ctx) {
        return get(ctx).getBoolean(KEY_FUZZY, true);
    }

    static boolean abbr(Context ctx) {
        return get(ctx).getBoolean(KEY_ABBR, true);
    }

    static int count(Context ctx) {
        int v = get(ctx).getInt(KEY_COUNT, COUNT_DEFAULT);
        if (v < COUNT_MIN) return COUNT_MIN;
        if (v > COUNT_MAX) return COUNT_MAX;
        return v;
    }

    static boolean vibrate(Context ctx) {
        return get(ctx).getBoolean(KEY_VIBRATE, true);
    }

    static boolean assoc(Context ctx) {
        return get(ctx).getBoolean(KEY_ASSOC, false);
    }

    /** 候选字上方显示读音（拼音 / 注音 / 罗马音），默认开。 */
    static boolean reading(Context ctx) {
        return get(ctx).getBoolean(KEY_READING, true);
    }

    /** 注音模式下候选小字显示什么：0 拼音、1 注音符号，默认拼音。 */
    static int zyReadMode(Context ctx) {
        return get(ctx).getInt(KEY_ZY_READ_MODE, 0);
    }

    /** 展开面板筛选：true 只出单字，默认 false（词优先）。 */
    static boolean singleOnly(Context ctx) {
        return get(ctx).getBoolean(KEY_SINGLE, false);
    }

    /** 用户词库（自动记录常打的词并提权），默认开。 */
    static boolean userDict(Context ctx) {
        return get(ctx).getBoolean(KEY_USERDICT, true);
    }

    /** 按下蓝底：按住时键面变蓝，松手立刻恢复，默认开。 */
    static boolean visual(Context ctx) {
        return get(ctx).getBoolean(KEY_VISUAL, true);
    }

    /** 机械键盘回弹：按住下沉、松手弹回，默认关。 */
    static boolean keyBounce(Context ctx) {
        return get(ctx).getBoolean(KEY_KEY_BOUNCE, false);
    }

    /** 键盘高度百分比，70–150，默认 100。 */
    static int height(Context ctx) {
        int v = get(ctx).getInt(KEY_HEIGHT, HEIGHT_DEFAULT);
        return v < HEIGHT_MIN ? HEIGHT_MIN : (v > HEIGHT_MAX ? HEIGHT_MAX : v);
    }

    /** 按键盘类型各取各的高度：0 拼音（含英文）1 注音 2 日语 3 笔画。 */
    static int heightOf(Context ctx, int kind) {
        String key;
        switch (kind) {
            case 1: key = KEY_HEIGHT_ZHUYIN; break;
            case 2: key = KEY_HEIGHT_JP; break;
            case 3: key = KEY_HEIGHT_STROKE; break;
            default: key = KEY_HEIGHT_PINYIN; break;
        }
        int v = get(ctx).getInt(key, HEIGHT_DEFAULT);
        return v < HEIGHT_MIN ? HEIGHT_MIN : (v > HEIGHT_MAX ? HEIGHT_MAX : v);
    }

    static String heightKey(int kind) {
        switch (kind) {
            case 1: return KEY_HEIGHT_ZHUYIN;
            case 2: return KEY_HEIGHT_JP;
            case 3: return KEY_HEIGHT_STROKE;
            default: return KEY_HEIGHT_PINYIN;
        }
    }

    /** 候选上方小字百分比，100–180，默认 100（只能放大，不缩小）。 */
    static int readScale(Context ctx) {
        int v = get(ctx).getInt(KEY_READ_SCALE, READ_SCALE_DEFAULT);
        return v < READ_SCALE_MIN ? READ_SCALE_MIN : (v > READ_SCALE_MAX ? READ_SCALE_MAX : v);
    }

    /** 键面小字百分比，100–160，默认 100。实际绘制时还会被该键的字号上限压住。 */
    static int hintScale(Context ctx) {
        int v = get(ctx).getInt(KEY_HINT_SCALE, HINT_SCALE_DEFAULT);
        return v < HINT_SCALE_MIN ? HINT_SCALE_MIN : (v > HINT_SCALE_MAX ? HINT_SCALE_MAX : v);
    }

    /** 注音布局：0 当前方案 1 大千式 2 倚天式 3 精业式 4 IBM 式。 */
    static int zhuyinLayout(Context ctx) {
        int v = get(ctx).getInt(KEY_ZY_LAYOUT, 1);
        return v < 0 || v >= ZY_LAYOUT_NAMES.length ? 0 : v;
    }

    /** 分区键盘的底色，默认开。 */
    static boolean zhuyinTint(Context ctx) {
        return get(ctx).getBoolean(KEY_ZY_TINT, false);
    }

    /** 分区键盘的边框，默认开。 */
    static boolean zhuyinBorder(Context ctx) {
        return get(ctx).getBoolean(KEY_ZY_BORDER, false);
    }

    /** 分区键盘里「整体认读」那整行，默认开。 */
    static boolean zhuyinWholeRow(Context ctx) {
        return get(ctx).getBoolean(KEY_ZY_WHOLE, false);
    }

    /** 假名上方罗马音的颜色，默认灰蓝。 */
    static int romajiColor(Context ctx) {
        return get(ctx).getInt(KEY_ROMAJI_COLOR, 0xFF8FA3B3);
    }

    /** 注音符号右下角拼音小字的颜色，默认红。 */
    static int hintColor(Context ctx) {
        return get(ctx).getInt(KEY_HINT_COLOR, 0xFF8FA3B3);
    }

    /** 候选小字里「已经打出来的那一截」的高亮色，默认蓝。 */
    static int highlightColor(Context ctx) {
        return get(ctx).getInt(KEY_HL_COLOR, 0xFF3E7CB1);
    }

    /** 笔画候选上方的拼音颜色，默认蓝。 */
    static int strokeReadingColor(Context ctx) {
        return get(ctx).getInt(KEY_ST_READ_COLOR, 0xFF3E7CB1);
    }

    /** 选中那条候选的底色，默认蓝。 */
    static int selectedCandidateBg(Context ctx) {
        return get(ctx).getInt(KEY_CAND_SEL_BG, 0xFF3E7CB1);
    }

    /** 选中那条候选的文字颜色，默认白。 */
    static int selectedCandidateFg(Context ctx) {
        return get(ctx).getInt(KEY_CAND_SEL_FG, 0xFFFFFFFF);
    }

    /** 打出「时间 / 日期」时，候选项里补上当前的系统时间与日期（默认关）。 */
    static boolean timeCandidate(Context ctx) {
        return get(ctx).getBoolean(KEY_TIME_CAND, false);
    }

    /** 注音分区底色：kind 0 声母 1 韵母 2 整体认读 3 声调。 */
    static int zyTint(Context ctx, int kind) {
        return get(ctx).getInt(zyTintKey(kind), ZY_TINT_DEFAULT[Math.max(0, Math.min(3, kind))]);
    }

    static String zyTintKey(int kind) {
        switch (kind) {
            case 0: return KEY_ZY_TINT_INITIAL;
            case 1: return KEY_ZY_TINT_FINAL;
            case 2: return KEY_ZY_TINT_WHOLE;
            default: return KEY_ZY_TINT_TONE;
        }
    }

    /** 注音分区边框色：kind 0 声母 1 韵母 2 整体认读（声调不描边）。 */
    static int zyBorder(Context ctx, int kind) {
        return get(ctx).getInt(zyBorderKey(kind),
                ZY_BORDER_DEFAULT[Math.max(0, Math.min(2, kind))]);
    }

    static String zyBorderKey(int kind) {
        switch (kind) {
            case 0: return KEY_ZY_BORDER_INITIAL;
            case 1: return KEY_ZY_BORDER_FINAL;
            default: return KEY_ZY_BORDER_WHOLE;
        }
    }

    /** 注音分区键盘（声母 / 韵母 / 整体认读 / 声调分块摆放），默认关。 */
    static boolean zhuyinGroup(Context ctx) {
        return get(ctx).getBoolean(KEY_ZY_GROUP, false);
    }

    /** 平假名 / 片假名键上方显示罗马音，默认开。 */
    static boolean kanaRomaji(Context ctx) {
        return get(ctx).getBoolean(KEY_KANA_ROMAJI, true);
    }

    static int catMask(Context ctx) {
        return get(ctx).getInt(KEY_CAT_MASK, CAT_DEFAULT) & CAT_ALL;
    }
}

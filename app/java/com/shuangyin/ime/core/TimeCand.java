package com.shuangyin.ime.core;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 「时间 / 日期」候选：打出 时间、時間、时刻、時刻（→ 当前时间 23:43）或 日期、年月日（→ 当前日期 2026-11-19）
 * 时，在候选里补一条可以直接上屏的时间 / 日期串。
 */
public final class TimeCand {

    private static final String[] TIME_WORDS = {"时间", "時間", "时刻", "時刻"};
    private static final String[] DATE_WORDS = {"日期", "年月日"};

    private TimeCand() {
    }

    /** 这条候选词是不是「时间」类的触发词（必须整词相等，时间表不算）。 */
    public static boolean isTimeWord(String word) {
        if (word == null) return false;
        for (int i = 0; i < TIME_WORDS.length; i++) {
            if (TIME_WORDS[i].equals(word)) return true;
        }
        return false;
    }

    /** 这条候选词是不是「日期」类的触发词。 */
    public static boolean isDateWord(String word) {
        if (word == null) return false;
        for (int i = 0; i < DATE_WORDS.length; i++) {
            if (DATE_WORDS[i].equals(word)) return true;
        }
        return false;
    }

    /** 当前时间，24 小时制 HH:mm。 */
    public static String time(long ms) {
        return new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date(ms));
    }

    /** 当前日期 yyyy-MM-dd。 */
    public static String date(long ms) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date(ms));
    }

    /** 星期几，如「星期四」。 */
    public static String week(long ms) {
        Calendar c = Calendar.getInstance(Locale.CHINA);
        c.setTimeInMillis(ms);
        return WEEKS[c.get(Calendar.DAY_OF_WEEK) - 1];
    }

    private static final String[] WEEKS = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};

    /**
     * 时间 / 日期 / 星期 / 天气的快捷码。整串完全相等才出结果：
     * 打 tqx、tq、tj 之类半截一律不出，必须打满 tqxx 才给「天气：小雪」。
     */
    public static final String[] CODES = {"rq", "sj", "xq", "tqq", "tqy", "tqby", "tqxx", "tqyy"};

    /** 码对应的候选文字；不是完整码返回 null（不做前缀匹配）。 */
    public static String forCode(String code, long ms) {
        if (code == null) return null;
        if ("rq".equals(code)) return date(ms);
        if ("sj".equals(code)) return time(ms);
        if ("xq".equals(code)) return week(ms);
        if ("tqq".equals(code)) return "天气：晴";
        if ("tqy".equals(code)) return "天气：雨天";
        if ("tqby".equals(code)) return "天气：暴雨";
        if ("tqxx".equals(code)) return "天气：小雪";
        if ("tqyy".equals(code)) return "天气：阴云";
        return null;
    }
}

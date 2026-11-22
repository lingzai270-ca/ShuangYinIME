package com.shuangyin.ime;

import com.shuangyin.ime.core.TimeCand;

/** 「时间 / 日期候选」的 JVM 自检：触发词判定 + 输出格式。 */
public class TimeCandTest {

    public static void main(String[] args) {
        System.out.println("时间 -> " + TimeCand.isTimeWord("时间") + " (期望 true)");
        System.out.println("時間 -> " + TimeCand.isTimeWord("時間") + " (期望 true)");
        System.out.println("时刻 -> " + TimeCand.isTimeWord("时刻") + " (期望 true)");
        System.out.println("时间表 -> " + TimeCand.isTimeWord("时间表") + " (期望 false)");
        System.out.println("时候 -> " + TimeCand.isTimeWord("时候") + " (期望 false)");
        System.out.println("日期 -> " + TimeCand.isDateWord("日期") + " (期望 true)");
        System.out.println("年月日 -> " + TimeCand.isDateWord("年月日") + " (期望 true)");
        System.out.println("日 -> " + TimeCand.isDateWord("日") + " (期望 false)");

        // 2026-11-19 15:04:05 UTC+8 的时间戳
        long ms = 1795167845000L;
        System.out.println("time  " + TimeCand.time(ms) + " (格式 HH:mm)");
        System.out.println("date  " + TimeCand.date(ms) + " (格式 yyyy-MM-dd)");
        String t = TimeCand.time(System.currentTimeMillis());
        String d = TimeCand.date(System.currentTimeMillis());
        System.out.println("现在  " + d + " " + t);
        boolean ok = TimeCand.isTimeWord("时间") && TimeCand.isDateWord("日期")
                && !TimeCand.isTimeWord("时间表") && t.matches("\\d\\d:\\d\\d")
                && d.matches("\\d{4}-\\d\\d-\\d\\d");
        System.out.println(ok ? "OK 时间日期候选" : "FAIL 时间日期候选");
    }
}

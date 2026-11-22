package com.shuangyin.ime;

/** 布局自检 2：假名键盘（浊音 / 拗音 / 标点）、注音四套布局、无整体认读行版。 */
public class LayoutTest2 {
    public static void main(String[] args) {
        Keyboard kana = Keyboard.jpKana();
        System.out.println("== jpKana rows=" + kana.rowCount());
        for (int r = 0; r < kana.rows.size(); r++) {
            StringBuilder sb = new StringBuilder("row" + r + ": ");
            for (Keyboard.Key k : kana.rows.get(r)) {
                sb.append(k.label).append("(").append(k.action).append(") ");
            }
            System.out.println(sb.toString());
        }
        for (int id = 0; id <= 4; id++) {
            Keyboard kb = Keyboard.zhuyinLayout(id);
            StringBuilder sb = new StringBuilder("== zhuyin " + id + " [" + kb.name + "] rows=" + kb.rowCount() + "\n");
            for (int r = 0; r < kb.rows.size(); r++) {
                sb.append("row").append(r).append(" n=").append(kb.rows.get(r).size()).append(": ");
                for (Keyboard.Key k : kb.rows.get(r)) sb.append(k.label);
                sb.append("\n");
            }
            System.out.println(sb.toString());
        }
        Keyboard g1 = Keyboard.zhuyinGrouped(true);
        Keyboard g0 = Keyboard.zhuyinGrouped(false);
        System.out.println("grouped whole rows=" + g1.rowCount() + " noWhole rows=" + g0.rowCount());
        for (int r = 0; r < g0.rows.size(); r++) {
            StringBuilder sb = new StringBuilder("g0 row" + r + ": ");
            for (Keyboard.Key k : g0.rows.get(r)) sb.append(k.label).append("/").append(k.zone).append(" ");
            System.out.println(sb.toString());
        }
        // 浊音 / 半浊：用 FrostIME 的映射规则做一次等价检查
        String[] src = {"か", "さ", "た", "は", "ひ", "を", "ん"};
        for (String s : src) {
            System.out.println("voice " + s + " -> " + voice(s, false) + " / " + voice(s, true));
        }
    }

    private static String voice(String kana, boolean semi) {
        if (kana == null || kana.isEmpty()) return kana;
        String src = semi ? "はひふへほ" : "かきくけこさしすせそたちつてとはひふへほう";
        String dst = semi ? "ぱぴぷぺぽ" : "がぎぐげござじずぜぞだぢづでどばびぶべぼゔ";
        char last = kana.charAt(kana.length() - 1);
        int idx = src.indexOf(last);
        if (idx < 0) return kana;
        return kana.substring(0, kana.length() - 1) + dst.charAt(idx);
    }
}

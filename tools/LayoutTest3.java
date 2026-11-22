package com.shuangyin.ime;

/** 布局自检 3：假名六行（含发音符号行与小假名开关）、数字键盘。 */
public class LayoutTest3 {
    private static void dump(String title, Keyboard kb) {
        System.out.println("== " + title + " [" + kb.name + "] rows=" + kb.rowCount());
        for (int r = 0; r < kb.rows.size(); r++) {
            StringBuilder sb = new StringBuilder("row" + r + " n=" + kb.rows.get(r).size() + ": ");
            for (Keyboard.Key k : kb.rows.get(r)) {
                sb.append(k.label);
                if (k.disabled) sb.append("[灰]");
                sb.append(" ");
            }
            System.out.println(sb.toString());
        }
    }

    public static void main(String[] args) {
        dump("jpKana 普通", Keyboard.jpKana(false));
        dump("jpKana 小假名开", Keyboard.jpKana(true));
        dump("numbers", Keyboard.numbers());
        System.out.println("small of あ=" + Keyboard.smallKanaOf("あ") + " か=" + Keyboard.smallKanaOf("か")
                + " つ=" + Keyboard.smallKanaOf("つ") + " わ=" + Keyboard.smallKanaOf("わ")
                + " ん=" + Keyboard.smallKanaOf("ん") + " を=" + Keyboard.smallKanaOf("を"));
    }
}

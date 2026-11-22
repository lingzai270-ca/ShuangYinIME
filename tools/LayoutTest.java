package com.shuangyin.ime;

/** 布局自检：注音分区键盘的分区标注、假名罗马音表。 */
public class LayoutTest {
    public static void main(String[] args) {
        Keyboard kb = Keyboard.zhuyinGrouped();
        System.out.println("rows=" + kb.rowCount());
        for (int r = 0; r < kb.rows.size(); r++) {
            StringBuilder sb = new StringBuilder();
            sb.append("row").append(r).append(" n=").append(kb.rows.get(r).size()).append(" ");
            for (Keyboard.Key k : kb.rows.get(r)) {
                sb.append(k.label).append("/").append(k.zone).append("/").append(k.insert).append(" ");
            }
            System.out.println(sb.toString());
        }
        String[] probe = {"あ", "し", "つ", "ん", "っ", "ー", "を", "ふ", "か", "ア"};
        for (String s : probe) {
            System.out.println("romaji " + s + " -> " + Keyboard.romajiOf(s));
        }
        System.out.println("plain zhuyin rows=" + Keyboard.zhuyin().rowCount());
    }
}

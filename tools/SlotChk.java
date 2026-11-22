package com.shuangyin.ime;

import java.util.List;

/** 1.0.29 自检：注音单/多模式、英文、日语单模式的底行与上半部分排布。 */
public class SlotChk {

    public static void main(String[] args) {
        Keyboard zySingle = Keyboard.zhuyin();
        zySingle.zhuyinSingle();
        dump("注音（只开注音）· 单模式", zySingle);

        Keyboard zyMulti = Keyboard.zhuyin();
        zyMulti.zhuyinMulti();
        dump("注音 + 拼音（多模式）", zyMulti);

        Keyboard pin = Keyboard.pinyin(false);
        pin.fillHiddenSchemeSlot(0, false);
        dump("全拼（只开拼音）", pin);

        Keyboard eng = Keyboard.pinyin(true, true);
        eng.fillHiddenSchemeSlot(0, false);
        dump("英文（多模式下的英）", eng);

        Keyboard jpKana = Keyboard.jpRomaji();
        jpKana.fillHiddenSchemeSlot(2, true);
        dump("日语（只开日语，假名键盘开）", jpKana);

        Keyboard jpNoKana = Keyboard.jpRomaji();
        jpNoKana.fillHiddenSchemeSlot(2, false);
        dump("日语（只开日语，假名键盘关）", jpNoKana);
    }

    private static void dump(String title, Keyboard kb) {
        System.out.println("=== " + title + " ===");
        for (int r = 0; r < kb.rows.size(); r++) {
            List<Keyboard.Key> row = kb.rows.get(r);
            StringBuilder sb = new StringBuilder("  row" + r + "(" + row.size() + "): ");
            for (int i = 0; i < row.size(); i++) {
                Keyboard.Key k = row.get(i);
                String lbl = k.label.isEmpty() ? (k.action == Keyboard.A_SHIFT ? "⇧" : "空") : k.label;
                sb.append("[").append(lbl).append(" z").append(k.zone)
                        .append(" la=").append(k.longAction).append("] ");
            }
            System.out.println(sb.toString());
        }
        int space = 0, back = 0, shift = 0, punct = 0, tone1 = 0, sym = 0;
        for (int r = 0; r < kb.rows.size(); r++) {
            List<Keyboard.Key> row = kb.rows.get(r);
            for (int i = 0; i < row.size(); i++) {
                Keyboard.Key k = row.get(i);
                if (k.action == Keyboard.A_SPACE) space++;
                if (k.action == Keyboard.A_BACKSPACE) back++;
                if (k.action == Keyboard.A_SHIFT) shift++;
                if (k.action == Keyboard.A_CHAR && ("，。".equals(k.label) || "、。".equals(k.label))) punct++;
                if ("一声".equals(k.label)) tone1++;
                if (k.longAction == Keyboard.A_SYMBOL) sym++;
            }
        }
        System.out.println("  空格键=" + space + " 退格=" + back + " ⇧=" + shift
                + " 标点=" + punct + " 一声=" + tone1 + " 长按进符号=" + sym);
    }
}

package com.shuangyin.ime;

import java.util.List;

/** 用和 ImeView 相同的权重算法，把各键盘每行按键的实际宽度算出来核对。 */
public class LayoutCheck {

    static final float PAD_DP = 4f;
    static final float GAP_DP = 5f;

    public static void main(String[] args) {
        // 常见 360dp 宽屏，density 3
        int widthPx = 1080;
        float density = 3f;
        float usableW = widthPx - PAD_DP * density * 2;

        check("全拼", Keyboard.pinyin(false), usableW, density, widthPx);
        check("注音", Keyboard.zhuyin(), usableW, density, widthPx);
        check("日语罗马字", Keyboard.jpRomaji(), usableW, density, widthPx);
        check("日语五十音", Keyboard.jpKana(), usableW, density, widthPx);
        check("符号", Keyboard.symbols(), usableW, density, widthPx);
        check("笔画", Keyboard.stroke(), usableW, density, widthPx);
    }

    static void check(String name, Keyboard kb, float usableW, float density, int widthPx) {
        System.out.println("=== " + name + "  rows=" + kb.rowCount() + " ===");
        for (int r = 0; r < kb.rows.size(); r++) {
            List<Keyboard.Key> row = kb.rows.get(r);
            float sumW = 0;
            for (int i = 0; i < row.size(); i++) {
                sumW += row.get(i).weight;
            }
            float rowW = usableW - GAP_DP * density * (row.size() - 1);
            StringBuilder sb = new StringBuilder();
            float minDp = 999, maxDp = 0, centerAcc = 0;
            float x = PAD_DP * density;
            for (int i = 0; i < row.size(); i++) {
                Keyboard.Key k = row.get(i);
                float w = rowW * (k.weight / sumW);
                float wDp = w / density;
                if (wDp < minDp) minDp = wDp;
                if (wDp > maxDp) maxDp = wDp;
                if (i == 0) centerAcc = 0;
                if (k.action == Keyboard.A_SPACE) {
                    centerAcc = (x + w / 2f) / widthPx * 100f;
                }
                sb.append(String.format("%s=%.0fdp  ", k.label, wDp));
                x += w + GAP_DP * density;
            }
            System.out.println("  行" + (r + 1) + " 键数=" + row.size()
                    + " 最窄=" + String.format("%.1f", minDp) + "dp"
                    + " 最宽=" + String.format("%.1f", maxDp) + "dp"
                    + (centerAcc > 0 ? " 空格中心=" + String.format("%.1f%%", centerAcc) : ""));
            System.out.println("     " + sb);
        }
    }
}

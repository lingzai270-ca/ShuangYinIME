package com.shuangyin.ime;

import java.util.List;

/** 1.0.24 自检：注音大千式的区域标记、区域合并边框、单方案时移除「拼注日」。 */
public class ZyChk {

    public static void main(String[] args) {
        Keyboard zy = Keyboard.zhuyin();
        System.out.println("注音键盘名=" + zy.name + " 行数=" + zy.rowCount());
        for (int r = 0; r < zy.rows.size(); r++) {
            List<Keyboard.Key> row = zy.rows.get(r);
            StringBuilder sb = new StringBuilder("  row" + r + " (" + row.size() + "): ");
            for (int i = 0; i < row.size(); i++) {
                Keyboard.Key k = row.get(i);
                sb.append(k.label).append("{z").append(k.zone).append(",w").append(k.weight).append("} ");
            }
            System.out.println(sb.toString());
        }

        // 每行按 zone 合并相邻同类键，统计会画几个区域框（声调不画）
        int boxes = 0;
        for (int r = 0; r < zy.rows.size(); r++) {
            List<Keyboard.Key> row = zy.rows.get(r);
            int curZone = 0;
            StringBuilder sb = new StringBuilder("  行" + r + " 区域框: ");
            for (int i = 0; i < row.size(); i++) {
                Keyboard.Key k = row.get(i);
                boolean usable = k.zone == Keyboard.ZONE_INITIAL || k.zone == Keyboard.ZONE_FINAL
                        || k.zone == Keyboard.ZONE_WHOLE;
                int z = k.zone;
                if (!usable) {
                    if (curZone != 0) {
                        boxes++;
                        curZone = 0;
                    }
                    continue;
                }
                if (z != curZone) {
                    if (curZone != 0) boxes++;
                    curZone = z;
                    sb.append("[").append(zoneName(z)).append(" 起 ").append(k.label);
                } else {
                    sb.append(" ").append(k.label);
                }
            }
            if (curZone != 0) boxes++;
            sb.append("]");
            System.out.println(sb.toString());
        }
        System.out.println("区域框总数=" + boxes + "（声调不参与描边）");

        // 单方案：移除模式切换键，占位不塌、空格两侧比例不变
        Keyboard pin = Keyboard.pinyin(false);
        List<Keyboard.Key> last = pin.rows.get(pin.rows.size() - 1);
        float w0 = sum(last);
        int scheme0 = countAction(last, Keyboard.A_SCHEME);
        pin.removeSchemeKey();
        float w1 = sum(last);
        int scheme1 = countAction(last, Keyboard.A_SCHEME);
        System.out.println("拼音底行: 移除前权重=" + w0 + " 方案键=" + scheme0
                + " / 移除后权重=" + w1 + " 方案键=" + scheme1);
        StringBuilder sb = new StringBuilder("  底行: ");
        for (int i = 0; i < last.size(); i++) {
            Keyboard.Key k = last.get(i);
            sb.append("\"").append(k.label).append("\" a=").append(k.action)
                    .append(" w=").append(k.weight).append(" | ");
        }
        System.out.println(sb.toString());

        // 空占位键必须既不绘制也不响应（A_NONE）
        boolean ok = true;
        for (int i = 0; i < last.size(); i++) {
            Keyboard.Key k = last.get(i);
            if (k.label.isEmpty() && k.action != Keyboard.A_NONE) ok = false;
        }
        System.out.println("占位键为 A_NONE=" + ok + "（A_NONE=" + Keyboard.A_NONE + "）");
    }

    private static String zoneName(int z) {
        if (z == Keyboard.ZONE_INITIAL) return "声母";
        if (z == Keyboard.ZONE_FINAL) return "韵母";
        if (z == Keyboard.ZONE_WHOLE) return "认读";
        if (z == Keyboard.ZONE_TONE) return "声调";
        return "无";
    }

    private static float sum(List<Keyboard.Key> row) {
        float s = 0;
        for (int i = 0; i < row.size(); i++) s += row.get(i).weight;
        return s;
    }

    private static int countAction(List<Keyboard.Key> row, int action) {
        int n = 0;
        for (int i = 0; i < row.size(); i++) {
            if (row.get(i).action == action) n++;
        }
        return n;
    }
}

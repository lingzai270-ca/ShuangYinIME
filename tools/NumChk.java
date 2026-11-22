package com.shuangyin.ime;

import java.util.List;

public class NumChk {
    public static void main(String[] args) {
        Keyboard kb = Keyboard.numbers("注音");
        System.out.println("name=" + kb.name + " rows=" + kb.rows.size());
        for (int r = 0; r < kb.rows.size(); r++) {
            StringBuilder sb = new StringBuilder("row" + r + ": ");
            List<Keyboard.Key> row = kb.rows.get(r);
            for (Keyboard.Key k : row) {
                sb.append("[")
                        .append(k.action == Keyboard.A_NONE ? "<op>" : k.label)
                        .append("/w=").append(k.weight)
                        .append("/a=").append(k.action)
                        .append("] ");
            }
            System.out.println(sb.toString());
        }
        StringBuilder ops = new StringBuilder("opStrip: ");
        for (Keyboard.Key k : kb.opStrip) {
            ops.append(k.label).append("(a=").append(k.action).append(") ");
        }
        System.out.println(ops.toString());
        // 其它键盘仍不带运算符条
        Keyboard py = Keyboard.pinyin(false);
        System.out.println("pinyin opStrip=" + (py.opStrip == null ? "null" : "SET"));
        Keyboard sy = Keyboard.symbols();
        System.out.println("symbols opStrip=" + (sy.opStrip == null ? "null" : "SET")
                + " 底行=" + sy.rows.get(sy.rows.size() - 1).size() + "键");
    }
}

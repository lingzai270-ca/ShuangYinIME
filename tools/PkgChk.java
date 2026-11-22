package com.shuangyin.ime;

/** 布局 / 计算器的 JVM 冒烟自检（不依赖 Android 运行时）。 */
public final class PkgChk {
    public static void main(String[] a) {
        String[] exprs = {"12+34", "7*8-6", "100/8", "1.5+2.25", "9-", "12÷4", "3×3+1", "abc"};
        for (String e : exprs) {
            Double v = Calc.eval(e);
            System.out.println(e + " -> " + (v == null ? "null" : Calc.format(v)));
        }
        dump("zhuyin", Keyboard.zhuyinLayout(0));
        dump("大千式", Keyboard.zhuyinLayout(1));
        dump("numbers", Keyboard.numbers());
        dump("pinyin", Keyboard.pinyin(false, false));
        dump("english", Keyboard.pinyin(true, true));
        dump("jpRomaji", Keyboard.jpRomaji());
        dump("jpKana", Keyboard.jpKana(false));
        dump("symbolsOn", Keyboard.symbols(true));
        dump("symbolsOff", Keyboard.symbols(false));
        dump("stroke", Keyboard.stroke());
    }

    private static void dump(String tag, Keyboard kb) {
        System.out.println("== " + tag + " rows=" + kb.rowCount() + " english=" + kb.english);
        for (int i = 0; i < kb.rowCount(); i++) {
            StringBuilder sb = new StringBuilder("  r" + i + "(" + kb.rows.get(i).size() + "): ");
            for (Keyboard.Key k : kb.rows.get(i)) {
                sb.append(k.label);
                if (k.hint != null) sb.append('/').append(k.hint);
                sb.append(' ');
            }
            System.out.println(sb.toString());
        }
    }
}

public class CalcTest {
    public static void main(String[] a) {
        String[] exprs = {"12+34", "7*8-6", "100/8", "1.5+2.25", "9-", "12÷4", "3×3+1"};
        for (String e : exprs) {
            Double v = com.shuangyin.ime.Calc.eval(e);
            System.out.println(e + " -> " + (v == null ? "null" : com.shuangyin.ime.Calc.format(v)));
        }
        com.shuangyin.ime.Keyboard kb = com.shuangyin.ime.Keyboard.zhuyinLayout(0);
        System.out.println("zhuyin rows=" + kb.rowCount());
        for (int i = 0; i < kb.rowCount(); i++) {
            StringBuilder sb = new StringBuilder("row" + i + "(" + kb.rows.get(i).size() + "): ");
            for (com.shuangyin.ime.Keyboard.Key k : kb.rows.get(i)) {
                sb.append(k.label).append(' ');
            }
            System.out.println(sb.toString());
        }
        com.shuangyin.ime.Keyboard num = com.shuangyin.ime.Keyboard.numbers();
        System.out.println("numbers rows=" + num.rowCount());
        for (int i = 0; i < num.rowCount(); i++) {
            StringBuilder sb = new StringBuilder("num" + i + "(" + num.rows.get(i).size() + "): ");
            for (com.shuangyin.ime.Keyboard.Key k : num.rows.get(i)) {
                sb.append(k.label).append(' ');
            }
            System.out.println(sb.toString());
        }
    }
}

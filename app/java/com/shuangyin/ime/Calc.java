package com.shuangyin.ime;

/**
 * 数字键盘上的简易计算器：只支持 + - * / 和小数，够日常算账用。
 * 递归下降，先乘除后加减，括号不处理（数字键盘上也没有括号键）。
 */
final class Calc {

    private Calc() {
    }

    /** 算不出来（中间有缺项、以运算符结尾、除以 0）时返回 null。 */
    static Double eval(String expr) {
        if (expr == null || expr.isEmpty()) return null;
        String s = expr.replace('×', '*').replace('÷', '/');
        Parser p = new Parser(s);
        try {
            double v = p.expr();
            p.skipSpace();
            if (!p.atEnd()) return null;
            return Double.valueOf(v);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** 整数不带小数点，小数最多留 6 位并去掉尾部 0。 */
    static String format(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return "错误";
        if (Math.abs(v) < 1e15 && v == Math.rint(v)) {
            return String.valueOf((long) v);
        }
        String s = String.format(java.util.Locale.US, "%.6f", v);
        while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        return s;
    }

    private static final class Parser {
        private final String s;
        private int i;

        Parser(String s) {
            this.s = s;
        }

        boolean atEnd() {
            return i >= s.length();
        }

        void skipSpace() {
            while (i < s.length() && s.charAt(i) == ' ') i++;
        }

        double expr() {
            double v = term();
            while (true) {
                skipSpace();
                if (atEnd()) return v;
                char c = s.charAt(i);
                if (c == '+') {
                    i++;
                    v += term();
                } else if (c == '-') {
                    i++;
                    v -= term();
                } else {
                    return v;
                }
            }
        }

        double term() {
            double v = factor();
            while (true) {
                skipSpace();
                if (atEnd()) return v;
                char c = s.charAt(i);
                if (c == '*') {
                    i++;
                    v *= factor();
                } else if (c == '/') {
                    i++;
                    double d = factor();
                    if (d == 0d) throw new ArithmeticException("div0");
                    v /= d;
                } else {
                    return v;
                }
            }
        }

        double factor() {
            skipSpace();
            if (atEnd()) throw new IllegalArgumentException("eof");
            char c = s.charAt(i);
            if (c == '-') {
                i++;
                return -factor();
            }
            if (c == '+') {
                i++;
                return factor();
            }
            int start = i;
            while (i < s.length()) {
                char d = s.charAt(i);
                if ((d >= '0' && d <= '9') || d == '.') {
                    i++;
                } else {
                    break;
                }
            }
            if (i == start) throw new IllegalArgumentException("num");
            double v = Double.parseDouble(s.substring(start, i));
            // 百分号当后缀：20% → 0.2（数字键盘左列有 ％ 键）
            if (i < s.length() && (s.charAt(i) == '%' || s.charAt(i) == '％')) {
                i++;
                v /= 100d;
            }
            return v;
        }
    }
}

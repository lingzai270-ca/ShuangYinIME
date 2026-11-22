package com.shuangyin.ime;

public class CalcChk {
    public static void main(String[] args) {
        String[] cases = {"1+1", "12+34", "10%", "100*10%", "1+1%", "2*3", "9/2", "1+", "%", "20%-5"};
        for (String c : cases) {
            Double v = Calc.eval(c);
            System.out.println(c + " = " + (v == null ? "null" : Calc.format(v.doubleValue())));
        }
    }
}

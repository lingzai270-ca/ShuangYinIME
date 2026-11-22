package com.shuangyin.ime;

import com.shuangyin.ime.core.BinDict;
import com.shuangyin.ime.core.Candidate;
import com.shuangyin.ime.core.PinyinEngine;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 自检：单字模式逐字 + 连打整句（词库不含长词）。 */
public class SingleChk {
    public static void main(String[] a) throws Exception {
        String assets = a.length > 0 ? a[0] : "app/assets";
        Set<String> syls = new HashSet<String>();
        BufferedReader r = new BufferedReader(new InputStreamReader(
                new FileInputStream(new File(assets, "syllables.txt")), "UTF-8"));
        String l;
        while ((l = r.readLine()) != null) {
            l = l.trim();
            if (!l.isEmpty()) syls.add(l);
        }
        r.close();
        PinyinEngine pe = new PinyinEngine(new BinDict(new File(assets, "pinyin.bin")), syls);
        pe.setCatMask(127);

        int fail = 0;
        String q = "zhonghuarenmingongheguo";
        System.out.println("[单字模式] q=" + q + " 首音节长度=" + pe.firstSyllableLength(q));
        List<Candidate> s = pe.query(q, true, true, 18, true);
        for (Candidate c : s) System.out.println("  -> " + c.word + " s=" + c.score + " w=" + c.weight);
        if (s.isEmpty()) { System.out.println("  FAIL 单字候选为空"); fail++; }
        for (Candidate c : s) {
            if (c.word.codePointCount(0, c.word.length()) != 1) {
                System.out.println("  FAIL 出现非单字: " + c.word); fail++;
            }
        }
        if (!s.isEmpty() && !"中".equals(s.get(0).word)) {
            System.out.println("  WARN 首候选不是「中」: " + s.get(0).word);
        }
        if (pe.firstSyllableLength(q) != 5) { System.out.println("  FAIL 首音节应为 zhong(5)"); fail++; }

        System.out.println("[整句]");
        String[] qs = {"zhonghuarenmingongheguo", "woaini", "nihaoma", "wobuzhidao", "zhongguorenmin"};
        String[] want = {"中华人民共和国", "我爱你", "你好吗", "我不知道", "中国人民"};
        for (int i = 0; i < qs.length; i++) {
            List<Candidate> c = pe.query(qs[i], true, true, 18, false);
            String top = c.isEmpty() ? "(空)" : c.get(0).word;
            boolean ok = want[i].equals(top);
            System.out.println("  " + (ok ? "OK  " : "FAIL") + " " + qs[i] + " -> " + top + "（期望 " + want[i] + "）");
            if (!ok) fail++;
        }
        System.out.println(fail == 0 ? "ALL PASS" : ("FAILURES=" + fail));
    }
}

package com.shuangyin.ime;

import com.shuangyin.ime.core.BinDict;
import com.shuangyin.ime.core.Candidate;
import com.shuangyin.ime.core.PinyinEngine;
import com.shuangyin.ime.core.Readings;

import java.io.File;

/** 打印整句候选的 DP 过程与最终结果（调试用）。 */
public class SentDbg {
    public static void main(String[] a) throws Exception {
        String assets = a[0];
        java.util.Set<String> syls = new java.util.HashSet<String>();
        java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(
                new java.io.FileInputStream(new File(assets, "syllables.txt")), "UTF-8"));
        String l;
        while ((l = r.readLine()) != null) {
            l = l.trim();
            if (!l.isEmpty()) syls.add(l);
        }
        r.close();
        PinyinEngine pe = new PinyinEngine(new BinDict(new File(assets, "pinyin.bin")), syls);
        pe.setCatMask(127);
        String[] qs = a.length > 1 ? new String[]{a[1]} : new String[]{"zhonghuarenmingongheguo"};
        for (String q : qs) {
            System.out.println("Q=" + q);
            for (Candidate c : pe.query(q, true, true, 18)) {
                System.out.println("  -> " + c.word + " t=" + c.tones + " s=" + c.score);
            }
        }
    }
}

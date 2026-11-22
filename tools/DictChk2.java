package com.shuangyin.ime;

import com.shuangyin.ime.core.BinDict;
import com.shuangyin.ime.core.Candidate;
import com.shuangyin.ime.core.JapaneseEngine;
import com.shuangyin.ime.core.StrokeEngine;
import com.shuangyin.ime.core.ZhuyinEngine;

import java.io.File;
import java.io.IOException;
import java.util.List;

/** 三本词库（注音 / 日语 / 笔画）在新格式（v3, 16 位分类）下能否正常读出候选。 */
public class DictChk2 {
    public static void main(String[] args) throws IOException {
        String assets = args[0];
        BinDict zy = new BinDict(new File(assets, "zhuyin.bin"));
        BinDict jp = new BinDict(new File(assets, "japanese.bin"));
        BinDict st = new BinDict(new File(assets, "stroke.bin"));
        System.out.println("zhuyin keys=" + zy.keyCount(0) + " words=" + zy.wordCount(0));
        System.out.println("japanese keys=" + jp.keyCount(0) + " words=" + jp.wordCount(0));
        System.out.println("stroke keys=" + st.keyCount(0) + " words=" + st.wordCount(0));

        ZhuyinEngine ze = new ZhuyinEngine(zy);
        ze.setCatMask(511);
        List<Candidate> zres = ze.query("ㄏㄨㄤˊ", 12);
        System.out.println("zhuyin ㄏㄨㄤˊ -> " + words(zres));

        JapaneseEngine je = new JapaneseEngine(jp);
        je.setCatMask(511);
        List<Candidate> jres = je.query("にほん", false, 8);
        System.out.println("jp にほん -> " + words(jres));

        StrokeEngine se = new StrokeEngine(st);
        se.setCatMask(511);
        List<Candidate> sres = se.query("12345", 10);
        System.out.println("stroke 12345 -> " + words(sres));
    }

    private static String words(List<Candidate> list) {
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (Candidate c : list) {
            if (n++ >= 8) break;
            sb.append(c.word).append('(').append(c.cat).append(") ");
        }
        return sb.toString();
    }
}

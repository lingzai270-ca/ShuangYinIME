package com.shuangyin.ime;

import com.shuangyin.ime.core.BinDict;
import com.shuangyin.ime.core.Candidate;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

/** 按不同分类掩码看同一编码下的候选，验证「语义大类」过滤是否按预期生效。 */
public class CatChk {
    public static void main(String[] args) throws java.io.IOException {
        BinDict d = new BinDict(new File(args[0], "pinyin.bin"));
        String key = args[1];
        int[] masks = {1023, 775, 2, 8, 16};
        for (int m : masks) {
            List<Candidate> out = new ArrayList<Candidate>();
            d.exact(0, key, 20, 1.0f, m, out);
            StringBuilder sb = new StringBuilder("mask " + m + " -> ");
            for (Candidate c : out) {
                sb.append(c.word).append('[').append(c.weight).append("] ");
            }
            System.out.println(sb.toString());
        }
        String prefix = args.length > 2 ? args[2] : "";
        if (prefix.length() > 0) {
            List<Candidate> out = new ArrayList<Candidate>();
            d.textPrefix(0, prefix, 12, 1023, out);
            StringBuilder sb = new StringBuilder("assoc " + prefix + ": ");
            for (Candidate c : out) {
                sb.append(c.word).append('[').append(c.weight).append("] ");
            }
            System.out.println(sb.toString());
        }
    }
}

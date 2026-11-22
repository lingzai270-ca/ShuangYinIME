package com.shuangyin.ime;

import com.shuangyin.ime.core.BinDict;
import com.shuangyin.ime.core.Candidate;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** 打印某键下候选的 weight / tones / cat，用于核对声调元数据。 */
public final class KeyProbe {
    public static void main(String[] args) throws Exception {
        BinDict d = new BinDict(new File(args[0], "pinyin.bin"));
        for (int i = 1; i < args.length; i++) {
            List<Candidate> out = new ArrayList<Candidate>();
            d.exact(0, args[i], 2000, 1.0f, 127, out);
            StringBuilder sb = new StringBuilder();
            int n = 0;
            for (Candidate c : out) {
                if (n++ >= 25) break;
                sb.append(c.word).append("[w=").append(c.weight).append(" t='").append(c.tones)
                        .append("' cat=").append(c.cat).append("] ");
            }
            System.out.println("key=" + args[i] + " count=" + out.size() + " -> " + sb);
        }
        d.close();
    }
}

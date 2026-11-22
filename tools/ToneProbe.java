package com.shuangyin.ime;

import com.shuangyin.ime.core.BinDict;
import com.shuangyin.ime.core.Candidate;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class ToneProbe {
    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        String[] names = {"pinyin.bin", "zhuyin.bin"};
        String[] keys = {"ba", "bai", "ㄅㄚ"};
        for (int n = 0; n < names.length; n++) {
            BinDict d = new BinDict(new File(dir, names[n]));
            for (int k = 0; k < keys.length; k++) {
                List<Candidate> out = new ArrayList<Candidate>();
                d.exact(0, keys[k], 2000, 1.0f, 127, out);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < out.size(); i++) {
                    Candidate c = out.get(i);
                    if (!"把".equals(c.word) && !"百".equals(c.word) && !"吧".equals(c.word)) continue;
                    sb.append(c.word).append("[w=").append(c.weight).append(" t=").append(c.tones)
                            .append(" cat=").append(c.cat).append("] ");
                }
                System.out.println(names[n] + " key=" + keys[k] + " -> " + sb);
            }
            d.close();
        }
    }
}

package com.shuangyin.ime;

import com.shuangyin.ime.core.BinDict;
import com.shuangyin.ime.core.Candidate;
import com.shuangyin.ime.core.ZhuyinEngine;

import java.io.File;
import java.util.List;

public final class ZhyChk {
    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        BinDict zhy = new BinDict(new File(dir, "zhuyin.bin"));
        BinDict pin = new BinDict(new File(dir, "pinyin.bin"));
        ZhuyinEngine e = new ZhuyinEngine(zhy, pin);
        e.setCatMask(127);
        String[] keys = {"ㄅㄚ", "ㄅㄚ3", "ㄓ", "ㄨㄛ3", "ㄐㄩ", "ㄋㄧ3", "ㄏㄠ3",
                "ㄓㄨㄥㄍㄨㄛ2", "ㄒㄧㄝ4", "ㄧ", "ㄦ2", "ㄌㄩㄝ4", "ㄐㄧ1", "ㄅ"};
        for (int i = 0; i < keys.length; i++) {
            String k = keys[i];
            List<Candidate> list = e.query(k, 8);
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < list.size(); j++) {
                Candidate c = list.get(j);
                sb.append(c.word).append('(').append(String.format("%.0f", c.score))
                        .append('/').append(c.pinyin).append(") ");
            }
            System.out.println("[" + k + "] n=" + list.size() + " " + sb);
        }
        zhy.close();
        pin.close();
    }
}

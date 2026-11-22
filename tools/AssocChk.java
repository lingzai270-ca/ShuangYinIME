package com.shuangyin.ime;

import com.shuangyin.ime.core.BinDict;
import com.shuangyin.ime.core.Candidate;
import com.shuangyin.ime.core.TimeCand;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** 快捷码（时间/日期/星期/天气）+ 联想续写（以整词为前缀）抽查。 */
public class AssocChk {

    public static void main(String[] args) throws Exception {
        long now = System.currentTimeMillis();
        String[] codes = {"rq", "sj", "xq", "tqq", "tqy", "tqby", "tqxx", "tqyy",
                "tqx", "tq", "t", "s", "r", "tqbyy"};
        for (String c : codes) {
            String v = TimeCand.forCode(c, now);
            System.out.println("code " + c + " -> " + (v == null ? "(不出)" : v));
        }

        BinDict d = new BinDict(new File(args[0], "pinyin.bin"));
        String[] words = {"中国", "中华", "我", "北京", "天气", "你好", "时间", "谢谢"};
        for (String w : words) {
            List<Candidate> out = new ArrayList<Candidate>();
            d.textPrefix(0, w, 8, 511, out);
            StringBuilder sb = new StringBuilder(w + " -> ");
            for (Candidate c : out) {
                sb.append(c.word).append('(').append(c.weight).append(") ");
            }
            System.out.println(sb.toString().trim());
        }
    }
}

package com.shuangyin.ime;

import com.shuangyin.ime.core.BinDict;
import com.shuangyin.ime.core.Candidate;
import com.shuangyin.ime.core.PinyinEngine;
import com.shuangyin.ime.core.ZhuyinEngine;

import java.io.File;
import java.io.IOException;
import java.util.List;

/** 常用词候选质量抽查：整句切分、单字模式、候选排序是否都正常。 */
public class PinyinChk {

    public static void main(String[] args) throws IOException {
        String assets = args[0];
        BinDict pin = new BinDict(new File(assets, "pinyin.bin"));
        System.out.println("pinyin keys=" + pin.keyCount(0) + " words=" + pin.wordCount(0)
                + " abbr_keys=" + pin.keyCount(1));

        PinyinEngine pe = new PinyinEngine(pin, loadSyllables(new File(assets, "syllables.txt")));
        pe.setCatMask(511);
        show(pe, "huang", "黄");
        show(pe, "huangn", "黄牛");
        show(pe, "niu", "牛");
        show(pe, "shijian", "时间");
        show(pe, "zhonghua", "中华");
        show(pe, "riqi", "日期");
        show(pe, "nihao", "你好");
        show(pe, "xiexie", "谢谢");
        show(pe, "zhongguo", "中国");
        show(pe, "beijing", "北京");
        show(pe, "yinhang", "银行");
        show(pe, "chifan", "吃饭");
        show(pe, "sj", "简拼 sj");
        show(pe, "nh", "简拼 nh");

        BinDict zy = new BinDict(new File(assets, "zhuyin.bin"));
        ZhuyinEngine ze = new ZhuyinEngine(zy);
        ze.setCatMask(511);
        List<Candidate> zres = ze.query("ㄏㄨㄤˊ", 10);
        System.out.println("zhuyin ㄏㄨㄤˊ -> " + words(zres));
        zres = ze.query("ㄓㄨㄥㄏㄨㄚˊ", 10);
        System.out.println("zhuyin ㄓㄨㄥㄏㄨㄚˊ -> " + words(zres));

        if (args.length >= 2) {
            String code = args[1];
            List<Candidate> deep = pe.query(code, true, true, 20);
            StringBuilder sb = new StringBuilder(code + " 明细: ");
            for (Candidate c : deep) {
                sb.append(c.word).append('(').append((int) c.score).append(") ");
            }
            System.out.println(sb.toString());
        }
    }

    private static java.util.Set<String> loadSyllables(File f) throws IOException {
        java.util.Set<String> set = new java.util.HashSet<String>();
        java.io.BufferedReader r = new java.io.BufferedReader(
                new java.io.InputStreamReader(new java.io.FileInputStream(f), "UTF-8"));
        String line;
        while ((line = r.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty()) set.add(line);
        }
        r.close();
        return set;
    }

    private static void show(PinyinEngine pe, String code, String expect) {
        List<Candidate> list = pe.query(code, true, true, 10);
        String top = list.isEmpty() ? "(空)" : list.get(0).word;
        System.out.println(code + " -> " + words(list) + (top.equals(expect) ? "  OK" : "  !!期望" + expect));
    }

    private static String words(List<Candidate> list) {
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (Candidate c : list) {
            if (n++ >= 6) break;
            sb.append(c.word).append(' ');
        }
        return sb.toString().trim();
    }
}

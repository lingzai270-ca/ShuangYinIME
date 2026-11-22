import com.shuangyin.ime.core.BinDict;
import com.shuangyin.ime.core.Candidate;
import com.shuangyin.ime.core.JapaneseEngine;
import com.shuangyin.ime.core.PinyinEngine;
import com.shuangyin.ime.core.ZhuyinEngine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EngineTest {
    public static void main(String[] args) throws Exception {
        PrintStream out = new PrintStream(System.out, true, "UTF-8");
        System.setOut(out);
        String dir = args[0];
        Set<String> syl = new HashSet<String>();
        BufferedReader r = new BufferedReader(new InputStreamReader(
                new FileInputStream(new File(dir, "syllables.txt")), StandardCharsets.UTF_8));
        String line;
        while ((line = r.readLine()) != null) {
            if (!line.trim().isEmpty()) syl.add(line.trim());
        }
        r.close();
        out.println("音节表: " + syl.size());

        BinDict pyd = new BinDict(new File(dir, "pinyin.bin"));
        out.println("拼音键: " + pyd.keyCount(0) + "  词条: " + pyd.wordCount(0)
                + "  简拼键: " + pyd.keyCount(1) + "  简拼词条: " + pyd.wordCount(1));
        PinyinEngine py = new PinyinEngine(pyd, syl);
        String[] qs = {"ni", "nihao", "nihaoma", "zhongguo", "woshi", "wo", "nih", "nh",
                "jintiantianqihenhao", "xian", "lvxing", "yigeren", "shanghai", "zhongwen",
                "ceshi", "qq", "zhei", "keyi", "shuoming"};
        for (String q : qs) {
            long t0 = System.nanoTime();
            List<Candidate> c = py.query(q, true, true, 10);
            long ms = (System.nanoTime() - t0) / 1000000L;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < c.size(); i++) {
                sb.append(c.get(i).word).append(" ");
            }
            out.println(String.format("%-22s %4dms  %s", q, ms, sb.toString()));
        }

        BinDict zyd = new BinDict(new File(dir, "zhuyin.bin"));
        out.println("注音键: " + zyd.keyCount(0) + "  词条: " + zyd.wordCount(0));
        ZhuyinEngine zy = new ZhuyinEngine(zyd);
        String[] zs = {"ㄋㄧ", "ㄋㄧˇ", "ㄋㄧˇㄏㄠˇ", "ㄓㄨㄥ", "ㄓㄨㄥㄨㄣˊ", "ㄊㄞˊㄨㄢ", "ㄨㄛˇ"};
        for (String q : zs) {
            long t0 = System.nanoTime();
            List<Candidate> c = zy.query(q, 10);
            long ms = (System.nanoTime() - t0) / 1000000L;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < c.size(); i++) {
                sb.append(c.get(i).word).append(" ");
            }
            out.println(String.format("%-22s %4dms  %s", q, ms, sb.toString()));
        }

        File jpFile = new File(dir, "japanese.bin");
        if (jpFile.exists()) {
            BinDict jpd = new BinDict(jpFile);
            out.println("日语读音: " + jpd.keyCount(0) + "  词条: " + jpd.wordCount(0));
            JapaneseEngine jp = new JapaneseEngine(jpd);
            // 罗马字转假名
            String[] romaji = {"nihon", "gakkou", "konnichiwa", "kya", "kka", "shinbun", "tokyo", "arigatou"};
            for (int k = 0; k < romaji.length; k++) {
                out.println(String.format("  %-14s -> %s", romaji[k], JapaneseEngine.toHiragana(romaji[k])));
            }
            // 假名查词
            String[] jq = {"にほん", "がっこう", "とうきょう", "でんしゃ", "ありがとう", "たべる", "か"};
            for (String q : jq) {
                long t0 = System.nanoTime();
                List<Candidate> c = jp.query(q, false, 8);
                long ms = (System.nanoTime() - t0) / 1000000L;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < c.size(); i++) sb.append(c.get(i).word).append(" ");
                out.println(String.format("%-12s %4dms  ひら: %s", q, ms, sb.toString()));
                StringBuilder sb2 = new StringBuilder();
                List<Candidate> c2 = jp.query(q, true, 8);
                for (int i = 0; i < c2.size(); i++) sb2.append(c2.get(i).word).append(" ");
                out.println(String.format("%-12s       カナ: %s", "", sb2.toString()));
            }
        }
    }
}

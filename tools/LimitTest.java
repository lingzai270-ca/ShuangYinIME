import com.shuangyin.ime.core.BinDict;
import com.shuangyin.ime.core.Candidate;
import com.shuangyin.ime.core.PinyinEngine;
import com.shuangyin.ime.core.ZhuyinEngine;
import com.shuangyin.ime.core.ZhuyinSyllables;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 候选上限 50 / 100 时的产量冒烟：各方案能给出多少候选。 */
public final class LimitTest {

    public static void main(String[] args) throws Exception {
        File assets = new File(args[0]);
        int limit = Integer.parseInt(args[1]);

        Set<String> syl = new HashSet<String>();
        BufferedReader r = new BufferedReader(new InputStreamReader(
                new FileInputStream(new File(assets, "syllables.txt")), "UTF-8"));
        String line;
        while ((line = r.readLine()) != null) {
            String s = line.trim();
            if (!s.isEmpty()) syl.add(s);
        }
        r.close();

        BinDict pinyin = new BinDict(new File(assets, "pinyin.bin"));
        BinDict zhuyin = new BinDict(new File(assets, "zhuyin.bin"));
        PinyinEngine py = new PinyinEngine(pinyin, syl);
        ZhuyinEngine zy = new ZhuyinEngine(zhuyin);
        py.setCatMask(107);   // 新默认：扩展词、生僻字关闭
        zy.setCatMask(107);

        String[] pins = {"ni", "nihao", "wo", "zhongguo", "huangse", "shuangyin", "xuexi", "renmin"};
        for (String k : pins) {
            List<Candidate> out = py.query(k, true, true, limit);
            p("拼音 " + k + " -> " + out.size() + " 个，前五个 "
                    + head(out));
        }
        String[] zhys = {"ㄋㄧ", "ㄏㄨㄤ", "ㄓㄨㄥㄍㄨㄛ", "ㄒㄩㄝㄒㄧ"};
        for (String k : zhys) {
            List<Candidate> out = zy.query(k, limit);
            p("注音 " + k + " -> " + out.size() + " 个，前五个 " + head(out));
        }
        p("音节表 " + syl.size() + " / ZhuyinSyllables " + ZhuyinSyllables.size());
    }

    private static String head(List<Candidate> out) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < out.size() && i < 5; i++) {
            if (i > 0) sb.append(' ');
            sb.append(out.get(i).word);
        }
        return sb.toString();
    }

    private static void p(String s) {
        System.out.println(s);
    }
}

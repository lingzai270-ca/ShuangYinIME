import com.shuangyin.ime.core.BinDict;
import com.shuangyin.ime.core.Candidate;
import com.shuangyin.ime.core.JapaneseEngine;
import com.shuangyin.ime.core.PinyinEngine;
import com.shuangyin.ime.core.ZhuyinEngine;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 验证 v2 词库：分类过滤、联想段、三引擎查询。 */
public final class CatTest {
    static StringBuilder log = new StringBuilder();

    static void p(String s) {
        log.append(s).append('\n');
        System.out.println(s);
    }

    static String names(List<Candidate> list, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size() && i < n; i++) {
            if (i > 0) sb.append('/');
            sb.append(list.get(i).word);
        }
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        String dir = args[0];
        Set<String> syls = new HashSet<String>();
        BufferedReader r = new BufferedReader(new InputStreamReader(
                new FileInputStream(dir + "/syllables.txt"), "UTF-8"));
        String line;
        while ((line = r.readLine()) != null) {
            if (!line.trim().isEmpty()) syls.add(line.trim());
        }
        r.close();

        BinDict py = new BinDict(new java.io.File(dir, "pinyin.bin"));
        p("pinyin sections=" + py.sectionCount());
        p("assoc keys=" + py.keyCount(2));
        p("zhuyin keys=" + py.keyCount(0));

        BinDict zy = new BinDict(new java.io.File(dir, "zhuyin.bin"));
        BinDict jp = new BinDict(new java.io.File(dir, "japanese.bin"));

        PinyinEngine pe = new PinyinEngine(py, syls);
        List<Candidate> out = new ArrayList<Candidate>();
        for (int k = 0; k < 3; k++) {
            pe.query("wo", true, true, 9);
        }
        long t0 = System.nanoTime();
        pe.query("wo", true, true, 9);
        long t1 = System.nanoTime();
        p("单次 wo 查询耗时 ms=" + (t1 - t0) / 1000000.0);
        p("segment(wo) = " + pe.segmentForDisplay("wo"));
        p("全量 wo(15) → " + names(pe.query("wo", true, true, 15), 15));
        List<Candidate> dbg2 = pe.query("wo", true, true, 15);
        StringBuilder sc = new StringBuilder();
        for (int i = 0; i < dbg2.size(); i++) {
            sc.append(dbg2.get(i).word).append(":").append((int) dbg2.get(i).score)
                    .append("/w").append(dbg2.get(i).weight).append("  ");
        }
        p("wo 分数 → " + sc);
        p("全量 wo 不带模糊简拼 → " + names(pe.query("wo", false, false, 9), 9));
        List<Candidate> dbg = new ArrayList<Candidate>();
        p("exact women mask127 = " + py.exact(0, "women", 4, 0.8f, 127, dbg) + " → " + names(dbg, 4)
                + " score=" + dbg.get(0).score + " w=" + dbg.get(0).weight + " cat=" + dbg.get(0).cat);
        dbg.clear();
        p("exact women mask-1 = " + py.exact(0, "women", 4, 0.8f, -1, dbg) + " → " + names(dbg, 4));
        dbg.clear();
        p("exact women mask0 = " + py.exact(0, "women", 4, 0.8f, 0, dbg) + " → " + names(dbg, 4));
        p("音节表大小 = " + syls.size() + "，含 men=" + syls.contains("men"));
        pe.setCatMask(1); // 只开常用字
        p("仅常用 wo → " + names(pe.query("wo", true, true, 9), 6));
        pe.setCatMask(1 | 2 | 4);
        p("常用+基础+扩展 women → " + names(pe.query("women", true, true, 9), 6));
        pe.setCatMask(127);
        p("全量 women → " + names(pe.query("women", true, true, 9), 6));
        p("全量 jintiantianqihenhao → " + names(pe.query("jintiantianqihenhao", true, true, 5), 4));
        String[] qs = {"de", "shi", "nihao", "zhongguo", "xuexi", "nihaoMa", "womenzai", "beijing",
                "weixin", "zhifubao", "xiaochengxu", "waimai", "shujia", "gongzuo", "shenghuo"};
        for (int i = 0; i < qs.length; i++) {
            p("查 " + qs[i] + " → " + names(pe.query(qs[i], true, true, 9), 5));
        }

        List<Candidate> assoc = new ArrayList<Candidate>();
        int n = py.exact(2, "我", 12, 1f, -1, assoc);
        p("联想 我 → " + names(assoc, 8));
        assoc.clear();
        n = py.exact(2, "今", 12, 1f, -1, assoc);
        p("联想 今 → " + names(assoc, 8));
        assoc.clear();
        n = py.exact(2, "手", 12, 1f, -1, assoc);
        p("联想 手 → " + names(assoc, 8));

        ZhuyinEngine ze = new ZhuyinEngine(zy);
        p("注音 ㄨㄛˇ → " + names(ze.query("ㄨㄛˇ", 5), 4));
        ze.setCatMask(0); // 全关
        p("注音分类全关 ㄨㄛˇ → " + names(ze.query("ㄨㄛˇ", 5), 4));
        ze.setCatMask(32);

        JapaneseEngine je = new JapaneseEngine(jp);
        p("日语 にほん → " + names(je.query("にほん", false, 6), 6));
        p("日语 にほん(片假名) → " + names(je.query("にほん", true, 6), 6));
        je.setCatMask(0);
        p("日语分类关 にほん → " + names(je.query("にほん", false, 6), 6));

        java.io.OutputStreamWriter w = new java.io.OutputStreamWriter(
                new java.io.FileOutputStream(args[1]), "UTF-8");
        w.write(log.toString());
        w.close();
    }
}

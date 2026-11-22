import com.shuangyin.ime.core.BinDict;
import com.shuangyin.ime.core.Candidate;
import com.shuangyin.ime.core.ZhuyinEngine;

import java.io.File;
import java.io.PrintStream;
import java.util.List;

/** 注音冒烟：按声母/韵母/整体认读音节/声调 四个维度验证候选顺序。 */
public final class ZhuyinTest {
    public static void main(String[] args) throws Exception {
        PrintStream out = new PrintStream(System.out, true, "UTF-8");
        ZhuyinEngine e = new ZhuyinEngine(new BinDict(new File(args[0], "zhuyin.bin")));
        String[] keys = {
                // 单音节：四声应各归其位
                "ㄏㄨㄤ1", "ㄏㄨㄤ2", "ㄏㄨㄤ3", "ㄏㄨㄤ4", "ㄏㄨㄤ",
                // 整体认读音节（单符号即整字）
                "ㄓ", "ㄓ1", "ㄔ2", "ㄕ1", "ㄙ4", "ㄦ2",
                // 只打声母/介音（未打完音节）
                "ㄏ", "ㄓㄨ",
                // 双音节：应出词
                "ㄓㄨㄥ1ㄨㄣ2", "ㄒㄩㄝ2ㄕㄥ1", "ㄏㄨㄤ2ㄙㄜ4", "ㄅㄟ3ㄐㄧㄥ1",
                // 单字常用度（历史 bug 点）
                "ㄏㄨㄤ2", "ㄋㄧㄡ2", "ㄏㄠ3", "ㄒㄩㄝ2", "ㄍㄨㄛ2",
        };
        for (int i = 0; i < keys.length; i++) {
            List<Candidate> r = e.query(keys[i], 9);
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < r.size(); j++) {
                sb.append(r.get(j).word).append('(').append(r.get(j).tones).append(") ");
            }
            out.println(keys[i] + " -> " + sb);
        }
    }
}

import com.shuangyin.ime.core.BinDict;
import com.shuangyin.ime.core.Candidate;
import com.shuangyin.ime.core.StrokeEngine;

import java.io.File;
import java.util.List;

/** 笔画引擎冒烟测试：输入若干笔画码，看候选是否符合预期。 */
public class StrokeTest {

    public static void main(String[] args) throws Exception {
        File f = new File(args[0], "stroke.bin");
        StrokeEngine engine = new StrokeEngine(new BinDict(f));
        String[] codes = {"3", "31", "311", "3112", "3112|2512", "12212|2512", "3115|251",
                "1", "12", "34", "251", "12212", "12212512", "1234", "0", "25125"};
        for (int i = 0; i < codes.length; i++) {
            List<Candidate> list = engine.query(codes[i], 9);
            StringBuilder sb = new StringBuilder();
            sb.append(codes[i]).append(" -> ");
            for (int j = 0; j < list.size(); j++) {
                sb.append(list.get(j).word).append('(').append((int) list.get(j).score).append(") ");
            }
            System.out.println(sb.toString());
        }
        System.out.println("keys=" + engine.query("", 9).size());
    }
}

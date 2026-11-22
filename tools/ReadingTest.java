import com.shuangyin.ime.core.JapaneseEngine;
import com.shuangyin.ime.core.Readings;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.io.PrintStream;

/** 命令行校验：读音表加载 + 假名转罗马音。 */
public final class ReadingTest {
    public static void main(String[] args) throws Exception {
        PrintStream out = new PrintStream(System.out, true, "UTF-8");
        Readings r = Readings.load(new File(args[0]));
        out.println("entries=" + r.size());
        String[] chars = {"黄", "牛", "学", "是", "知", "手", "中", "行", "的"};
        for (int i = 0; i < chars.length; i++) {
            out.println(chars[i] + " 拼音=" + r.pinyin(chars[i]) + " 注音=" + r.zhuyin(chars[i]));
        }
        out.println("词 中文 -> " + r.word("中文", 1) + " / " + r.word("中文", 2));
        out.println("词 学生 -> " + r.word("学生", 1));
        out.println("词 日本 -> " + r.word("日本", 1) + " (日语词应查不到拼音)");

        String[] kana = {"にほん", "がっこう", "とうきょう", "しゃしん", "きょう", "まっちゃ",
                "コンピュータ", "ありがとう"};
        for (int i = 0; i < kana.length; i++) {
            out.println(kana[i] + " -> " + JapaneseEngine.toRomaji(kana[i]));
        }
    }
}

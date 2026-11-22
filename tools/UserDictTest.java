import com.shuangyin.ime.core.UserDict;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

/** 用户词库逻辑冒烟：自动学习 / 手动添加 / 修改 / 删自动 / 锁定生效 / 6 列持久化。 */
public final class UserDictTest {

    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        File f = new File(dir, "ud-a.txt");
        File f2 = new File(dir, "ud-b.txt");
        if (f.exists()) f.delete();
        if (f2.exists()) f2.delete();

        UserDict d = UserDict.get(f);
        p("A. 空库 size=" + d.size());

        d.record("huang", "黄");
        d.record("huang", "黄");
        d.record("huangse", "黄色");
        p("B. 自动学习后 auto=" + d.autoCount() + " manual=" + d.manualCount()
                + " usage(黄)=" + d.usage("黄"));

        boolean ok = d.addManual("niunai", "ㄋㄧㄡㄋㄞ", "牛奶");
        p("C. addManual ok=" + ok + " manual=" + d.manualCount() + " auto=" + d.autoCount());

        List<UserDict.Word> m1 = d.match("niunai", 4, true);
        p("D. 拼音码命中 " + m1.size() + " 条 -> "
                + (m1.isEmpty() ? "-" : m1.get(0).word + " manual=" + m1.get(0).manual));
        List<UserDict.Word> m2 = d.match("ㄋㄧㄡㄋㄞ", 4, false);
        p("E. 注音码命中(只手动词) " + m2.size() + " 条 -> "
                + (m2.isEmpty() ? "-" : m2.get(0).word));

        List<UserDict.Word> m3 = d.match("huang", 4, false);
        p("F. 开关关闭时自动词匹配数 " + m3.size() + "（应为 0）");
        List<UserDict.Word> m4 = d.match("huang", 4, true);
        p("G. 开关开启时自动词匹配数 " + m4.size() + "（应为 1）");
        p("H. usageManual(黄)=" + d.usageManual("黄") + " usage(黄)=" + d.usage("黄"));

        boolean up = d.update("niunai", "牛奶", "niunaigai", "ㄋㄧㄡㄋㄞㄍㄞ", "牛奶盖");
        List<UserDict.Word> m5 = d.match("niunaigai", 4, true);
        p("I. 修改 ok=" + up + " 新码命中 -> " + (m5.isEmpty() ? "-" : m5.get(0).word)
                + " manual=" + (m5.isEmpty() ? "-" : m5.get(0).manual));

        int removed = d.removeAuto();
        p("J. 删自动 " + removed + " 条 -> 剩 size=" + d.size()
                + " auto=" + d.autoCount() + " manual=" + d.manualCount());

        d.save();
        String content = new String(Files.readAllBytes(f.toPath()), "UTF-8");
        String[] lines = content.split("\n");
        p("K. 落盘行数=" + lines.length + " 首行=" + lines[0].replace("\t", "|"));

        Files.copy(f.toPath(), f2.toPath(), StandardCopyOption.REPLACE_EXISTING);
        UserDict r = UserDict.get(f2);
        p("L. 重新加载 size=" + r.size() + " manual=" + r.manualCount()
                + " auto=" + r.autoCount());
        List<UserDict.Word> m6 = r.match("niunaigai", 4, false);
        p("M. 重载后手动词仍生效 -> " + (m6.isEmpty() ? "失败" : m6.get(0).word));

        r.clear();
        p("N. 清空后 size=" + r.size());
    }

    private static void p(String s) {
        System.out.println(s);
    }
}

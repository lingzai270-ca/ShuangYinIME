package com.shuangyin.ime;

import com.shuangyin.ime.core.UserDict;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/** 用户词库「不设上限 + 导出 / 导入合并」的 JVM 自检。 */
public class DictIOTest {

    public static void main(String[] args) throws Exception {
        File dir = new File("build/dictio");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File a = new File(dir, "src.txt");
        File exp = new File(dir, "export.txt");
        File b = new File(dir, "dst.txt");
        for (File f : new File[]{a, exp, b}) {
            if (f.exists()) {
                f.delete();
            }
        }

        // 源词库：1 条手动词 + 3 条自动词
        UserDict src = UserDict.get(a);
        src.addManual("zhonghua", "ㄓㄨㄥㄏㄨㄚ", "中華");
        src.record("zhonghua", "中华");
        src.record("zhonghua", "中华");
        src.record("zhonghua", "种花");
        src.save();
        System.out.println("src size=" + src.size() + " manual=" + src.manualCount()
                + " auto=" + src.autoCount());

        FileOutputStream os = new FileOutputStream(exp);
        src.exportTo(os);
        os.close();
        System.out.println("导出文件 " + exp.length() + " 字节");

        // 目标词库里先放一条同样的词（自动词），验证合并而不是重复
        UserDict dst = UserDict.get(b);
        dst.record("zhonghua", "中华");
        dst.record("zhonghua", "中华");
        dst.record("zhonghua", "中华");
        dst.record("zhonghua", "重要");
        dst.save();
        System.out.println("dst 导入前 size=" + dst.size());

        FileInputStream is = new FileInputStream(exp);
        int[] r = dst.importFrom(is);
        is.close();
        System.out.println("导入结果 新增=" + r[0] + " 更新=" + r[1] + " 跳过=" + r[2]);
        System.out.println("dst 导入后 size=" + dst.size() + " manual=" + dst.manualCount()
                + " auto=" + dst.autoCount());

        // 「中华」应该被合并成 1 条（次数取大者 3），且仍是自动词；手动词「中華」应新增且锁定
        int n = 0;
        boolean sawZhonghua = false;
        boolean sawZhonghuaManual = false;
        for (UserDict.Word w : dst.all()) {
            n++;
            if ("中华".equals(w.word) && "zhonghua".equals(w.code)) {
                sawZhonghua = true;
                System.out.println("中华 count=" + w.count + " manual=" + w.manual);
            }
            if ("中華".equals(w.word)) {
                sawZhonghuaManual = w.manual;
            }
        }
        System.out.println("条目数=" + n + " 有中华=" + sawZhonghua + " 中華锁定=" + sawZhonghuaManual);

        // 再导出一次，确认导出格式能被自己吃回去（幂等：新增 0 条）
        FileOutputStream os2 = new FileOutputStream(new File(dir, "export2.txt"));
        dst.exportTo(os2);
        os2.close();
        UserDict round = UserDict.get(new File(dir, "round.txt"));
        FileInputStream is2 = new FileInputStream(new File(dir, "export2.txt"));
        int[] r2 = round.importFrom(is2);
        is2.close();
        System.out.println("回灌 新增=" + r2[0] + " 更新=" + r2[1] + " 跳过=" + r2[2]
                + " size=" + round.size());

        // 不设上限：塞 5000 条进去，确认一条都没被淘汰
        File big = new File(dir, "big.txt");
        if (big.exists()) {
            big.delete();
        }
        UserDict bigDict = UserDict.get(big);
        for (int i = 0; i < 5000; i++) {
            bigDict.addManual("code" + i, "", "词" + i);
        }
        bigDict.save();
        File reloadFile = new File(dir, "big.txt");
        UserDict reload = UserDict.get(new File(dir, "big2.txt"));
        FileInputStream bis = new FileInputStream(reloadFile);
        int[] rb = reload.importFrom(bis);
        bis.close();
        System.out.println("大库 size=" + bigDict.size() + " 重载后=" + reload.size()
                + " 新增=" + rb[0]);
        System.out.println(rb[0] == 5000 && reload.size() == 5000 ? "OK 无上限" : "FAIL 有上限");
        System.out.println(sawZhonghua && dst.size() == 5 ? "OK 合并" : "FAIL 合并");
    }
}

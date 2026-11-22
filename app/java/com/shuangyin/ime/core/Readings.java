package com.shuangyin.ime.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 单字读音表：候选字上方的小字注释用。
 *
 * 数据文件每行「字<TAB>读音」，拼音表存带调拼音（nǐ），注音表存注音符号（ㄋㄧˇ）。
 * 由 tools/build_readings.py 从词库 + pypinyin 生成，随 APK 一起打进 assets。
 */
public final class Readings {
    private final Map<String, String> pin = new HashMap<String, String>();
    private final Map<String, String> zhy = new HashMap<String, String>();

    /** 从已解压到私有目录的文件加载；缺失时静默跳过，功能退化为不显示小字。 */
    public static Readings load(File dir) {
        Readings r = new Readings();
        r.loadOne(new File(dir, "reading_pin.txt"), r.pin);
        r.loadOne(new File(dir, "reading_zhy.txt"), r.zhy);
        return r;
    }

    private void loadOne(File f, Map<String, String> into) {
        if (f == null || !f.isFile()) return;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(f),
                    StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                int tab = line.indexOf('\t');
                if (tab <= 0 || tab + 1 >= line.length()) continue;
                into.put(line.substring(0, tab), line.substring(tab + 1));
            }
        } catch (IOException ignored) {
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public int size() {
        return pin.size() + zhy.size();
    }

    public String pinyin(String ch) {
        return pin.get(ch);
    }

    public String zhuyin(String ch) {
        return zhy.get(ch);
    }

    /**
     * 取一个词每个字的读音并拼起来；只要有一个字查不到就返回 null。
     * kind：1 拼音，2 注音。
     */
    public String word(String text, int kind) {
        if (text == null || text.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            String r = kind == 2 ? zhy.get(ch) : pin.get(ch);
            if (r == null) {
                return null;
            }
            if (sb.length() > 0 && kind == 1) {
                sb.append(' ');
            }
            sb.append(r);
        }
        return sb.toString();
    }
}

package com.shuangyin.ime.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户词库：记录用户上过屏的词（连同当时的输入编码），也支持手动添加。
 *
 * 三个用途：
 *   1. 提权——候选里出现用户常打的词，分数按使用次数上浮，越常用越靠前；
 *   2. 自造词——用户打出的词若词库没有，下次输入同一编码时直接作为候选给出；
 *   3. 手动词——用户自己添加的词，词库开关关掉后仍然生效（锁定）。
 *
 * 存储：getFilesDir()/userdict.txt
 *   每行「编码 \t 词 \t 次数 \t 最后使用毫秒 \t 手动(0/1) \t 第二编码」，第 5 列缺失按自动词处理（兼容旧文件）。
 * 不设条数上限：只要在长，就一直留着；可以整库导出 / 导入成文本文件。
 */
public final class UserDict {

    public static final class Word {
        public String code;
        /** 同一词的第二个编码（手动词会同时写拼音与注音两套码），可空。 */
        public String code2;
        public String word;
        public int count;
        public long last;
        public boolean manual;
    }

    private static final char SEP = '\u0001';
    private static final Map<String, UserDict> CACHE = new HashMap<String, UserDict>();

    private final File file;
    private final Map<String, Word> map = new HashMap<String, Word>();
    /** 全部词的使用次数。 */
    private final Map<String, Integer> usageAll = new HashMap<String, Integer>();
    /** 仅手动词的使用次数：用户词库开关关闭后，只有这部分还生效。 */
    private final Map<String, Integer> usageManual = new HashMap<String, Integer>();
    private boolean dirty;
    private boolean saving;
    private long version;

    private UserDict(File file) {
        this.file = file;
        load();
    }

    /** 同一文件在进程内共享一个实例（输入法与设置页改的是同一份内存）。 */
    public static synchronized UserDict get(File file) {
        String key = file.getAbsolutePath();
        UserDict d = CACHE.get(key);
        if (d == null) {
            d = new UserDict(file);
            CACHE.put(key, d);
        }
        return d;
    }

    private static String k(String code, String word) {
        return code + SEP + word;
    }

    private void load() {
        if (file == null || !file.isFile()) {
            return;
        }
        BufferedReader r = null;
        try {
            r = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            String line;
            while ((line = r.readLine()) != null) {
                String[] p = line.split("\t");
                if (p.length < 4) {
                    continue;
                }
                Word w = new Word();
                w.code = p[0];
                w.word = p[1];
                try {
                    w.count = Integer.parseInt(p[2]);
                    w.last = Long.parseLong(p[3]);
                } catch (NumberFormatException e) {
                    continue;
                }
                w.manual = p.length >= 5 && "1".equals(p[4]);
                w.code2 = p.length >= 6 ? p[5] : "";
                map.put(k(w.code, w.word), w);
                addUsage(w);
            }
        } catch (Exception e) {
            // 文件损坏就当空库，不影响输入
        } finally {
            close(r);
        }
    }

    private void addUsage(Word w) {
        Integer a = usageAll.get(w.word);
        usageAll.put(w.word, (a == null ? 0 : a) + w.count);
        if (w.manual) {
            Integer m = usageManual.get(w.word);
            usageManual.put(w.word, (m == null ? 0 : m) + w.count);
        }
    }

    private void subUsage(Word w) {
        Integer a = usageAll.get(w.word);
        if (a != null) {
            int left = a - w.count;
            if (left > 0) {
                usageAll.put(w.word, left);
            } else {
                usageAll.remove(w.word);
            }
        }
        if (w.manual) {
            Integer m = usageManual.get(w.word);
            if (m != null) {
                int left = m - w.count;
                if (left > 0) {
                    usageManual.put(w.word, left);
                } else {
                    usageManual.remove(w.word);
                }
            }
        }
    }

    private void close(BufferedReader r) {
        if (r != null) {
            try {
                r.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    /** 整库导出成文本（UTF-8）：首行是注释，其余每行 编码\t词\t次数\t最后使用\t手动\t第二编码。 */
    public synchronized void exportTo(OutputStream os) throws IOException {
        OutputStreamWriter w = new OutputStreamWriter(os, "UTF-8");
        w.write("# shuangyin-userdict v1\n");
        for (Word e : map.values()) {
            w.write(e.code);
            w.write('\t');
            w.write(e.word);
            w.write('\t');
            w.write(Integer.toString(e.count));
            w.write('\t');
            w.write(Long.toString(e.last));
            w.write('\t');
            w.write(e.manual ? "1" : "0");
            w.write('\t');
            w.write(e.code2 == null ? "" : e.code2);
            w.write('\n');
        }
        w.flush();
    }

    /**
     * 从文本导入，按「编码 + 词」合并，不清掉原有条目：
     * 已有的取次数更大的那条，本地是手动词就保持手动，导入是手动词则升级成手动。
     * 返回 {新增, 更新, 跳过}。
     */
    /**
     * 从文件导入词条，支持多种格式，自动识别：
     *   1. 文本：TSV（制表符）/ CSV（逗号、全角逗号、带引号）/ 竖线 / 全角空格 / 空格分隔，
     *      两列以上即可，「编码 + 词」顺序写反也能自动纠正；中文那列当词、字母那列当编码。
     *   2. JSON：数组 [{"code":"ni hao","word":"你好","count":2}]、{"nihao":"你好"}、
     *      或 [{"拼音":"nihao","词":"你好"}] 这类键名都认。
     * 表头行、空行、# 与 // 注释自动跳过。返回 {新增, 合并, 跳过}。
     */
    public synchronized int[] importFrom(InputStream is) throws IOException {
        String text = readText(is);
        int[] c = new int[]{0, 0, 0};
        String t = text.trim();
        boolean done = false;
        if (t.startsWith("[") || t.startsWith("{")) {
            done = importJson(t, c);
        }
        if (!done) importDelimited(text, c);
        if (c[0] > 0 || c[1] > 0) {
            dirty = true;
            version++;
            scheduleSave();
        }
        return c;
    }

    private String readText(InputStream is) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[8192];
        int n;
        while ((n = r.read(buf)) > 0) sb.append(buf, 0, n);
        String s = sb.toString();
        if (s.startsWith("\uFEFF")) s = s.substring(1);
        return s;
    }

    /** 文本格式：按行拆，分隔符自动判断。 */
    private void importDelimited(String text, int[] c) {
        String[] lines = text.split("\r\n|\n|\r");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].replace("\uFEFF", "").trim();
            if (line.length() == 0 || line.startsWith("#") || line.startsWith("//")) continue;
            String low = line.toLowerCase();
            // 表头（含 word/code 或 词/编码）直接跳过，不算错误行
            if ((low.contains("word") || line.contains("词")) &&
                    (low.contains("code") || low.contains("pinyin") || line.contains("编码") || line.contains("拼音"))) {
                continue;
            }
            String[] p = splitRow(line);
            if (p.length < 2) {
                c[2]++;
                continue;
            }
            String a = p[0].trim();
            String b = p[1].trim();
            if (a.length() == 0 || b.length() == 0) {
                c[2]++;
                continue;
            }
            // 写反了：中文前、编码后，自动换回来
            if (hasCjk(a) && !hasCjk(b)) {
                String tmp = a;
                a = b;
                b = tmp;
            }
            int count = 1;
            long last = System.currentTimeMillis();
            boolean manual = false;
            String code2 = "";
            try {
                if (p.length >= 3) count = Integer.parseInt(p[2].trim());
                if (p.length >= 4) last = Long.parseLong(p[3].trim());
            } catch (NumberFormatException e) {
                // 数字列写坏了就按 1 次、当前时间收下，词本身不丢
            }
            if (p.length >= 5) manual = "1".equals(p[4].trim());
            if (p.length >= 6) code2 = p[5].trim();
            if (count < 1) count = 1;
            putEntry(a, b, count, last, manual, code2, c);
        }
    }

    private String[] splitRow(String line) {
        if (line.indexOf('\t') >= 0) return line.split("\t", -1);
        if (line.indexOf(',') >= 0 || line.indexOf('，') >= 0) return splitCsv(line);
        if (line.indexOf('|') >= 0) return line.split("\\|", -1);
        if (line.indexOf('\u3000') >= 0) return line.split("\u3000", -1);
        String[] w = line.split("\\s+");
        if (w.length >= 3 && hasCjk(w[w.length - 1])) {
            return new String[]{join(w, 0, w.length - 1), w[w.length - 1]};
        }
        if (w.length >= 3 && hasCjk(w[0])) {
            return new String[]{join(w, 1, w.length), w[0]};
        }
        return w;
    }

    private String join(String[] arr, int from, int to) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < to; i++) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    /** CSV：认双引号里的逗号，全角逗号当分隔符。 */
    private String[] splitCsv(String line) {
        List<String> out = new ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        boolean quote = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                quote = !quote;
                continue;
            }
            boolean sep = !quote && (ch == ',' || ch == '，');
            if (sep) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    private static boolean hasCjk(String s) {
        if (s == null) return false;
        int i = 0;
        while (i < s.length()) {
            int cp = s.codePointAt(i);
            Character.UnicodeScript sc = Character.UnicodeScript.of(cp);
            if (sc == Character.UnicodeScript.HAN) return true;
            i += Character.charCount(cp);
        }
        return false;
    }

    private static String optStr(org.json.JSONObject o, String... keys) {
        for (int i = 0; i < keys.length; i++) {
            if (o.has(keys[i]) && !o.isNull(keys[i])) {
                String v = o.optString(keys[i], "").trim();
                if (v.length() > 0) return v;
            }
        }
        return null;
    }

    /** JSON：数组 / 对象都认，返回是否确实解析过（解析失败就回退按文本处理）。 */
    private boolean importJson(String t, int[] c) {
        try {
            long now = System.currentTimeMillis();
            if (t.startsWith("[")) {
                org.json.JSONArray arr = new org.json.JSONArray(t);
                for (int i = 0; i < arr.length(); i++) {
                    Object o = arr.get(i);
                    if (o instanceof org.json.JSONObject) {
                        org.json.JSONObject jo = (org.json.JSONObject) o;
                        String word = optStr(jo, "word", "词", "text", "w", "name");
                        String code = optStr(jo, "code", "pinyin", "拼音", "py", "code1");
                        String code2 = optStr(jo, "code2", "zhuyin", "注音", "roman");
                        if (word == null || code == null) {
                            c[2]++;
                            continue;
                        }
                        int count = jo.optInt("count", 1);
                        boolean manual = jo.optBoolean("manual", true);
                        putEntry(code, word, Math.max(1, count), now, manual,
                                code2 == null ? "" : code2, c);
                    } else {
                        String[] p = splitRow(String.valueOf(o));
                        if (p.length < 2) {
                            c[2]++;
                        } else if (hasCjk(p[0]) && !hasCjk(p[1])) {
                            putEntry(p[1].trim(), p[0].trim(), 1, now, true, "", c);
                        } else {
                            putEntry(p[0].trim(), p[1].trim(), 1, now, true, "", c);
                        }
                    }
                }
                return true;
            }
            org.json.JSONObject obj = new org.json.JSONObject(t);
            java.util.Iterator<String> it = obj.keys();
            int n = 0;
            while (it.hasNext()) {
                String key = it.next();
                Object val = obj.get(key);
                if (val instanceof org.json.JSONArray) {
                    org.json.JSONArray words = (org.json.JSONArray) val;
                    for (int j = 0; j < words.length(); j++) {
                        String word = String.valueOf(words.get(j)).trim();
                        if (word.length() > 0) putEntry(key, word, 1, now, true, "", c);
                    }
                } else if (val instanceof String) {
                    String word = ((String) val).trim();
                    if (word.length() > 0) putEntry(key, word, 1, now, true, "", c);
                }
                n++;
            }
            return n > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /** 一条词写进词库：1=新增，2=合并，0=跳过（同时累加进 c）。 */
    private void putEntry(String code, String word, int count, long last, boolean manual,
                          String code2, int[] c) {
        code = code == null ? "" : code.trim();
        word = word == null ? "" : word.trim();
        if (code.length() == 0 || word.length() == 0) {
            c[2]++;
            return;
        }
        if (count < 1) count = 1;
        Word w = map.get(k(code, word));
        if (w == null) {
            w = new Word();
            w.code = code;
            w.code2 = code2 == null ? "" : code2;
            w.word = word;
            w.count = count;
            w.last = last;
            w.manual = manual;
            map.put(k(code, word), w);
            addUsage(w);
            c[0]++;
        } else {
            subUsage(w);
            if (count > w.count) w.count = count;
            if (last > w.last) w.last = last;
            if (w.code2 == null || w.code2.isEmpty()) w.code2 = code2 == null ? "" : code2;
            if (manual) w.manual = true;
            addUsage(w);
            c[1]++;
        }
    }

    public synchronized int size() {
        return map.size();
    }

    public synchronized int autoCount() {
        int n = 0;
        for (Word w : map.values()) {
            if (!w.manual) {
                n++;
            }
        }
        return n;
    }

    public synchronized int manualCount() {
        int n = 0;
        for (Word w : map.values()) {
            if (w.manual) {
                n++;
            }
        }
        return n;
    }

    public synchronized long version() {
        return version;
    }

    /** 这个词被上屏过多少次（跨编码累计）。 */
    public synchronized int usage(String word) {
        Integer u = usageAll.get(word);
        return u == null ? 0 : u;
    }

    /** 只看手动词的使用次数。 */
    public synchronized int usageManual(String word) {
        Integer u = usageManual.get(word);
        return u == null ? 0 : u;
    }

    /** 该编码下用户自己打出来的词，按次数取前几条；includeAuto=false 时只给手动词。 */
    public synchronized List<Word> match(String code, int limit, boolean includeAuto) {
        List<Word> out = new ArrayList<Word>();
        if (code == null || code.isEmpty()) {
            return out;
        }
        for (Word w : map.values()) {
            if (!code.equals(w.code) && !code.equals(w.code2)) {
                continue;
            }
            if (!includeAuto && !w.manual) {
                continue;
            }
            out.add(w);
        }
        Collections.sort(out, new Comparator<Word>() {
            @Override
            public int compare(Word a, Word b) {
                if (a.manual != b.manual) {
                    return a.manual ? -1 : 1;      // 手动词排前
                }
                if (a.count != b.count) {
                    return a.count < b.count ? 1 : -1;
                }
                return a.last < b.last ? 1 : (a.last == b.last ? 0 : -1);
            }
        });
        if (out.size() > limit) {
            return new ArrayList<Word>(out.subList(0, limit));
        }
        return out;
    }

    /** 记录一次上屏（自动学习）。 */
    public synchronized void record(String code, String word) {
        if (code == null || word == null || code.isEmpty() || word.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        Word w = map.get(k(code, word));
        if (w == null) {
            w = new Word();
            w.code = code;
            w.word = word;
            w.count = 1;
            w.last = now;
            w.manual = false;
            map.put(k(code, word), w);
        } else {
            w.count++;
            w.last = now;
        }
        addUsageOne(1, w);
        dirty = true;
        version++;
        scheduleSave();
    }

    private void addUsageOne(int delta, Word w) {
        Integer a = usageAll.get(w.word);
        usageAll.put(w.word, (a == null ? 0 : a) + delta);
        if (w.manual) {
            Integer m = usageManual.get(w.word);
            usageManual.put(w.word, (m == null ? 0 : m) + delta);
        }
    }

    /** 手动添加词条；同一个编码下已有该词则升级为手动并锁定。 */
    public synchronized boolean addManual(String code, String code2, String word) {
        if (code == null || word == null || code.isEmpty() || word.isEmpty()) {
            return false;
        }
        String key = k(code, word);
        Word w = map.get(key);
        if (w != null) {
            if (w.manual) {
                return false;
            }
            subUsage(w);
            w.manual = true;
            w.code2 = code2 == null ? "" : code2;
            addUsage(w);
            dirty = true;
            version++;
            scheduleSave();
            return true;
        }
        w = new Word();
        w.code = code;
        w.code2 = code2 == null ? "" : code2;
        w.word = word;
        w.count = 1;
        w.last = System.currentTimeMillis();
        w.manual = true;
        map.put(key, w);
        addUsage(w);
        dirty = true;
        version++;
        scheduleSave();
        return true;
    }

    /** 修改词条：词本身和编码都可以改。 */
    public synchronized boolean update(String oldCode, String oldWord, String newCode, String newCode2, String newWord) {
        if (oldCode == null || oldWord == null || newCode == null || newWord == null) {
            return false;
        }
        if (newCode.isEmpty() || newWord.isEmpty()) {
            return false;
        }
        Word w = map.get(k(oldCode, oldWord));
        if (w == null) {
            return false;
        }
        String targetKey = k(newCode, newWord);
        if (!targetKey.equals(k(oldCode, oldWord)) && map.containsKey(targetKey)) {
            return false;      // 目标已存在
        }
        subUsage(w);
        map.remove(k(oldCode, oldWord));
        w.code = newCode;
        w.code2 = newCode2 == null ? "" : newCode2;
        w.word = newWord;
        w.last = System.currentTimeMillis();
        map.put(targetKey, w);
        addUsage(w);
        dirty = true;
        version++;
        scheduleSave();
        return true;
    }

    public synchronized boolean remove(String code, String word) {
        Word w = map.remove(k(code, word));
        if (w == null) {
            return false;
        }
        subUsage(w);
        dirty = true;
        version++;
        scheduleSave();
        return true;
    }

    /** 删除全部自动录入的词，手动添加的保留。返回删掉的条数。 */
    public synchronized int removeAuto() {
        List<String> keys = new ArrayList<String>();
        for (Map.Entry<String, Word> e : map.entrySet()) {
            if (!e.getValue().manual) {
                keys.add(e.getKey());
            }
        }
        for (int i = 0; i < keys.size(); i++) {
            Word w = map.remove(keys.get(i));
            if (w != null) {
                subUsage(w);
            }
        }
        if (!keys.isEmpty()) {
            dirty = true;
            version++;
            scheduleSave();
        }
        return keys.size();
    }

    public synchronized void clear() {
        map.clear();
        usageAll.clear();
        usageManual.clear();
        dirty = true;
        version++;
        scheduleSave();
    }

    /** 全部条目，手动词在前，再按次数从多到少。 */
    public synchronized List<Word> all() {
        List<Word> out = new ArrayList<Word>(map.values());
        Collections.sort(out, new Comparator<Word>() {
            @Override
            public int compare(Word a, Word b) {
                if (a.manual != b.manual) {
                    return a.manual ? -1 : 1;
                }
                if (a.count != b.count) {
                    return a.count < b.count ? 1 : -1;
                }
                return a.last < b.last ? 1 : (a.last == b.last ? 0 : -1);
            }
        });
        return out;
    }

    /** 合并写盘：上屏很频繁，延迟 1.5 秒落一次，避免每次按键都动文件。 */
    private void scheduleSave() {
        if (saving) {
            return;
        }
        saving = true;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    // ignore
                }
                saving = false;
                save();
            }
        }, "userdict-save");
        t.setDaemon(true);
        t.start();
    }

    public synchronized void save() {
        if (!dirty || file == null) {
            return;
        }
        dirty = false;
        OutputStreamWriter w = null;
        try {
            File dir = file.getParentFile();
            if (dir != null && !dir.exists()) {
                dir.mkdirs();
            }
            File tmp = new File(file.getAbsolutePath() + ".tmp");
            w = new OutputStreamWriter(new FileOutputStream(tmp), "UTF-8");
            for (Word e : map.values()) {
                w.write(e.code);
                w.write('\t');
                w.write(e.word);
                w.write('\t');
                w.write(Integer.toString(e.count));
                w.write('\t');
                w.write(Long.toString(e.last));
                w.write('\t');
                w.write(e.manual ? '1' : '0');
                w.write('\t');
                w.write(e.code2 == null ? "" : e.code2);
                w.write('\n');
            }
            w.flush();
            w.close();
            w = null;
            if (file.exists()) {
                file.delete();
            }
            if (!tmp.renameTo(file)) {
                // 落盘失败，保留 tmp，下次再试
            }
        } catch (Exception e) {
            dirty = true;
        } finally {
            if (w != null) {
                try {
                    w.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }
}

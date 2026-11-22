package com.shuangyin.ime;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import com.shuangyin.ime.core.BinDict;
import com.shuangyin.ime.core.Candidate;
import com.shuangyin.ime.core.JapaneseEngine;
import com.shuangyin.ime.core.PinyinEngine;
import com.shuangyin.ime.core.Readings;
import com.shuangyin.ime.core.StrokeEngine;
import com.shuangyin.ime.core.TimeCand;
import com.shuangyin.ime.core.UserDict;
import com.shuangyin.ime.core.ZhuyinEngine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 霜音输入法主服务：全拼 / 注音 / 日语 三方案。 */
public class FrostIME extends android.inputmethodservice.InputMethodService implements ImeView.Listener {

    private static final int SCHEME_PINYIN = 0;
    private static final int SCHEME_ZHUYIN = 1;
    private static final int SCHEME_JAPANESE = 2;
    /** 笔画只是临时输入模式，不落盘，退出即回拼音。 */
    private static final int SCHEME_STROKE = 3;

    private static final String[] SCHEME_NAMES = {"已切换：拼音", "已切换：注音", "已切换：日本語"};

    private final Handler handler = new Handler(Looper.getMainLooper());

    private ImeView view;
    private int scheme = SCHEME_PINYIN;
    private boolean english;
    /** 英文大写锁定：长按 123 键切换，切到小写才恢复。 */
    private boolean englishUpper;
    private boolean symbolPage;
    /** 符号页当前在第几屏（0 常用 / 1 数学箭头 / 2 序号与进阶符号），左右滑动切换。 */
    private int symPage;
    /** 数字键盘页：短按 123 进来，只出数字与标点，不参与联想/组词。 */
    private boolean numberPage;
    /** 日语假名键盘的「小假名」开关：开了之后 あ 行 / や 行 / わ 行 / つ 出小写假名。 */
    private boolean kanaSmall;
    private String buffer = "";
    /** 组字串内部光标下标（0..buffer.length()）；只在输入法缓冲里移动，不回写编辑器选区。 */
    private int caret = 0;
    /** 光标编辑是否激活：默认关，点字条上的拼音才打开；关着时字母照旧追加到串尾。 */
    private boolean caretActive = false;
    /** 展开面板筛选：true 只出单字（按字频排），false 词优先。 */
    private boolean singleOnly;
    private List<Candidate> candidates = new ArrayList<Candidate>();
    /** 本轮候选里补进来的「时间 / 日期」串，上屏时不要当新词学进用户词库。 */
    private final List<String> timeDateWords = new ArrayList<String>();
    private boolean timeCand;

    // 日语状态：jpKana 为已转换的平假名，jpRomaji 为等待下一键的罗马字尾巴
    private String jpKana = "";
    private String jpRomaji = "";
    private boolean katakana;
    private boolean kanaLayout;

    private PinyinEngine pinyinEngine;
    private Set<String> syllableSet;
    private ZhuyinEngine zhuyinEngine;
    private JapaneseEngine japaneseEngine;
    private StrokeEngine strokeEngine;
    /** 笔画模式：只从符号页进入，退出必回拼音。 */
    private boolean strokeMode;
    private String strokeBuffer = "";
    private volatile boolean dictReady;
    private volatile String dictError;
    private volatile int loadPercent;

    private int candidateLimit = 9;
    private boolean fuzzy = true;
    private boolean abbr = true;
    private boolean vibrate = true;
    private int catMask = Prefs.CAT_ALL;
    private boolean assoc = true;

    // 剪贴板
    private final ArrayList<String> clips = new ArrayList<String>();
    private boolean clipPanel;

    /** 撤回栈：只记「粘贴进来的文字」和「被删掉的文字」，最多 30 步。 */
    private static final class UndoEntry {
        final boolean insert;   // true=插入（撤回时删掉），false=删除（撤回时补回）
        final String text;
        UndoEntry(boolean insert, String text) {
            this.insert = insert;
            this.text = text == null ? "" : text;
        }
    }

    private final java.util.ArrayList<UndoEntry> undoStack = new java.util.ArrayList<UndoEntry>();

    private void pushUndo(boolean insert, String text) {
        if (text == null || text.isEmpty()) return;
        undoStack.add(new UndoEntry(insert, text));
        while (undoStack.size() > 30) undoStack.remove(0);
    }

    @Override
    public void onUndo() {
        if (undoStack.isEmpty()) {
            if (view != null) view.flash("没有可撤回的操作");
            return;
        }
        UndoEntry e = undoStack.remove(undoStack.size() - 1);
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            if (view != null) view.flash("撤回失败：输入框不可用");
            return;
        }
        if (e.insert) {
            ic.deleteSurroundingText(e.text.length(), 0);
            if (view != null) view.flash("已撤回粘贴");
        } else {
            ic.commitText(e.text, 1);
            if (view != null) view.flash("已撤回删除");
        }
    }
    private String lastCommit = "";

    // 单字模式连打：本轮攒下来的字与各字拼音，整串打完才整体记一笔（黄 + 牛 → 黄牛 / huangniu）
    private String singleChain = "";
    private String singleChainKey = "";

    // 联想词
    private BinDict pinyinDict;
    private boolean assocActive;

    // 候选字读音小字
    private Readings readings;
    private boolean showReading = true;

    // 用户词库
    private UserDict userDict;
    private boolean userDictOn = true;

    // 按键视觉反馈（按下字母 / 假名 / 注音键高亮 5 秒）
    private boolean visualFeedback = true;
    // 键盘高度百分比（70–150）
    /** 候选上方小字百分比（100 起）。 */
    private int readScale = 100;
    /** 键面右下角小字百分比（100 起）。 */
    private int hintScale = 100;
    /** 机械键盘回弹开关（独立于按下蓝底）。 */
    private boolean keyBounce;
    /** 声调筛选：0 不过滤，1–4 只看这个调值。 */
    private int toneFilter;
    private int keyHeight = Prefs.HEIGHT_DEFAULT;
    // 输入法开关（设置页可关）：关掉就不进方案循环，相关按键隐藏
    private boolean pinyinOn = true;
    private boolean zhuyinOn = true;
    private boolean strokeOn = true;
    private boolean japaneseOn = true;
    // 分区底色 / 分区边框（注音固定大千式，边框按区域整块描）
    private boolean zyTint = true;
    private boolean zyBorder = true;
    // 假名罗马音、注音拼音小字颜色
    private int romajiColor = 0xFF8FA3B3;
    private int hintColor = 0xFFC62828;
    // 假名键上方罗马音
    private boolean kanaRomaji = true;

    @Override
    public void onCreate() {
        super.onCreate();
        Prefs.migrate(this);
        reloadPrefs();
        scheme = Prefs.scheme(this);
        // 设置页一改就立刻生效，不用等下次弹出键盘（注音布局这类切换才能马上看到）
        Prefs.get(this).registerOnSharedPreferenceChangeListener(prefListener);
        loadDictionaries();
    }

    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener prefListener =
            new android.content.SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(android.content.SharedPreferences sp, String key) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            reloadPrefs();
                            enforceSchemeEnabled();
                            applyKeyboard();
                            updateCandidates();
                        }
                    });
                }
            };

    @Override
    public void onDestroy() {
        try {
            Prefs.get(this).unregisterOnSharedPreferenceChangeListener(prefListener);
        } catch (RuntimeException ignored) {
        }
        super.onDestroy();
    }

    private void reloadPrefs() {
        candidateLimit = Prefs.count(this);
        fuzzy = Prefs.fuzzy(this);
        abbr = Prefs.abbr(this);
        vibrate = Prefs.vibrate(this);
        katakana = Prefs.katakana(this);
        kanaLayout = Prefs.kanaLayout(this);
        catMask = Prefs.catMask(this);
        assoc = Prefs.assoc(this);
        showReading = Prefs.reading(this);
        userDictOn = Prefs.userDict(this);
        visualFeedback = Prefs.visual(this);
        keyHeight = Prefs.height(this);
        kanaRomaji = Prefs.kanaRomaji(this);
        zyTint = Prefs.zhuyinTint(this);
        zyBorder = Prefs.zhuyinBorder(this);
        romajiColor = Prefs.romajiColor(this);
        hintColor = Prefs.hintColor(this);
        timeCand = Prefs.timeCandidate(this);
        pinyinOn = Prefs.pinyinOn(this);
        zhuyinOn = Prefs.zhuyinOn(this);
        strokeOn = Prefs.strokeOn(this);
        japaneseOn = Prefs.japaneseOn(this);
        singleOnly = Prefs.singleOnly(this);
        readScale = Prefs.readScale(this);
        hintScale = Prefs.hintScale(this);
        keyBounce = Prefs.keyBounce(this);
        applyMasks();
        // 设置页一改（包括「恢复默认设置」）就立刻落到当前键盘上，不用等下次弹键盘
        if (view != null) {
            view.setReadScale(readScale / 100f);
            view.setHintScale(hintScale / 100f);
        view.setKeyBounce(keyBounce);
            view.setKeyBounce(keyBounce);
        }
    }

    /** 当前方案被关掉时自动落到第一个开着的方案上；笔画模式跟着开关走。 */
    private void enforceSchemeEnabled() {
        if (!Prefs.schemeEnabled(this, scheme)) {
            scheme = Prefs.firstEnabledScheme(this);
            Prefs.get(this).edit().putInt(Prefs.KEY_SCHEME, scheme).apply();
            buffer = "";
            jpKana = "";
            jpRomaji = "";
        }
        if (strokeMode && !strokeOn) {
            strokeMode = false;
            strokeBuffer = "";
        }
    }

    private void applyMasks() {
        if (pinyinEngine != null) pinyinEngine.setCatMask(catMask);
        if (zhuyinEngine != null) zhuyinEngine.setCatMask(catMask);
        if (japaneseEngine != null) japaneseEngine.setCatMask(catMask);
        if (strokeEngine != null) strokeEngine.setCatMask(catMask);
    }

    // ---------------- 用户词库 ----------------

    private void initUserDict() {
        if (userDict == null) {
            userDict = UserDict.get(new File(getFilesDir(), "userdict.txt"));
        }
    }

    private static boolean isCjk(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0x4E00 && c <= 0x9FFF) return true;
            if (c >= 0x3400 && c <= 0x4DBF) return true;
            if (c >= 0x3040 && c <= 0x30FF) return true;
        }
        return false;
    }

    /** 上屏一个词就记一笔，下次同样的编码它会靠前甚至直接出现。 */
    private void learnWord(String code, String word) {
        if (!userDictOn || word == null || word.isEmpty()) return;
        if (code == null || code.isEmpty()) return;
        if (!isCjk(word)) return;
        initUserDict();
        userDict.record(code, word);
    }

    /**
     * 单字模式连打收尾：把这轮挨个点出来的字当成一个整词记一笔。
     * 只点了一个字就是单字词，点了「黄 + 牛」就记「黄牛 / huangniu」，
     * 不再把每个字按各自的单音节拆成好几条塞进用户词库。
     */
    private void flushSingleChain() {
        if (singleChain.isEmpty()) return;
        if (userDictOn) learnWord(singleChainKey, singleChain);
        singleChain = "";
        singleChainKey = "";
    }

    /**
     * 用户词库参与候选排序：
     *   1. 候选里出现用户打过的词，按次数提权（最多 2.2 倍）；
     *   2. 这个编码下用户打出过词库没有的词，直接插到最前面；
     *   3. 开关关掉时只让手动词（锁定词）参与，自动录入的词一律不用。
     */
    /** 打出「时间 / 日期」这类词时，把当前的系统时间、日期塞到候选最前面（汉字候选照旧保留）。 */
    private void applyTimeDate(List<Candidate> list, String code) {
        timeDateWords.clear();
        if (list == null || !timeCand) return;
        long now = System.currentTimeMillis();
        // 快捷码：输入串跟码完全相等才出（rq 日期 / sj 时间 / xq 星期 / tqq 晴 / tqy 雨天 /
        // tqby 暴雨 / tqxx 小雪 / tqyy 阴云），半截（tqx、tq）一律不出；命中就放最前面，空格直接上屏
        if (code != null && !code.isEmpty()) {
            String hit = TimeCand.forCode(code, now);
            if (hit != null) {
                list.add(0, new Candidate(hit, 30000, "", 30000));
                timeDateWords.add(hit);
                return;
            }
        }
        if (list.isEmpty()) return;
        int scan = Math.min(list.size(), 80);
        boolean time = false;
        boolean date = false;
        for (int i = 0; i < scan && !(time && date); i++) {
            String w = list.get(i).word;
            if (w == null) continue;
            if (!time && TimeCand.isTimeWord(w)) {
                time = true;
            } else if (!date && TimeCand.isDateWord(w)) {
                date = true;
            }
        }
        if (!time && !date) return;
        // 插在第一条汉字的后面：既能一眼看到、点一下就上屏，又不会把空格默认上屏的汉字顶掉
        int at = list.size() >= 1 ? 1 : 0;
        if (time) {
            String t = TimeCand.time(now);
            list.add(at, new Candidate(t, 9000, "", 9000));
            timeDateWords.add(t);
            at++;
        }
        if (date) {
            String d = TimeCand.date(now);
            list.add(at, new Candidate(d, 9000, "", 9000));
            timeDateWords.add(d);
        }
    }

    private void applyUserDict(List<Candidate> list, String code) {
        if (list == null) {
            return;
        }
        initUserDict();
        boolean learning = userDictOn;
        String c = code == null ? "" : code;
        for (int i = 0; i < list.size(); i++) {
            Candidate cd = list.get(i);
            int n = learning ? userDict.usage(cd.word) : userDict.usageManual(cd.word);
            if (n > 0) {
                cd.score *= 1f + Math.min(1.2f, n * 0.15f);
            }
        }
        if (c.isEmpty()) {
            return;
        }
        List<UserDict.Word> own = userDict.match(c, 4, learning);
        if (own.isEmpty()) {
            return;
        }
        float top = 0;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).score > top) top = list.get(i).score;
        }
        for (int i = 0; i < own.size(); i++) {
            UserDict.Word uw = own.get(i);
            boolean exists = false;
            for (int j = 0; j < list.size(); j++) {
                if (list.get(j).word.equals(uw.word)) {
                    exists = true;
                    break;
                }
            }
            if (exists) continue;
            float boost = uw.manual ? 1.8f : 1.35f;   // 手动词更靠前
            list.add(new Candidate(uw.word, 1200 + uw.count, "", top * boost + uw.count));
        }
        Collections.sort(list, new Comparator<Candidate>() {
            @Override
            public int compare(Candidate a, Candidate b) {
                if (a.score != b.score) return a.score < b.score ? 1 : -1;
                return a.weight < b.weight ? 1 : (a.weight == b.weight ? 0 : -1);
            }
        });
    }

    @Override
    public android.view.View onCreateInputView() {
        if (view == null) {
            view = new ImeView(this);
            view.setListener(this);
        view.setReadScale(readScale / 100f);
        view.setHintScale(hintScale / 100f);
        view.setToneFilter(toneFilter);
        }
        applyKeyboard();
        if (!dictReady) {
            view.setStatus(dictError != null ? dictError : "词库加载中 " + loadPercent + "%");
        } else {
            updateCandidates();
        }
        return view;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        reloadPrefs();
        toneFilter = 0;
        undoStack.clear();
        if (view != null) view.setToneFilter(0);
        scheme = Prefs.scheme(this);
        enforceSchemeEnabled();
        english = false;
        symbolPage = false;
        symPage = 0;
        numberPage = false;
        kanaSmall = false;
        clipPanel = false;
        buffer = "";
        caret = 0;
        caretActive = false;
        jpKana = "";
        jpRomaji = "";
        assocActive = false;
        candidates = new ArrayList<Candidate>();
        // 1.0.50 起不再在每次弹出键盘时读系统剪贴板：只有点开剪贴板面板 / 按「粘贴」时才读
        applyKeyboard();
        if (dictReady) {
            updateCandidates();
        } else if (view != null) {
            view.setStatus(dictError != null ? dictError : "词库加载中 " + loadPercent + "%");
        }
    }

    @Override
    public void onFinishInput() {
        commitComposingRaw();
        super.onFinishInput();
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return false;
    }

    private void applyKeyboard() {
        if (view == null) return;
        Keyboard kb;
        if (numberPage) {
            kb = Keyboard.numbers(modeLabel());
        } else if (symbolPage) {
            kb = Keyboard.symbols(strokeOn, symPage);
        } else if (english) {
            kb = Keyboard.pinyin(true, englishUpper);
        } else if (strokeMode) {
            kb = Keyboard.stroke();
        } else if (scheme == SCHEME_ZHUYIN) {
            // 注音固定用大千式（四行各 10~11 键），不再提供倚天 / 精业 / IBM 与分区键盘
            kb = Keyboard.zhuyin();
        } else if (scheme == SCHEME_JAPANESE) {
            kb = (kanaLayout && japaneseOn) ? Keyboard.jpKana(kanaSmall) : Keyboard.jpRomaji();
        } else {
            kb = Keyboard.pinyin(false);
        }
        boolean single = Prefs.cycleSchemeCount(this) <= 1;
        if (!pageMode() && !strokeMode) {
            if (english) {
                // 英文键盘：左下「拼注日」换成「，。」，原来的「，。」换成 ⇧ 上档键（多模式也一样）
                kb.fillHiddenSchemeSlot(0, kanaLayout);
            } else if (scheme == SCHEME_ZHUYIN) {
                // 注音：单模式没有独立空格键（底部宽键「一声」，长按出空格）；
                // 多模式底行不动（仍留「拼注日」），中间空格同样换成「一声」
                if (single) {
                    kb.zhuyinSingle();
                } else {
                    kb.zhuyinMulti();
                }
            } else if (scheme == SCHEME_JAPANESE) {
                // 单模式日语：开了假名键盘才给「日語」键（点一下循环 羅 / あ / ア），否则还是「、。」
                if (single) {
                    kb.fillHiddenSchemeSlot(2, kanaLayout);
                }
            } else if (single) {
                // 单模式拼音：左下补「，。」，第三行最左换成 ⇧
                kb.fillHiddenSchemeSlot(0, kanaLayout);
            }
        }
        view.setMode(strokeMode && !english && !pageMode() ? SCHEME_STROKE : scheme, english);
        view.setEnglishUpper(englishUpper);
        view.setReading(readings, showReading);
        view.setSyllables(syllableSet);
        view.setZyReadMode(Prefs.zyReadMode(this));
        view.setVisualFeedback(visualFeedback);
        view.setKeyScale(heightOfCurrent() / 100f);
        view.setKanaRomaji(kanaRomaji);
        view.setZoneStyle(zyTint, zyBorder);
        view.setZoneColors(
                new int[]{Prefs.zyTint(this, 0), Prefs.zyTint(this, 1),
                        Prefs.zyTint(this, 2), Prefs.zyTint(this, 3)},
                new int[]{Prefs.zyBorder(this, 0), Prefs.zyBorder(this, 1),
                        Prefs.zyBorder(this, 2)});
        view.setRomajiColor(romajiColor);
        view.setHighlightColor(Prefs.highlightColor(this));
        view.setStrokeReadColor(Prefs.strokeReadingColor(this));
        view.setSelectedColors(Prefs.selectedCandidateBg(this), Prefs.selectedCandidateFg(this));
        view.setHintColor(hintColor);
        view.setJpMode(kanaLayout ? (katakana ? 2 : 1) : 0);
        view.setSmallKana(kanaSmall);
        view.setExpanded(false);
        view.setKeyboard(kb);
    }

    /** 符号页 / 数字页都算「页面模式」，这些页里不组词、不联想。 */
    private boolean pageMode() {
        return symbolPage || numberPage;
    }

    /** 拼音 / 注音 / 日语里只开了一个时，底行不需要模式切换键（空位由 Keyboard 补键）。 */
    private boolean keyboardHidesScheme() {
        int n = 0;
        if (pinyinOn) n++;
        if (zhuyinOn) n++;
        if (japaneseOn) n++;
        return n <= 1;
    }

    /** 进入数字页之前那个模式的名字，显示在数字键盘底行第一个键上（按一下回这个模式）。 */
    private String modeLabel() {
        if (english) return englishUpper ? "ABC" : "abc";
        if (strokeMode) return "笔画";
        if (scheme == SCHEME_JAPANESE) return "日語";
        if (scheme == SCHEME_ZHUYIN) return "注音";
        return "拼音";
    }

    /** 当前这块键盘自己的高度百分比：拼音 / 注音 / 日语 / 笔画各存各的。 */
    private int heightOfCurrent() {
        if (pageMode()) return Prefs.heightOf(this, 0);
        if (strokeMode && !english) return Prefs.heightOf(this, 3);
        if (english) return Prefs.heightOf(this, 0);
        if (scheme == SCHEME_ZHUYIN) return Prefs.heightOf(this, 1);
        if (scheme == SCHEME_JAPANESE) return Prefs.heightOf(this, 2);
        return Prefs.heightOf(this, 0);
    }

    private void commitComposingRaw() {
        flushSingleChain();
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.finishComposingText();
        }
        buffer = "";
        jpKana = "";
        jpRomaji = "";
        clipPanel = false;
        assocActive = false;
        candidates = new ArrayList<Candidate>();
    }

    private String jpComposingText() {
        return JapaneseEngine.convertScript(jpKana, katakana) + jpRomaji;
    }

    private void updateCandidates() {
        InputConnection ic = getCurrentInputConnection();
        if (view == null) return;
        view.setClipMode(clipPanel);

        if (clipPanel) {
            List<Candidate> list = new ArrayList<Candidate>();
            for (int i = 0; i < clips.size() && i < 15; i++) {
                list.add(new Candidate(clips.get(i), 1000 - i, "", 1000 - i));
            }
            candidates = list;
            view.setCandidates(list, "");
            view.setExpanded(true);
            if (ic != null) ic.finishComposingText();
            return;
        }

        // 数字键盘：边打边算，候选栏只放一条「1+1=2」，点一下整条上屏
        if (numberPage) {
            List<Candidate> list = new ArrayList<Candidate>();
            Candidate calc = calcPreview();
            if (calc != null) list.add(calc);
            candidates = list;
            view.setCandidates(list, "");
            if (ic != null) ic.finishComposingText();
            return;
        }

        if (assocActive && buffer.isEmpty() && !candidates.isEmpty()) {
            view.setCandidates(candidates, "");
            if (ic != null) ic.finishComposingText();
            return;
        }

        if (strokeMode && !english && !pageMode()) {
            String composing = strokeHint();
            if (strokeBuffer.isEmpty()) {
                candidates = new ArrayList<Candidate>();
                view.setCandidates(candidates, "");
                if (ic != null) {
                    ic.setComposingText("", 1);
                    ic.finishComposingText();
                }
                return;
            }
            if (!dictReady) {
                view.setStatus(dictError != null ? dictError : "词库加载中 " + loadPercent + "%");
                return;
            }
            List<Candidate> list;
            String code = strokeBuffer;
            if (strokeEngine == null || code.replace("|", "").isEmpty()) {
                list = Collections.<Candidate>emptyList();
            } else {
                list = strokeEngine.query(code, candidateLimit);
            }
            candidates = new ArrayList<Candidate>(list);
            applyUserDict(candidates, code);
            applyTimeDate(candidates, null);
            view.setCandidates(candidates, composing);
            if (ic != null) ic.setComposingText(composing, 1);
            return;
        }

        if (scheme == SCHEME_JAPANESE && !english && !pageMode()) {
            String composing = jpComposingText();
            if (jpKana.isEmpty()) {
                candidates = new ArrayList<Candidate>();
                view.setCandidates(candidates, composing);
                if (ic != null) {
                    if (composing.isEmpty()) {
                        ic.setComposingText("", 1);
                        ic.finishComposingText();
                    } else {
                        ic.setComposingText(composing, 1);
                    }
                }
                return;
            }
            if (!dictReady) {
                view.setStatus(dictError != null ? dictError : "词库加载中 " + loadPercent + "%");
                return;
            }
            List<Candidate> list = japaneseEngine == null ? Collections.<Candidate>emptyList()
                    : japaneseEngine.query(jpKana, katakana, candidateLimit);
            candidates = new ArrayList<Candidate>(list);
            applyUserDict(candidates, jpKana);
            view.setCandidates(candidates, composing);
            if (ic != null) ic.setComposingText(composing, 1);
            return;
        }

        if (buffer.isEmpty()) {
            candidates = new ArrayList<Candidate>();
            view.setCandidates(candidates, "");
            // 先把组字区清空再结束组字，否则最后剩下的那一个字母会被当成正文留下来
            if (ic != null) {
                ic.setComposingText("", 1);
                ic.finishComposingText();
            }
            return;
        }
        if (!dictReady) {
            view.setStatus(dictError != null ? dictError : "词库加载中 " + loadPercent + "%");
            return;
        }
        List<Candidate> list;
        if (scheme == SCHEME_ZHUYIN) {
            list = zhuyinEngine == null ? Collections.<Candidate>emptyList()
                    : zhuyinEngine.query(buffer, candidateLimit);
        } else {
            list = pinyinEngine == null ? Collections.<Candidate>emptyList()
                    : pinyinEngine.query(buffer, fuzzy, abbr, candidateLimit,
                            singleOnly && !strokeMode);
        }
        candidates = new ArrayList<Candidate>(list);
        applyUserDict(candidates, buffer);
        applyTimeDate(candidates, buffer);
        // 用户词、时间日期候选也守同一套口径：单字模式只留单字
        if (scheme == SCHEME_PINYIN && !strokeMode && singleOnly) {
            Iterator<Candidate> it = candidates.iterator();
            while (it.hasNext()) {
                Candidate c = it.next();
                if (c.word.codePointCount(0, c.word.length()) != 1) it.remove();
            }
        }
        toneFilterList(candidates);
        dedupeList(candidates);
        clampCaret();
        view.setCandidates(candidates, buffer);
        view.setCaretPos(caret, caretActive);
        view.setSingleFilter(singleOnly);
        view.setToneFilter(toneFilter);
        if (ic != null) {
            ic.setComposingText(buffer, 1);
        }
    }

    private void commitCandidate(int index) {
        // 数字页：点候选就把「原式＝结果」整条写进去
        if (numberPage) {
            if (index < 0 || index >= candidates.size()) return;
            String text = candidates.get(index).word;
            String expr = pendingExpr();
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                if (!expr.isEmpty()) ic.deleteSurroundingText(expr.length(), 0);
                ic.commitText(text, 1);
            }
            candidates = new ArrayList<Candidate>();
            lastCommit = text;
            if (view != null) view.setCandidates(candidates, "");
            return;
        }
        if (scheme == SCHEME_JAPANESE && !english && !pageMode()) {
            commitJapanese(index);
            return;
        }
        if (clipPanel) {
            if (index >= 0 && index < candidates.size()) {
                String text = candidates.get(index).word;
                InputConnection cic = getCurrentInputConnection();
                if (cic != null) {
                    cic.commitText(text, 1);
                    pushUndo(true, text);
                }
                rememberClip(text);
                lastCommit = text;
            }
            closeClipPanel();
            return;
        }
        if (index < 0 || index >= candidates.size()) return;
        InputConnection ic = getCurrentInputConnection();
        String word = candidates.get(index).word;
        if (ic != null) {
            ic.commitText(word, 1);
        }
        boolean timeDate = timeDateWords.contains(word);
        String learnedKey = strokeMode && !english && !pageMode() ? strokeBuffer : buffer;
        int consume = 0;
        boolean singleCommit = singleOnly && scheme == SCHEME_PINYIN && !english && !pageMode()
                && pinyinEngine != null && !buffer.isEmpty();
        if (singleCommit) {
            // 单字模式：上屏一个字就吃掉第一个音节的字母，剩下的拼音继续逐字出字
            consume = pinyinEngine.firstSyllableLength(buffer);
            if (consume <= 0 || consume > buffer.length()) {
                consume = 0;
                singleCommit = false;
            }
        }
        // 词模式连打：选中的词只吃掉它对应的那几个音节，剩下的拼音继续留在组字区
        // （打「wodachuleyichuanzi」先点「我」，后面的 dachuleyichuanzi 不会白打）
        boolean wordPartial = false;
        if (!singleCommit && !timeDate && !strokeMode && scheme == SCHEME_PINYIN
                && !english && !pageMode() && pinyinEngine != null && !buffer.isEmpty()) {
            String norm = PinyinEngine.normalize(buffer);
            if (norm.length() == buffer.length()) {
                List<String> segs = pinyinEngine.segmentForDisplay(buffer);
                int n = segs == null ? 0 : segs.size();
                int chars = word.codePointCount(0, word.length());
                if (n >= 2 && chars >= 1 && chars < n && segs.get(0).length() >= 2) {
                    int len = 0;
                    for (int i = 0; i < chars; i++) len += segs.get(i).length();
                    if (len > 0 && len < buffer.length()) {
                        consume = len;
                        wordPartial = true;
                    }
                }
            }
        }
        boolean partialCommit = singleCommit || wordPartial;
        if (partialCommit) learnedKey = buffer.substring(0, consume);
        if (!timeDate) {
            if (singleCommit) {
                // 单字模式先攒着，整串字打完再整体记一笔（黄 + 牛 → 黄牛）
                singleChain += word;
                singleChainKey += learnedKey;
            } else {
                flushSingleChain();
                learnWord(learnedKey, word);
            }
        }
        lastCommit = word;
        if (partialCommit) {
            buffer = buffer.substring(consume);
            caret = caret > consume ? caret - consume : 0;
        } else {
            buffer = "";
            caret = 0;
        }
        caretActive = false;
        clampCaret();
        strokeBuffer = "";
        candidates = new ArrayList<Candidate>();
        if (!buffer.isEmpty()) {
            // 还有后续音节没打完：接着出下一个字的候选，联想先不抢候选栏
            assocActive = false;
            updateCandidates();
            InputConnection ic2 = getCurrentInputConnection();
            if (ic2 != null) ic2.setComposingText(buffer, 1);
            return;
        }
        flushSingleChain();
        if (!timeDate) {
            maybeAssociate(word);
        }
        if (view != null) view.setCandidates(candidates, "");
    }

    /** 提交一个词之后联想：以刚上屏的整串文字为前缀，给出以它开头的更长的词（中国 → 中国人 / 中国电信）。 */
    private void maybeAssociate(String word) {
        assocActive = false;
        if (!assoc || english || pageMode() || pinyinDict == null) return;
        if (word == null || word.isEmpty()) return;
        List<Candidate> out = new ArrayList<Candidate>();
        pinyinDict.textPrefix(0, word, 16, catMask, out);
        // 联想只给「以这个词开头」的新词：去掉刚上屏的同名条目与重复项
        java.util.HashSet<String> seen = new java.util.HashSet<String>();
        seen.add(word);
        Iterator<Candidate> it = out.iterator();
        while (it.hasNext()) {
            if (!seen.add(it.next().word)) it.remove();
        }
        if (!out.isEmpty()) {
            candidates = out;
            assocActive = true;
        }
    }

    private void clearAssociate() {
        if (!assocActive) return;
        assocActive = false;
        candidates = new ArrayList<Candidate>();
        if (view != null) view.setCandidates(candidates, "");
    }

    /** 把还没确定的罗马字尾巴并进假名串。 */
    private void jpFlushPending() {
        if (jpRomaji.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        String rest = JapaneseEngine.feed(jpRomaji, sb);
        jpKana = jpKana + sb.toString() + JapaneseEngine.tail(rest);
        jpRomaji = "";
    }

    private void commitJapanese(int index) {
        boolean hadPending = !jpRomaji.isEmpty();
        jpFlushPending();
        String word;
        if (!hadPending && index >= 0 && index < candidates.size()) {
            word = candidates.get(index).word;
        } else {
            List<Candidate> list = japaneseEngine == null ? Collections.<Candidate>emptyList()
                    : japaneseEngine.query(jpKana, katakana, candidateLimit);
            word = list.isEmpty() ? JapaneseEngine.convertScript(jpKana, katakana) : list.get(0).word;
        }
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.commitText(word, 1);
        }
        learnWord(jpKana, word);
        lastCommit = word;
        jpKana = "";
        jpRomaji = "";
        assocActive = false;
        candidates = new ArrayList<Candidate>();
        if (view != null) view.setCandidates(candidates, "");
    }

    // ---------------- 剪贴板 ----------------

    private ClipboardManager clipboard() {
        return (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
    }

    /** 读取系统剪贴板里的文本（非文本内容返回 null）。 */
    private String clipboardText() {
        try {
            ClipboardManager cm = clipboard();
            if (cm == null || !cm.hasPrimaryClip()) return null;
            ClipData clip = cm.getPrimaryClip();
            if (clip == null || clip.getItemCount() == 0) return null;
            CharSequence cs = clip.getItemAt(0).coerceToText(this);
            if (cs == null) return null;
            String s = cs.toString();
            return s.isEmpty() ? null : s;
        } catch (Exception e) {
            return null;
        }
    }

    private void rememberClip(String text) {
        if (text == null) return;
        if (text.isEmpty() || text.length() > 4000) return;
        clips.remove(text);
        clips.add(0, text);
        while (clips.size() > 12) {
            clips.remove(clips.size() - 1);
        }
    }

    /** 复制：优先复制输入框里选中的文字，没有选区就复制刚上屏的词。 */
    private void copySelection() {
        String text = null;
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            CharSequence sel = ic.getSelectedText(0);
            if (!TextUtils.isEmpty(sel)) text = sel.toString();
        }
        if (text == null || text.isEmpty()) text = lastCommit;
        if (text == null || text.isEmpty()) {
            if (view != null) view.flash("没有可复制的文字");
            return;
        }
        try {
            ClipboardManager cm = clipboard();
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("霜音输入法", text));
        } catch (Exception ignored) {
        }
        rememberClip(text);
        if (view != null) view.flash("已复制");
    }

    /**
     * 给 jpKana 末尾那个假名加浊点 / 半浊点：か→が、さ→ざ、た→だ、は→ば；半浊只认 は行→ぱ行。
     * 没有任何假名、或末字不能加浊点时原样返回。
     */
    private String applyVoiced(String kana, boolean semi) {
        if (kana == null || kana.isEmpty()) return kana;
        String src = semi ? "はひふへほ" : "かきくけこさしすせそたちつてとはひふへほう";
        String dst = semi ? "ぱぴぷぺぽ" : "がぎぐげござじずぜぞだぢづでどばびぶべぼゔ";
        char last = kana.charAt(kana.length() - 1);
        int idx = src.indexOf(last);
        if (idx < 0) return kana;
        return kana.substring(0, kana.length() - 1) + dst.charAt(idx);
    }

    /** 全选输入框里的全部文字并直接复制到剪贴板：点一下就能拿去粘贴。 */
    private void selectAllAndCopy() {
        InputConnection ic = getCurrentInputConnection();
        String text = null;
        boolean ok = false;
        if (ic != null) {
            ic.performContextMenuAction(android.R.id.selectAll);
            CharSequence sel = ic.getSelectedText(0);
            if (!TextUtils.isEmpty(sel)) {
                text = sel.toString();
                ok = true;
            }
        }
        if (text == null || text.isEmpty()) {
            if (view != null) view.flash("输入框里没有文字");
            return;
        }
        boolean copied = false;
        try {
            ClipboardManager cm = clipboard();
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("霜音输入法", text));
                copied = true;
            }
        } catch (Exception ignored) {
        }
        if (copied) rememberClip(text);
        if (view != null) view.flash(copied ? "已全选并复制" : "已全选（复制失败）");
    }

    /** 粘贴：把剪贴板内容上屏。 */
    private void pasteClipboard() {
        String text = clipboardText();
        if (text == null) {
            if (view != null) view.flash("剪贴板是空的");
            return;
        }
        if (!pageMode() && !english && composing()) {
            commitCandidate(0);
        }
        closeClipPanel();
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.commitText(text, 1);
            pushUndo(true, text);
        }
        rememberClip(text);
        lastCommit = text;
        if (view != null) view.flash("已粘贴");
    }

    private void toggleClipPanel() {
        if (clipPanel) {
            closeClipPanel();
            return;
        }
        String text = clipboardText();
        if (text != null) rememberClip(text);
        if (clips.isEmpty()) {
            if (view != null) view.flash("剪贴板是空的");
            return;
        }
        assocActive = false;
        clipPanel = true;
        updateCandidates();
    }

    private void closeClipPanel() {
        boolean was = clipPanel;
        clipPanel = false;
        if (!was) return;
        if (view != null) view.setExpanded(false);
        updateCandidates();
    }

    private boolean isChinesePunct(String s) {
        if (s.length() != 1) return false;
        char c = s.charAt(0);
        return "，。？！：；、（）“”‘’《》…".indexOf(c) >= 0;
    }

    /** 日语键盘上的半角标点转成全角日文标点。 */
    private String jpPunct(String s) {
        if (s.equals("，") || s.equals(",")) return "、";
        if (s.equals("。") || s.equals(".")) return "。";
        if (s.equals("？") || s.equals("?")) return "？";
        if (s.equals("！") || s.equals("!")) return "！";
        return s;
    }

    private void handleChar(String s) {
        if (s == null || s.length() == 0) return;
        if (clipPanel) closeClipPanel();
        if (english || pageMode()) {
            // 英文模式下「，。」键输出半角标点，轻点句点、长按逗号
            if (english && s.length() == 1) {
                if (s.equals("，")) s = ".";
                else if (s.equals("。")) s = ",";
            }
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) ic.commitText(s, 1);
            // 数字页：每打一个字符就重算一次，候选栏里常驻「1+1=2」这样的结果
            if (numberPage) updateCandidates();
            return;
        }
        clearAssociate();

        // 笔画模式：1~5 是笔画码，0 是通配；其余字符（标点）先上屏候选项再输出标点
        if (strokeMode && !english && !pageMode() && s.length() == 1) {
            char sc = s.charAt(0);
            if (".012345".indexOf(sc) >= 0) {
                if (sc == '.') return;
                appendStroke(sc);
                return;
            }
            if (!strokeBuffer.isEmpty()) {
                commitCandidate(0);
            }
        }

        if (scheme == SCHEME_JAPANESE) {
            char c0 = s.charAt(0);
            if (s.equals("゛") || s.equals("゜")) {
                // 浊点 / 半浊点：直接把上一个假名换成浊音（か→が、は→ぱ）
                jpKana = applyVoiced(jpKana, s.equals("゜"));
                updateCandidates();
                return;
            }
            boolean kana = JapaneseEngine.isHiragana(c0) || (c0 >= '\u30A1' && c0 <= '\u30F6');
            if (kana) {
                // 直接输入假名（五十音键盘）
                if (!jpRomaji.isEmpty()) jpFlushPending();
                jpKana = jpKana + JapaneseEngine.normalizeKey(s);
                updateCandidates();
                return;
            }
            if (c0 >= 'a' && c0 <= 'z' || c0 == '-' || c0 == '\'') {
                StringBuilder sb = new StringBuilder();
                jpRomaji = JapaneseEngine.feed(jpRomaji + s, sb);
                jpKana = jpKana + sb.toString();
                updateCandidates();
                return;
            }
            if (isChinesePunct(s) || s.equals(",") || s.equals(".")) {
                commitCandidate(0);
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) ic.commitText(jpPunct(s), 1);
                return;
            }
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) ic.commitText(s, 1);
            return;
        }

        char c = s.charAt(0);
        boolean append;
        if (scheme == SCHEME_ZHUYIN) {
            append = ZhuyinEngine.isBopomofo(c) || (c >= '1' && c <= '5') || Character.isLetter(c);
        } else {
            append = c >= 'a' && c <= 'z';
        }
        if (append) {
            if (caretActive) {
                clampCaret();
                StringBuilder sb = new StringBuilder(buffer);
                sb.insert(caret, s);
                buffer = sb.toString();
                caret += s.length();
            } else {
                buffer = buffer + s;
                caret = buffer.length();
            }
            updateCandidates();
            return;
        }
        if (isChinesePunct(s) && !candidates.isEmpty()) {
            commitCandidate(0);
        }
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.commitText(s, 1);
    }

    /** 把光标下标钳制到当前缓冲范围内。 */
    private void clampCaret() {
        if (caret < 0) caret = 0;
        if (caret > buffer.length()) caret = buffer.length();
    }

    private boolean composing() {
        if (strokeMode && !english && !pageMode()) {
            return !strokeBuffer.isEmpty();
        }
        if (scheme == SCHEME_JAPANESE) {
            return !jpKana.isEmpty() || !jpRomaji.isEmpty();
        }
        return !buffer.isEmpty();
    }

    private void backspace() {
        if (clipPanel) {
            closeClipPanel();
            return;
        }
        clearAssociate();
        if (strokeMode && !english && !pageMode() && !strokeBuffer.isEmpty()) {
            strokeBuffer = strokeBuffer.substring(0, strokeBuffer.length() - 1);
            updateCandidates();
            return;
        }
        if (!pageMode() && !english && composing()) {
            if (scheme == SCHEME_JAPANESE) {
                if (!jpRomaji.isEmpty()) {
                    jpRomaji = jpRomaji.substring(0, jpRomaji.length() - 1);
                } else if (jpKana.length() > 1) {
                    jpKana = jpKana.substring(0, jpKana.offsetByCodePoints(jpKana.length(), -1));
                } else {
                    jpKana = "";
                }
                updateCandidates();
                return;
            } else {
                clampCaret();
                if (caretActive && caret > 0) {
                    buffer = buffer.substring(0, caret - 1) + buffer.substring(caret);
                    caret--;
                    if (buffer.isEmpty()) {
                        flushSingleChain();
                        caretActive = false;
                        caret = 0;
                    }
                    updateCandidates();
                    return;
                }
                if (!buffer.isEmpty()) {
                    buffer = buffer.substring(0, buffer.length() - 1);
                    caret = buffer.length();
                    if (buffer.isEmpty()) {
                        flushSingleChain();
                        caretActive = false;
                    }
                    updateCandidates();
                    return;
                }
                // 缓冲为空：落到下面的编辑器级退格
            }
        }
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        CharSequence sel = ic.getSelectedText(0);
        if (!TextUtils.isEmpty(sel)) {
            pushUndo(false, sel.toString());
            ic.commitText("", 1);
        } else {
            CharSequence before = ic.getTextBeforeCursor(1, 0);
            if (!TextUtils.isEmpty(before)) pushUndo(false, before.toString());
            ic.deleteSurroundingText(1, 0);
        }
    }

    private void space() {
        if (clipPanel) {
            closeClipPanel();
            return;
        }
        if (assocActive) {
            // 联想词只能点选，不抢空格：空格照常打个空格，联想随之收起
            clearAssociate();
            InputConnection c = getCurrentInputConnection();
            if (c != null) c.commitText(" ", 1);
            return;
        }
        if (!english && !pageMode() && composing()) {
            if (!candidates.isEmpty()) {
                commitCandidate(0);
                return;
            }
            if (scheme == SCHEME_JAPANESE) {
                // 只有半截罗马字时，先落成假名再上屏
                commitJapanese(0);
                return;
            }
        }
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.commitText(" ", 1);
    }

    private void enter() {
        if (clipPanel) {
            closeClipPanel();
            return;
        }
        if (assocActive) {
            // 回车也不替用户选联想词，只把联想收起，接着走正常的回车流程
            clearAssociate();
        }
        if (!english && !pageMode() && composing()) {
            if (!candidates.isEmpty()) {
                commitCandidate(0);
            } else if (scheme == SCHEME_JAPANESE) {
                commitJapanese(0);
            } else {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) ic.commitText(buffer, 1);
                buffer = "";
                updateCandidates();
            }
            return;
        }
        if (getCurrentInputEditorInfo() != null) {
            int a = getCurrentInputEditorInfo().imeOptions & EditorInfo.IME_MASK_ACTION;
            if (a != EditorInfo.IME_ACTION_NONE && a != EditorInfo.IME_ACTION_UNSPECIFIED) {
                sendDefaultEditorAction(true);
                return;
            }
        }
        sendKeyChar('\n');
    }

    private void toggleLang() {
        english = !english;
        clipPanel = false;
        assocActive = false;
        strokeBuffer = "";
        if (english) {
            buffer = "";
            jpKana = "";
            jpRomaji = "";
            candidates = new ArrayList<Candidate>();
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) ic.finishComposingText();
        }
        applyKeyboard();
        updateCandidates();
        if (view != null) {
            view.flash(english ? "English 输入" : "中文 · " + schemeName());
        }
    }

    private String schemeName() {
        if (scheme == SCHEME_ZHUYIN) return "注音";
        if (scheme == SCHEME_JAPANESE) return "日本語";
        return "拼音";
    }

    // ---------------- 笔画输入 ----------------

    private void appendStroke(char code) {
        if (strokeBuffer.length() >= 24) return;
        strokeBuffer = strokeBuffer + code;
        updateCandidates();
    }

    /** 删除键横向一划：清空当前正在输入的整串（拼音串 / 假名 / 笔画串 / 联想），不动已上屏文字。 */
    private void clearInput() {
        boolean had = !buffer.isEmpty() || !jpKana.isEmpty() || !jpRomaji.isEmpty()
                || !strokeBuffer.isEmpty() || !candidates.isEmpty();
        buffer = "";
        caret = 0;
        caretActive = false;
        jpKana = "";
        jpRomaji = "";
        strokeBuffer = "";
        assocActive = false;
        clipPanel = false;
        clearToneFilter();
        candidates = new ArrayList<Candidate>();
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            // 关键：把输入框里的预选文字真正删掉（以前只 finishComposing 会把字母留在框里）
            ic.setComposingText("", 1);
            ic.finishComposingText();
        }
        updateCandidates();
        if (had && view != null) view.flash("已清空输入");
    }

    /** 分词键：在笔画串里插入分隔符，把已经点好的部分当作一个字结束，后续笔画开始下一个字。 */
    private void splitStroke() {
        if (strokeBuffer.isEmpty() || strokeBuffer.endsWith("|")) return;
        if (strokeBuffer.length() >= 24) return;
        strokeBuffer = strokeBuffer + "|";
        updateCandidates();
    }

    /** 笔画码转成看得懂的字形串，显示在候选栏。 */
    private String strokeHint() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strokeBuffer.length(); i++) {
            switch (strokeBuffer.charAt(i)) {
                case '1': sb.append('一'); break;
                case '2': sb.append('丨'); break;
                case '3': sb.append('丿'); break;
                case '4': sb.append('丶'); break;
                case '5': sb.append('ㄥ'); break;
                case '|': sb.append('｜'); break;
                default: sb.append('＊'); break;
            }
        }
        return sb.toString();
    }

    /** 只有符号页的「笔画」键能进来，进来后方案键消失，只能从「笔拼」回拼音。 */
    private void enterStroke() {
        symbolPage = false;
        symPage = 0;
        numberPage = false;
        clipPanel = false;
        strokeMode = true;
        strokeBuffer = "";
        buffer = "";
        jpKana = "";
        jpRomaji = "";
        assocActive = false;
        candidates = new ArrayList<Candidate>();
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.finishComposingText();
        applyKeyboard();
        updateCandidates();
        if (view != null) {
            view.setStatus(null);
            view.flash("笔画输入 · 点「笔拼」回拼音");
        }
    }

    private void exitStroke() {
        strokeMode = false;
        strokeBuffer = "";
        scheme = SCHEME_PINYIN;
        Prefs.get(this).edit().putInt(Prefs.KEY_SCHEME, scheme).apply();
        candidates = new ArrayList<Candidate>();
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.setComposingText("", 1);
            ic.finishComposingText();
        }
        applyKeyboard();
        updateCandidates();
        if (view != null) {
            view.setStatus(null);
            view.flash("拼音输入");
        }
    }

    private void toggleScheme() {
        // 笔画模式下方案键不存在，保险再挡一层：笔画只能回拼音
        if (strokeMode) {
            exitStroke();
            return;
        }
        int next = scheme;
        for (int i = 0; i < 3; i++) {
            next = (next + 1) % 3;
            if (Prefs.schemeEnabled(this, next)) break;
        }
        if (next == scheme) {
            if (view != null) view.flash("只开了这一种输入法，可在设置里打开其它");
            return;
        }
        scheme = next;
        Prefs.get(this).edit().putInt(Prefs.KEY_SCHEME, scheme).apply();
        clipPanel = false;
        assocActive = false;
        buffer = "";
        jpKana = "";
        jpRomaji = "";
        candidates = new ArrayList<Candidate>();
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.finishComposingText();
        applyKeyboard();
        updateCandidates();
        if (view != null) {
            view.setStatus(null);
            view.setCandidates(candidates, "");
            view.flash(SCHEME_NAMES[scheme]);
        }
    }

    private void toggleKatakana() {
        katakana = !katakana;
        Prefs.get(this).edit().putBoolean(Prefs.KEY_KATAKANA, katakana).apply();
        applyKeyboard();
        updateCandidates();
        if (view != null) {
            view.flash(katakana ? "片假名 カタカナ" : "平假名 ひらがな");
        }
    }

    private void toggleKanaLayout() {
        kanaLayout = !kanaLayout;
        Prefs.get(this).edit().putBoolean(Prefs.KEY_KANA_LAYOUT, kanaLayout).apply();
        applyKeyboard();
        if (view != null) {
            view.flash(kanaLayout ? "五十音键盘" : "罗马字键盘");
        }
    }

    private void flashTip(String text) {
        if (view != null) view.flash(text);
    }

    /** 光标前那段连续的算式字符（没有就返回空串）。 */
    private String pendingExpr() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return "";
        CharSequence before = ic.getTextBeforeCursor(64, 0);
        String s = before == null ? "" : before.toString();
        int start = s.length();
        while (start > 0) {
            char c = s.charAt(start - 1);
            if ((c >= '0' && c <= '9') || c == '.' || c == '+' || c == '-'
                    || c == '*' || c == '/' || c == '×' || c == '÷'
                    || c == '%' || c == '％') {
                start--;
            } else {
                break;
            }
        }
        return s.substring(start);
    }

    /** 把算好的结果拼成「1＋1＝2」这种整条式子，运算符用键盘上的全角字形。 */
    private static String equation(String expr, String result) {
        StringBuilder sb = new StringBuilder(expr.length() + result.length() + 1);
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            switch (c) {
                case '+': sb.append('＋'); break;
                case '-': sb.append('－'); break;
                case '*': sb.append('×'); break;
                case '/': sb.append('÷'); break;
                case '%': sb.append('％'); break;
                default: sb.append(c); break;
            }
        }
        sb.append('＝').append(result);
        return sb.toString();
    }

    /** 数字页候选：整条「1＋1＝2」，没有可算的算式时返回 null。 */
    private Candidate calcPreview() {
        String expr = pendingExpr();
        if (expr.isEmpty()) return null;
        if (expr.indexOf('+') < 0 && expr.indexOf('-') < 0 && expr.indexOf('*') < 0
                && expr.indexOf('/') < 0 && expr.indexOf('×') < 0 && expr.indexOf('÷') < 0
                && expr.indexOf('%') < 0) {
            return null;
        }
        Double v = Calc.eval(expr);
        if (v == null) return null;
        String shown = equation(expr, Calc.format(v));
        return new Candidate(shown, 1000, "", 1000);
    }

    /** 数字键盘的「＝」：把光标前那段算式换成整条「1＋1＝2」。 */
    private void calculate() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            flashTip("算不了：当前没有输入框");
            return;
        }
        String expr = pendingExpr();
        if (expr.isEmpty()) {
            flashTip("先打算式，例如 12+34 再点 ＝");
            return;
        }
        Double v = Calc.eval(expr);
        if (v == null) {
            flashTip("算式不完整：" + expr);
            return;
        }
        String whole = equation(expr, Calc.format(v));
        ic.deleteSurroundingText(expr.length(), 0);
        ic.commitText(whole, 1);
        flashTip(whole);
        updateCandidates();
    }

    /** 长按「拼注日」在日语下循环：罗马字 → 平假名五十音 → 片假名五十音 → 罗马字。 */
    private void cycleJpMode() {
        if (scheme != SCHEME_JAPANESE || english) {
            if (view != null) view.flash("「日語」键 / 长按「拼注日」只在日语模式下切罗马字、假名");
            return;
        }
        if (!japaneseOn) {
            if (view != null) view.flash("日语输入法已在设置里关闭");
            return;
        }
        if (!kanaLayout) {
            kanaLayout = true;
            katakana = false;
        } else if (!katakana) {
            katakana = true;
        } else {
            kanaLayout = false;
            katakana = false;
        }
        Prefs.get(this).edit()
                .putBoolean(Prefs.KEY_KANA_LAYOUT, kanaLayout)
                .putBoolean(Prefs.KEY_KATAKANA, katakana)
                .apply();
        applyKeyboard();
        updateCandidates();
        if (view != null) {
            view.flash(kanaLayout
                    ? (katakana ? "片假名 五十音" : "平假名 五十音")
                    : "罗马字输入");
        }
    }

    @Override
    public void onKey(Keyboard.Key key) {
        if (vibrate && view != null) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
        switch (key.action) {
            case Keyboard.A_CHAR:
                handleChar(key.insert);
                break;
            case Keyboard.A_BACKSPACE:
                backspace();
                if (numberPage) updateCandidates();
                break;
            case Keyboard.A_SPACE:
                space();
                break;
            case Keyboard.A_ENTER:
                enter();
                break;
            case Keyboard.A_LANG:
                toggleLang();
                break;
            case Keyboard.A_SCHEME:
                toggleScheme();
                break;
            case Keyboard.A_JP_MODE:
                cycleJpMode();
                break;
            case Keyboard.A_KANA:
                toggleKanaLayout();
                break;
            case Keyboard.A_SHIFT:
                // ⇧ 键（原来「，。」的位置）：中文界面按一下直接进英文大写；
                // 英文界面按一下切大小写（键面蓝色高亮跟着变）。英文长按 123 切大小写依旧保留
                if (!english) {
                    english = true;
                    englishUpper = true;
                    buffer = "";
                    jpKana = "";
                    jpRomaji = "";
                    strokeBuffer = "";
                    candidates = new ArrayList<Candidate>();
                    InputConnection icS = getCurrentInputConnection();
                    if (icS != null) icS.finishComposingText();
                    applyKeyboard();
                    updateCandidates();
                    if (view != null) view.flash("英文大写 ABC");
                    break;
                }
                englishUpper = !englishUpper;
                applyKeyboard();
                if (view != null) view.flash(englishUpper ? "大写 ABC" : "小写 abc");
                break;
            case Keyboard.A_SCRIPT:
                toggleKatakana();
                break;
            case Keyboard.A_SYMBOL:
                symbolPage = true;
                symPage = 0;
                numberPage = false;
                buffer = "";
                candidates = new ArrayList<Candidate>();
                applyKeyboard();
                updateCandidates();
                break;
            case Keyboard.A_NUMBER:
                // 短按 123：进数字键盘；符号页仍然从数字页的「符号」键进
                numberPage = true;
                symbolPage = false;
                symPage = 0;
                buffer = "";
                candidates = new ArrayList<Candidate>();
                applyKeyboard();
                updateCandidates();
                break;
            case Keyboard.A_SMALL_KANA:
                kanaSmall = !kanaSmall;
                applyKeyboard();
                if (view != null) {
                    view.flash(kanaSmall ? "小假名 开：ぁぃぅぇぉ ゃゅょ っ ゎ" : "小假名 关");
                }
                break;
            case Keyboard.A_CALC:
                calculate();
                break;
            case Keyboard.A_ALPHA:
                // 数字页 / 符号页的 ABC：直接进英文字母键盘（要回中文按「中/英」）
                boolean fromPage = pageMode();
                symbolPage = false;
                symPage = 0;
                numberPage = false;
                if (fromPage) {
                    english = true;
                    englishUpper = false;
                }
                applyKeyboard();
                updateCandidates();
                break;
            case Keyboard.A_BACK_MODE:
                // 数字页的「返回」与底行第一个模式键：退回进入数字页前的那个输入模式
                numberPage = false;
                symbolPage = false;
                symPage = 0;
                buffer = "";
                candidates = new ArrayList<Candidate>();
                applyKeyboard();
                updateCandidates();
                break;
            case Keyboard.A_SETTINGS:
                Intent i = new Intent(this, SettingsActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                break;
            case Keyboard.A_COPY:
                copySelection();
                break;
            case Keyboard.A_SELECT_ALL:
                selectAllAndCopy();
                break;
            case Keyboard.A_DAKU:
                jpKana = applyVoiced(jpKana, false);
                updateCandidates();
                break;
            case Keyboard.A_HANDAKU:
                jpKana = applyVoiced(jpKana, true);
                updateCandidates();
                break;
            case Keyboard.A_PASTE:
                pasteClipboard();
                break;
            case Keyboard.A_CLIP:
                toggleClipPanel();
                break;
            case Keyboard.A_STROKE:
                if (!strokeOn) {
                    if (view != null) view.flash("笔画输入法已在设置里关闭");
                    break;
                }
                enterStroke();
                break;
            case Keyboard.A_STROKE_PINYIN:
                if (strokeMode) {
                    exitStroke();
                } else {
                    enterStroke();
                }
                break;
            case Keyboard.A_SPLIT:
                splitStroke();
                break;
            case Keyboard.A_CLEAR_ALL:
                clearInput();
                break;
            case Keyboard.A_CLOSE_IME:
                requestHideSelf(0);
                break;
            default:
                break;
        }
    }

    @Override
    public void onCandidate(int index) {
        commitCandidate(index);
        // 选完一条就退出声调筛选，不把「只看第 N 声」的状态带到下一句
        clearToneFilter();
        if (view != null) {
            view.setExpanded(false);
            view.setToneFilter(0);
        }
    }

    /** 「字词」键：切换单字 / 词模式，重算候选但不改变展开状态。 */
    @Override
    public void onToggleSingle() {
        singleOnly = !singleOnly;
        Prefs.get(this).edit().putBoolean(Prefs.KEY_SINGLE, singleOnly).apply();
        flushSingleChain();
        updateCandidates();
        if (view != null) view.setSingleFilter(singleOnly);
    }

    /** 点字条上的拼音：激活光标并落到点中的位置，不碰编辑器选区。 */
    @Override
    public void onCaretTap(int index) {
        if (english || pageMode() || strokeMode || scheme == SCHEME_JAPANESE) return;
        if (buffer.isEmpty()) return;
        caretActive = true;
        caret = Math.max(0, Math.min(buffer.length(), index));
        updateCandidates();
    }

    /** 声调筛选复位：选完候选 / 清空输入后回到「不限」，按键同时熄灭。 */
    private void clearToneFilter() {
        if (toneFilter == 0) return;
        toneFilter = 0;
        if (view != null) view.setToneFilter(0);
    }

    /** 声调筛选键：0 取消，1–4 只看这个调的同音候选。 */
    @Override
    public void onToneFilter(int tone) {
        toneFilter = tone < 0 || tone > 4 ? 0 : tone;
        if (view != null) view.setToneFilter(toneFilter);
        updateCandidates();
        // 声调键自己会变蓝，不再弹横幅挡按键
    }

    /** 「全删」：把当前这一串输入连组字区一起清掉。 */
    @Override
    /**
     * 全选删除：真的把输入框里的文字删掉，不只是清预选。
     * 顺序：先落定并清掉正在输入的编码（否则编码会被当成正文一起删掉一半），
     * 再「全选 + 退格」，最后用 deleteSurroundingText 兜底（有的输入框不认全选）。
     */
    public void onSelectAllDelete() {
        clearInput();
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        int deleted = 0;

        // 1) 系统全选 + 退格：覆盖绝大多数输入框（聊天框、笔记、网页表单）
        CharSequence before = ic.getTextBeforeCursor(4096, 0);
        CharSequence after = ic.getTextAfterCursor(4096, 0);
        int nb = before == null ? 0 : before.length();
        int na = after == null ? 0 : after.length();
        if (nb + na > 0 && nb + na <= 5000) {
            // 全删后的撤回要能整段补回来，所以光标后那段也一起记
            pushUndo(false, (before == null ? "" : before.toString())
                    + (after == null ? "" : after.toString()));
        }
        if (nb > 0 || na > 0) {
            ic.performContextMenuAction(android.R.id.selectAll);
            CharSequence sel = ic.getSelectedText(0);
            int n = sel == null ? 0 : sel.length();
            if (n > 0) {
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
                deleted = n;
            } else {
                // 全选没生效（个别输入框拒绝 selectAll），直接删光标前后
                ic.deleteSurroundingText(nb, na);
                deleted = nb + na;
            }
        }

        // 2) 兜底：文字很长或编辑器限了单次删除长度，分段删到真的空
        for (int guard = 0; guard < 8; guard++) {
            CharSequence b2 = ic.getTextBeforeCursor(2048, 0);
            CharSequence a2 = ic.getTextAfterCursor(2048, 0);
            int nb2 = b2 == null ? 0 : b2.length();
            int na2 = a2 == null ? 0 : a2.length();
            if (nb2 == 0 && na2 == 0) break;
            ic.deleteSurroundingText(nb2, na2);
            deleted += nb2 + na2;
        }

        if (view != null) view.flash(deleted > 0 ? "已全选删除" : "输入框里没有文字");
    }

    /** 声调筛选：候选读音的第一音节调值对上才留（没有读音信息的不过滤）。 */
    private void toneFilterList(List<Candidate> list) {
        if (toneFilter <= 0 || list == null || list.isEmpty()) return;
        if (scheme != SCHEME_PINYIN && scheme != SCHEME_ZHUYIN) return;
        Iterator<Candidate> it = list.iterator();
        while (it.hasNext()) {
            Candidate c = it.next();
            String t = c.tones;
            if (t == null || t.isEmpty()) continue;      // 时间日期、用户词等没有调值，保留
            char c0 = t.charAt(0);
            if (c0 < '1' || c0 > '4') continue;
            if (c0 - '0' != toneFilter) it.remove();
        }
    }

    /** 同一个词只留分最高的那一条，避免同名候选重复出现。 */
    private void dedupeList(List<Candidate> list) {
        if (list == null || list.size() < 2) return;
        java.util.HashSet<String> seen = new java.util.HashSet<String>();
        Iterator<Candidate> it = list.iterator();
        while (it.hasNext()) {
            Candidate c = it.next();
            if (!seen.add(c.word)) it.remove();
        }
    }

    /** 符号页左右滑动翻屏：三屏循环，翻完闪一下当前是哪一屏。 */
    @Override
    public void onSymbolFlip(int dir) {
        symPage = (symPage + dir + Keyboard.SYMBOL_PAGES) % Keyboard.SYMBOL_PAGES;
        applyKeyboard();
        if (view != null) {
            String tip;
            if (symPage == 0) tip = "数字 · 四则 · 中文标点";
            else if (symPage == 1) tip = "数学 · 箭头 · 半角标点";
            else tip = "序号 · 集合几何 · 引号括号";
            view.flash("符号 " + (symPage + 1) + "/" + Keyboard.SYMBOL_PAGES + " · " + tip);
        }
    }

    @Override
    public void onClipDelete(int index) {
        if (index < 0 || index >= clips.size()) return;
        clips.remove(index);
        if (clips.isEmpty()) {
            closeClipPanel();
            if (view != null) view.flash("剪贴板已清空");
            return;
        }
        updateCandidates();
        if (view != null) view.flash("已删除 1 条");
    }

    @Override
    public void onClipClear() {
        // 一键清空：历史列表全清，顺便把系统剪贴板也置空（对方 App 粘出来就是空的）
        clips.clear();
        boolean sysCleared = false;
        try {
            ClipboardManager cm = clipboard();
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("", ""));
                sysCleared = true;
            }
        } catch (Exception ignored) {
        }
        closeClipPanel();
        if (view != null) {
            view.flash(sysCleared ? "剪贴板已清空" : "历史已清空（系统剪贴板不让清）");
        }
    }

    // ---------------- 词库加载 ----------------

    private void loadDictionaries() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File dir = new File(getFilesDir(), "dicts");
                    if (!dir.exists() && !dir.mkdirs()) {
                        throw new IOException("无法创建词库目录");
                    }
                    syncStamp(dir);
                    final String[] assets = {"pinyin.bin", "zhuyin.bin", "japanese.bin", "stroke.bin",
                            "reading_pin.txt", "reading_zhy.txt"};
                    final int step = 88 / assets.length;
                    for (int i = 0; i < assets.length; i++) {
                        final int base = i * step;
                        copyAssetIfNeeded(assets[i], new File(dir, assets[i]), new Progress() {
                            @Override
                            public void onProgress(int pct) {
                                loadPercent = base + pct * step / 100;
                                postStatus();
                            }
                        });
                        loadPercent = base + step;
                        postStatus();
                    }
                    Set<String> syls = readSyllables();
                    syllableSet = syls;
                    if (view != null) view.setSyllables(syls);
                    loadPercent = 94;
                    postStatus();
                    pinyinDict = new BinDict(new File(dir, "pinyin.bin"));
                    pinyinEngine = new PinyinEngine(pinyinDict, syls);
                    zhuyinEngine = new ZhuyinEngine(new BinDict(new File(dir, "zhuyin.bin")),
                            pinyinDict);
                    readings = Readings.load(dir);
                    File jp = new File(dir, "japanese.bin");
                    if (jp.exists()) {
                        japaneseEngine = new JapaneseEngine(new BinDict(jp));
                    }
                    File st = new File(dir, "stroke.bin");
                    if (st.exists()) {
                        strokeEngine = new StrokeEngine(new BinDict(st));
                    }
                    applyMasks();
                    dictReady = true;
                    loadPercent = 100;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (view != null) {
                                view.setStatus(null);
                                // 读音表是随词库一起加载的，加载完成前开键盘会拿到 null；
                                // 这里补一次回填，避免第一次上屏后候选上方没有读音小字。
                                view.setReading(readings, showReading);
                            }
                            updateCandidates();
                        }
                    });
                } catch (final Exception e) {
                    dictError = "词库加载失败：" + e.getMessage();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (view != null) view.setStatus(dictError);
                        }
                    });
                }
            }
        }, "dict-loader").start();
    }

    private interface Progress {
        void onProgress(int pct);
    }

    private void postStatus() {
        final int pct = loadPercent;
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (view != null && !dictReady) {
                    view.setStatus("词库加载中 " + pct + "%");
                }
            }
        });
    }

    /** APK 升级后清掉旧词库副本，避免继续用旧数据。 */
    private void syncStamp(File dir) {
        long updateTime = 0L;
        try {
            updateTime = getPackageManager().getPackageInfo(getPackageName(), 0).lastUpdateTime;
        } catch (Exception ignored) {
        }
        File stamp = new File(dir, ".apk-stamp");
        String want = String.valueOf(updateTime);
        String have = null;
        if (stamp.exists()) {
            try {
                BufferedReader r = new BufferedReader(new InputStreamReader(
                        new FileInputStream(stamp), "UTF-8"));
                have = r.readLine();
                r.close();
            } catch (IOException ignored) {
            }
        }
        if (!want.equals(have)) {
            File[] old = dir.listFiles();
            if (old != null) {
                for (int i = 0; i < old.length; i++) {
                    old[i].delete();
                }
            }
            try {
                FileOutputStream out = new FileOutputStream(stamp);
                out.write(want.getBytes("UTF-8"));
                out.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void copyAssetIfNeeded(String name, File target, Progress progress) throws IOException {
        if (target.exists() && target.length() > 1024) {
            progress.onProgress(100);
            return;
        }
        AssetManager am = getAssets();
        InputStream in = am.open(name);
        File tmp = new File(target.getAbsolutePath() + ".tmp");
        FileOutputStream out = new FileOutputStream(tmp);
        byte[] buf = new byte[8192];
        long total = 0;
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
            total += n;
        }
        out.flush();
        out.close();
        in.close();
        if (!tmp.renameTo(target)) {
            throw new IOException("词库写入失败");
        }
        progress.onProgress(100);
    }

    private Set<String> readSyllables() {
        Set<String> set = new HashSet<String>();
        try {
            InputStream in = getAssets().open("syllables.txt");
            BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) set.add(line);
            }
            r.close();
        } catch (IOException ignored) {
        }
        return set;
    }
}

package com.shuangyin.ime;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.OverScroller;

import com.shuangyin.ime.core.Candidate;
import com.shuangyin.ime.core.JapaneseEngine;
import com.shuangyin.ime.core.PinyinEngine;
import com.shuangyin.ime.core.Readings;
import com.shuangyin.ime.core.ZhuyinSyllables;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** 自绘键盘：候选栏 + 主键盘 + 符号页。 */
public class ImeView extends View {

    public interface Listener {
        void onKey(Keyboard.Key key);
        void onCandidate(int index);
        /** 删除剪贴板历史里的第 index 条。 */
        void onClipDelete(int index);
        /** 清空剪贴板历史。 */
        void onClipClear();
        /** 展开面板「单 / 词」筛选：单字模式开关。 */
        void onToggleSingle();
        /** 符号页左右滑动翻屏：dir=+1 下一屏，-1 上一屏。 */
        void onSymbolFlip(int dir);
        /** 声调筛选：tone 0 取消，1–4 只看该调值。 */
        void onToneFilter(int tone);
        /** 全选删除：清空整串输入。 */
        void onSelectAllDelete();
        /** 点组字条上的拼音：激活光标并落到第 index 个字母前。 */
        void onCaretTap(int index);
        /** 撤回上一步粘贴 / 删除。 */
        void onUndo();
    }

    private static final int C_BG = 0xFFE9EEF3;
    private static final int C_KEY = 0xFFFFFFFF;
    private static final int C_KEY_PRESSED = 0xFFCBDCEA;
    private static final int C_KEY_SPECIAL = 0xFFDDE7EF;
    private static final int C_ACCENT = 0xFF3E7CB1;
    private static final int C_TEXT = 0xFF22384D;
    private static final int C_HINT = 0xFF8FA3B3;
    private static final int C_BAR = 0xFFF7FAFC;
    private static final int C_DIVIDER = 0xFFD3DEE7;
    /** 注音键右下角的拼音字母用红色。 */
    private static final int C_HINT_RED = 0xFFC62828;
    private static final int C_ENGLISH = 0xFFB4691B;

    private final float density;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF tmpRect = new RectF();
    private final Handler handler = new Handler(Looper.getMainLooper());

    /** 顶部动作键那一行（设置 / 声调 / 字词 / 全删 / 全复 / 粘贴板 / ×）。 */
    private float actH;
    private float barH;
    private float gridRowH;
    private float keyH;
    private float gap;
    private float pad;
    /** 底行与上方按键之间的间距，让最下面一行不贴在一起。 */
    private float rowGapExtra;
    private float bottomPad;

    /** 动作键 id。 */
    private static final int ACT_SET = 0;
    private static final int ACT_TONE_BASE = 1;    // 1–4 对应声调 1–4
    private static final int ACT_WORD = 5;
    private static final int ACT_DEL_ALL = 6;
    private static final int ACT_SEL_ALL = 7;
    private static final int ACT_PASTE = 8;
    private static final int ACT_CLOSE = 9;
    private static final int ACT_UNDO = 10;
    private static final int ACT_CLIP_CLEAR = 11;
    /** 动作键命中矩形：x, y, w, h, id。 */
    private final List<float[]> actBoxes = new ArrayList<float[]>();
    /** 组字条：缓冲内光标位置与是否激活；未激活时只显示拼音、不画光标。 */
    private int caretPos = 0;
    private boolean caretActive = false;
    private int actPressed = -1;
    private long actDownTime;
    /** 动作键长按判定：全复的「长按变全删」用，比字母键稍长一点，避免误触。 */
    private static final long ACT_LONG_MS = 380;

    private Keyboard keyboard;
    private List<Candidate> candidates = new ArrayList<Candidate>();
    private int selected = 0;
    private boolean expanded;
    /** 展开面板：每页 6×3 = 18 个候选，左右滑动翻页。 */
    private static final int GRID_COLS = 6;
    private static final int GRID_ROWS = 3;
    private static final int GRID_PER_PAGE = GRID_COLS * GRID_ROWS;
    private int gridPage;
    private String composing = "";
    private String status;

    private final List<float[]> candBoxes = new ArrayList<float[]>();
    /** 剪贴板模式下每个格子右上角 × 的点击区域。 */
    private final List<float[]> candDelBoxes = new ArrayList<float[]>();

    /** 候选栏横向滚动：当前偏移、最大偏移、每个候选的左边与宽度。 */
    private float candScroll;
    private float candScrollMax;
    private float[] candLeft = new float[0];
    private float[] candWide = new float[0];
    private boolean candDragging;
    private boolean candTouchZone;
    private float candDownX;
    private float candDownScroll;
    private float candDownY;
    private OverScroller candScroller;
    private boolean candNeedSnap;
    private float candFlingV;
    private long candMoveT;
    private float candMoveX;
    private float candMoveY;
    /** 预选框上竖划：上划展开、面板上竖划收起。 */
    private boolean vertSwipe;
    private float vertY0;
    private float vertX0;

    private final List<float[]> keyBoxes = new ArrayList<float[]>();

    private boolean clipMode;
    /** 剪贴板：单点浏览的条目全文（显示在候选栏），双击才粘贴。 */
    private String clipPreview;
    /** 剪贴板：上一次单点的条目与时间，用于判定双击粘贴。 */
    private int clipLastTap = -1;
    private long clipLastTapMs;
    private static final long CLIP_DOUBLE_MS = 300;
    /** 展开面板「单 / 词」筛选：true 表示只出单字；动作键「字词」控制。 */
    private boolean singleFilter;
    /** 声调筛选：0 不过滤，1–4 只看这个调值。 */
    private int toneFilter;
    /** 候选上方小字倍率（1.0 = 100%）。 */
    private float readScale = 1f;
    /** 键面右下角小字倍率（1.0 = 100%）。 */
    private float hintScale = 1f;

    private Keyboard.Key pressedKey;
    private float pressX, pressY;
    private long pressDownTime;
    private boolean longFired;
    /** 删除键上横向一划清空输入：记录按下的键与起点，划动超过阈值就触发一次。 */
    private Keyboard.Key slideKey;
    private float slideStartX;
    private boolean slideFired;
    private int pressedCand = -1;
    private int pressedDel = -1;
    private boolean expandingArea;
    private boolean clipArea;
    private boolean clearArea;
    private boolean setArea;
    private boolean closeArea;

    private int schemeIndex = 0;
    private int jpMode = 0;
    private boolean englishMode;
    private boolean englishUpper;
    private Readings readings;
    /** 全拼音节表：按候选自带的拼音键 + 声调串重建词读音，校正多音字。 */
    private Set<String> syllables;
    private boolean showReading = true;
    /** 注音候选小字显示什么：0 拼音、1 注音符号。 */
    private int zyReadMode = 0;
    /** 按下蓝底：按住时键面变蓝，松手立刻恢复（不再停留 5 秒）。 */
    private boolean visualFeedback = true;
    /** 机械键盘回弹：按住时键面轻微下沉，松手弹一下。与蓝底是两个独立开关。 */
    private boolean keyBounce;
    private Keyboard.Key bounceKey;
    private long bounceStart;
    /** 键盘高度倍率（0.70–1.50），设置页单独可调。 */
    private float keyScale = 1f;
    /** 平假名 / 片假名键盘在键上方显示罗马音。 */
    private boolean kanaRomaji = true;
    /** 日语假名键盘的小假名开关状态（开关键高亮用）。 */
    private boolean smallKana;
    /** 注音分区键盘：区域底色 / 区域边框开关。 */
    private boolean zoneTint = true;
    private boolean zoneBorder = true;
    /** 假名罗马音、注音拼音小字的颜色。 */
    private int romajiColor = 0xFF8FA3B3;
    private int hintColor = 0xFFC62828;
    /** 长按已经触发（用来把键面标签换成「。」并提示）。 */
    private boolean longHeld;
    private String flashText;
    private long flashUntil;

    /** 数字页左列那条可上下滑动的运算符条：滚动量、按下的那一格、拖动状态。 */
    private float opScrollPx;
    private int pressedOp = -1;
    private boolean opTouching;
    private boolean opDragging;
    private float opDragY0;
    private float opDragScroll0;
    private float opLastY;
    private long opLastT;
    private float opFlingV;
    private boolean opNeedSnap;
    private float opStripL, opStripT, opStripR, opStripB, opCellH;
    private OverScroller opScroller;
    private static final int OP_VISIBLE = 4;

    private static final int C_JP = 0xFF2E7D5B;

    /** 符号页横向滑动翻屏：起点、是否已经翻过、滑动后不再触发按键。 */
    private boolean symSwipeOn;
    private boolean symSwiped;
    private float symSwipeX0;
    private float symSwipeY0;

    private Listener listener;

    public ImeView(Context context) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
        actH = dp(40);
        barH = dp(46);
        gridRowH = dp(34);
        keyH = dp(52);
        gap = dp(5);
        pad = dp(4);
        rowGapExtra = dp(4);
        bottomPad = dp(3);
        textPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        opScroller = new OverScroller(context);
        candScroller = new OverScroller(context);
        setFocusable(false);
    }

    /** 运算符条的惯性滚动：OverScroller 每帧把位置推给 opScrollPx。 */
    @Override
    public void computeScroll() {
        if (candScroller != null && candScroller.computeScrollOffset()) {
            candScroll = candScroller.getCurrX();
            invalidate();
            return;
        }
        if (candNeedSnap) {
            candNeedSnap = false;
            snapCandScroll();
        }
        if (opScroller != null && opScroller.computeScrollOffset()) {
            opScrollPx = opScroller.getCurrY();
            invalidate();
            return;
        }
        if (opNeedSnap) {
            opNeedSnap = false;
            snapOpScroll();
        }
    }

    /** 松开手以后把候选栏对齐到某个候选的左边缘，不会停在半个词上。 */
    private void snapCandScroll() {
        if (candLeft.length == 0) {
            candScroll = 0f;
            invalidate();
            return;
        }
        float best = 0f;
        float bestD = Float.MAX_VALUE;
        for (int i = 0; i < candLeft.length; i++) {
            float d = Math.abs(candLeft[i] - candScroll);
            if (d < bestD) {
                bestD = d;
                best = candLeft[i];
            }
        }
        if (best > candScrollMax) best = candScrollMax;
        if (best < 0f) best = 0f;
        if (Math.abs(best - candScroll) < 1f) {
            candScroll = best;
            invalidate();
            return;
        }
        candScroller.startScroll(Math.round(candScroll), 0, Math.round(best - candScroll), 0, 180);
        postInvalidateOnAnimation();
    }

    /** 滑完松手后把滚动位置对齐到最近的整格，不会卡在半格上。 */
    private void snapOpScroll() {
        if (keyboard == null || keyboard.opStrip == null || opCellH <= 0) return;
        float stripH = opStripB - opStripT;
        float maxScroll = Math.max(0, keyboard.opStrip.size() * opCellH - stripH);
        float target = Math.round(opScrollPx / opCellH) * opCellH;
        if (target > maxScroll) target = maxScroll;
        if (target < 0) target = 0;
        if (Math.abs(target - opScrollPx) < 1f) {
            opScrollPx = target;
            invalidate();
            return;
        }
        opScroller.startScroll(0, Math.round(opScrollPx), 0, Math.round(target - opScrollPx), 120);
        postInvalidateOnAnimation();
    }

    public void setListener(Listener l) {
        this.listener = l;
    }

    private float dp(float v) {
        return v * density;
    }

    public void setKeyboard(Keyboard kb) {
        this.keyboard = kb;
        this.bounceKey = null;
        // 换键盘时运算符条回到顶部、取消按住状态
        opScrollPx = 0;
        pressedOp = -1;
        opTouching = false;
        opDragging = false;
        opCellH = 0;
        if (opScroller != null) opScroller.forceFinished(true);
        requestLayout();
        invalidate();
    }

    /** 当前是不是符号页（三屏共用同一套左右滑动翻屏手势）。 */
    private boolean isSymbolKb() {
        return keyboard != null && keyboard.name != null && keyboard.name.startsWith("符号");
    }

    /** 按下蓝底开关：关掉后按住键不再变蓝。 */
    public void setVisualFeedback(boolean on) {
        if (this.visualFeedback != on) {
            this.visualFeedback = on;
            invalidate();
        }
    }

    /** 机械回弹开关：与按下蓝底互不影响。 */
    public void setKeyBounce(boolean on) {
        if (this.keyBounce != on) {
            this.keyBounce = on;
            if (!on) bounceKey = null;
            invalidate();
        }
    }

    /** 键盘高度倍率：0.70 最矮，1.50 最高。 */
    public void setKeyScale(float scale) {
        float s = scale < 0.70f ? 0.70f : (scale > 1.50f ? 1.50f : scale);
        if (Math.abs(s - keyScale) < 0.001f) return;
        keyScale = s;
        keyH = dp(52) * s;
        requestLayout();
        invalidate();
    }

    /** 假名键盘键上方的罗马音标注。 */
    public void setKanaRomaji(boolean on) {
        if (this.kanaRomaji != on) {
            this.kanaRomaji = on;
            invalidate();
        }
    }

    /** 小假名开关状态：开的时候键面高亮，关的时候回到普通配色。 */
    public void setSmallKana(boolean on) {
        if (this.smallKana != on) {
            this.smallKana = on;
            invalidate();
        }
    }

    /** 注音分区四块区域的底色：声母 / 韵母 / 整体认读 / 声调（颜色可在设置里调）。 */
    private int zoneColor(int zone) {
        switch (zone) {
            case Keyboard.ZONE_INITIAL: return zoneTintColors[0];
            case Keyboard.ZONE_FINAL: return zoneTintColors[1];
            case Keyboard.ZONE_WHOLE: return zoneTintColors[2];
            case Keyboard.ZONE_TONE: return zoneTintColors[3];
            default: return C_KEY;
        }
    }

    /** 分区边框用的深一号颜色，和底色同色系（可在设置里改）。 */
    private int zoneBorderColor(int zone) {
        switch (zone) {
            case Keyboard.ZONE_INITIAL: return zoneBorderColors[0];
            case Keyboard.ZONE_FINAL: return zoneBorderColors[1];
            case Keyboard.ZONE_WHOLE: return zoneBorderColors[2];
            case Keyboard.ZONE_TONE: return 0xFFC98A97;
            default: return C_DIVIDER;
        }
    }

    /** 注音分区配色（用户在设置里选的，含透明度）。下标：0 声母 1 韵母 2 整体认读 3 声调。 */
    private final int[] zoneTintColors = {
            0xFFEAF2FB, 0xFFEBF5EC, 0xFFFCF3E4, 0xFFFBEDF0};
    private final int[] zoneBorderColors = {0xFF7FA9CE, 0xFF6FAF7C, 0xFFC79C4E};

    public void setZoneColors(int[] tint, int[] border) {
        if (tint != null) {
            for (int i = 0; i < zoneTintColors.length && i < tint.length; i++) {
                zoneTintColors[i] = tint[i];
            }
        }
        if (border != null) {
            for (int i = 0; i < zoneBorderColors.length && i < border.length; i++) {
                zoneBorderColors[i] = border[i];
            }
        }
        invalidate();
    }

    /** 注音分区键盘的底色 / 边框开关。 */
    public void setZoneStyle(boolean tint, boolean border) {
        if (zoneTint != tint || zoneBorder != border) {
            zoneTint = tint;
            zoneBorder = border;
            invalidate();
        }
    }

    /** 候选小字里「已经打出来的那一截」的高亮色（可在设置里改）。 */
    private int hiColor = 0xFF3E7CB1;

    /** 笔画候选上方的拼音颜色（笔画输入没有「已打出」这一说，整串一个色）。 */
    private int strokeReadColor = 0xFF3E7CB1;

    public void setStrokeReadColor(int color) {
        if (strokeReadColor != color) {
            strokeReadColor = color;
            invalidate();
        }
    }

    /** 选中那条候选的底色 / 文字颜色（可在设置里改）。 */
    private int selBgColor = 0xFF3E7CB1;
    private int selFgColor = 0xFFFFFFFF;

    public void setSelectedColors(int bg, int fg) {
        if (selBgColor != bg || selFgColor != fg) {
            selBgColor = bg;
            selFgColor = fg;
            invalidate();
        }
    }

    /** 选中项上方小字里「没打出来的那一截」：文字色压到四成透明。 */
    private int selectedWeakColor() {
        return (selFgColor & 0x00FFFFFF) | 0x66000000;
    }

    public void setHighlightColor(int color) {
        if (hiColor != color) {
            hiColor = color;
            invalidate();
        }
    }

    /** 假名上方罗马音的颜色。 */
    public void setRomajiColor(int color) {
        if (romajiColor != color) {
            romajiColor = color;
            invalidate();
        }
    }

    /** 注音符号右下角拼音小字的颜色。 */
    public void setHintColor(int color) {
        if (hintColor != color) {
            hintColor = color;
            invalidate();
        }
    }

    public void setCandidates(List<Candidate> list, String composingText) {
        this.candidates = list == null ? new ArrayList<Candidate>() : list;
        this.composing = composingText == null ? "" : composingText;
        this.selected = 0;
        this.status = null;
        this.clipPreview = null;
        this.clipLastTap = -1;
        this.gridPage = 0;
        this.candScroll = 0f;      // 换了候选就从第一个重新排
        this.candScrollMax = 0f;
        this.candDragging = false;
        this.candTouchZone = false;
        if (candScroller != null) candScroller.forceFinished(true);
        requestLayout();      // 字条显隐 / 候选数量变化都要重新测量高度
        invalidate();
    }

    /** 候选上方小字倍率：1.0 以上，设置页按百分比给。 */
    public void setReadScale(float scale) {
        float v = scale < 1f ? 1f : scale;
        if (Math.abs(v - readScale) > 0.001f) {
            readScale = v;
            invalidate();
        }
    }

    /** 组字条光标位置（缓冲内下标），由输入法主逻辑同步过来。 */
    public void setCaretPos(int pos, boolean active) {
        caretPos = pos;
        caretActive = active;
    }

    /** 键面右下角小字倍率：1.0 以上；绘制时还会被该键正文字号压住。 */
    public void setHintScale(float scale) {
        float v = scale < 1f ? 1f : scale;
        if (Math.abs(v - hintScale) > 0.001f) {
            hintScale = v;
            invalidate();
        }
    }

    /** 声调筛选的高亮状态（真正过滤候选由 FrostIME 做）。 */
    public void setToneFilter(int tone) {
        int v = tone < 0 || tone > 4 ? 0 : tone;
        if (toneFilter != v) {
            toneFilter = v;
            invalidate();
        }
    }

    public void setStatus(String s) {
        this.status = s;
        invalidate();
    }

    public String getStatus() {
        return status;
    }

    public void setExpanded(boolean e) {
        if (expanded != e) {
            expanded = e;
            requestLayout();
            invalidate();
        }
    }

    /** 展开面板「单 / 词」筛选状态。 */
    public void setSingleFilter(boolean on) {
        if (singleFilter != on) {
            singleFilter = on;
            requestLayout();
            invalidate();
        }
    }

    /** 是否显示「单 / 词」筛选（只有拼音方案、非英文、非剪贴板、非数字页）。 */
    private boolean showFilters() {
        return !englishMode && schemeIndex == 0 && !clipMode;
    }

    public boolean isExpanded() {
        return expanded;
    }

    /** 当前模式：0 全拼 / 1 注音 / 2 日语，english 为英文状态。 */
    public void setMode(int scheme, boolean english) {
        this.schemeIndex = scheme;
        this.englishMode = english;
        invalidate();
    }

    /** 英文大写锁定：123 键右上角画 ⇧ 并整键变蓝。 */
    public void setEnglishUpper(boolean upper) {
        if (this.englishUpper != upper) {
            this.englishUpper = upper;
            invalidate();
        }
    }

    /** 候选字上方的读音小字：拼音方案给带调拼音，注音给拼音或注音符号（设置里可切），日语给罗马音。 */
    public void setReading(Readings r, boolean enabled) {
        this.readings = r;
        this.showReading = enabled;
        invalidate();
    }

    /** 注入全拼音节表，供按词重建带调读音用。 */
    public void setSyllables(Set<String> syls) {
        this.syllables = syls;
        invalidate();
    }

    /** 注音模式候选小字：0 拼音（默认）、1 注音符号。 */
    public void setZyReadMode(int mode) {
        if (zyReadMode != mode) {
            zyReadMode = mode;
            invalidate();
        }
    }

    /** 按当前方案取一个词的读音；取不到返回 null。 */
    /** 带调拼音字母 → 不带调的字形，只用来比较「打了几个字母」。 */
    private static char baseLetter(char c) {
        switch (c) {
            case 'ā': case 'á': case 'ǎ': case 'à': return 'a';
            case 'ē': case 'é': case 'ě': case 'è': return 'e';
            case 'ī': case 'í': case 'ǐ': case 'ì': return 'i';
            case 'ō': case 'ó': case 'ǒ': case 'ò': return 'o';
            case 'ū': case 'ú': case 'ǔ': case 'ù': return 'u';
            case 'ǖ': case 'ǘ': case 'ǚ': case 'ǜ': case 'ü': return 'u';
            case 'ń': case 'ň': case 'ǹ': return 'n';
            default: return Character.toLowerCase(c);
        }
    }

    /**
     * 候选小字里「已经打出来」的那一截有多长：返回 reading 中需要高亮的字符个数。
     * 拼音 / 罗马音按字母逐个比（忽略空格、忽略声调），注音直接比符号。
     */
    private int readingHiLen(String reading) {
        if (reading == null || composing == null || composing.isEmpty()) return 0;
        boolean zySymbol = schemeIndex == 1 && zyReadMode == 1;
        String typed = composing;
        if (schemeIndex == 1 && !zySymbol) {   // 注音显示拼音：把已打完的注音音节换算成拼音再比
            typed = pinyinOfComposing(composing);
            if (typed.isEmpty()) return 0;
        }
        int i = 0, j = 0;
        while (i < reading.length()) {
            char rc = reading.charAt(i);
            if (rc == ' ' || rc == '\'' || rc == '’') {   // 分隔符照留下，不算没打到
                i++;
                continue;
            }
            if (zySymbol && isZyToneMark(rc)) {           // 注音声调符号不参与比
                i++;
                continue;
            }
            while (j < typed.length()) {
                char tc = typed.charAt(j);
                if (tc == ' ' || tc == '\'') {
                    j++;
                    continue;
                }
                if (zySymbol && tc >= '1' && tc <= '5') {  // 输入里的声调数字也不参与比
                    j++;
                    continue;
                }
                break;
            }
            if (j >= typed.length()) break;
            boolean same = zySymbol
                    ? rc == typed.charAt(j)
                    : baseLetter(rc) == baseLetter(typed.charAt(j));
            if (!same) break;
            i++;
            j++;
        }
        return i;
    }

    /** 注音声调符号：ˉ ˊ ˇ ˋ ˙。 */
    private static boolean isZyToneMark(char c) {
        return c == '\u02C9' || c == '\u02CA' || c == '\u02C7' || c == '\u02CB' || c == '\u02D9';
    }

    /** 把输入的注音串按音节换成拼音字母（声调数字略去），只换完整音节。 */
    private static String pinyinOfComposing(String s) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            int len = 0;
            int max = Math.min(4, s.length() - i);
            for (int L = max; L >= 1; L--) {
                if (ZhuyinSyllables.isSyllable(s.substring(i, i + L))) {
                    len = L;
                    break;
                }
            }
            if (len == 0) break;
            String p = ZhuyinSyllables.pinyin(s.substring(i, i + len));
            if (p == null) break;
            sb.append(p);
            i += len;
        }
        return sb.toString();
    }

    private String readingOf(Candidate c) {
        if (!showReading || c == null) return null;
        if (schemeIndex == 2) {
            // 日语：候选带着假名读音，转成罗马音
            String kana = c.tones;
            if (kana == null || kana.isEmpty()) return null;
            String romaji = JapaneseEngine.toRomaji(kana);
            return romaji.isEmpty() ? null : romaji;
        }
        if (schemeIndex == 1) {
            // 注音：设置里可选「拼音」或「注音符号」小字
            if (zyReadMode == 1) {
                String z = readings == null ? null : readings.word(c.word, 2);
                return z == null || z.isEmpty() ? null : z;
            }
            String p = readings == null ? null : readings.word(c.word, 1);
            if (p != null && !p.isEmpty()) return p;
            return c.pinyin == null || c.pinyin.isEmpty() ? null : c.pinyin;
        }
        // 拼音：优先按候选自带的拼音键 + 声调串重建（多音字在词里的实际读音）；
        // 重建不出来再退回单字读音表。
        String fromDict = readingFromDict(c);
        if (fromDict != null) return fromDict;
        if (readings == null) return null;
        return readings.word(c.word, 1);
    }

    private static final String[] TONE_MARKS = {
            "āáǎà", "ōóǒò", "ēéěè", "īíǐì", "ūúǔù", "ǖǘǚǜ"
    };
    private static final char[] TONE_BASE = {'a', 'o', 'e', 'i', 'u', 'ü'};

    /** 给无声调音节加声调符号；tone 为 '1'~'4'，其它按轻声处理。 */
    private static String addToneMark(String syl, char tone) {
        if (syl == null || syl.isEmpty()) return null;
        String s = syl.replace("v", "ü");
        if (tone < '1' || tone > '4') return s;
        int idx = -1;
        if (s.indexOf('a') >= 0) idx = s.indexOf('a');
        else if (s.indexOf('o') >= 0) idx = s.indexOf('o');
        else if (s.indexOf('e') >= 0) idx = s.indexOf('e');
        else if (s.indexOf("iu") >= 0) idx = s.indexOf('u', s.indexOf("iu"));
        else if (s.indexOf("ui") >= 0) idx = s.indexOf('i', s.indexOf("ui"));
        else {
            for (int i = 0; i < s.length(); i++) {
                if (s.charAt(i) == 'i' || s.charAt(i) == 'u' || s.charAt(i) == 'ü') {
                    idx = i;
                    break;
                }
            }
        }
        if (idx < 0) return s;
        char base = s.charAt(idx);
        int bi = -1;
        for (int k = 0; k < TONE_BASE.length; k++) {
            if (TONE_BASE[k] == base) { bi = k; break; }
        }
        if (bi < 0) return s;
        char marked = TONE_MARKS[bi].charAt(tone - '1');
        return s.substring(0, idx) + marked + s.substring(idx + 1);
    }

    /**
     * 用词库记录里的拼音键（无声调音节连写）和逐字声调串重建带调读音；
     * 音节数与字数对不上、或音节切不出来时返回 null，交给单字表兜底。
     */
    private String readingFromDict(Candidate c) {
        if (syllables == null || c == null || c.key == null || c.key.isEmpty()) return null;
        if (c.tones == null || c.tones.length() != c.word.length()) return null;
        List<String> syls = PinyinEngine.split(c.key, syllables);
        if (syls == null || syls.size() != c.word.length()) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < c.word.length(); i++) {
            String marked = addToneMark(syls.get(i), c.tones.charAt(i));
            if (marked == null) return null;
            if (sb.length() > 0) sb.append(' ');
            sb.append(marked);
        }
        return sb.toString();
    }

    /** 日语键盘形态：0 罗马字 / 1 平假名五十音 / 2 片假名五十音。 */
    public void setJpMode(int mode) {
        this.jpMode = mode;
        invalidate();
    }

    /** 剪贴板面板模式：格子带删除角标，候选栏给「清空」按钮。 */
    public void setClipMode(boolean on) {
        if (clipMode != on) {
            clipMode = on;
            if (!on) {
                clipPreview = null;
                clipLastTap = -1;
            }
            invalidate();
        }
    }

    /** 切换后短暂提示当前模式。 */
    public void flash(String text) {
        this.flashText = text;
        this.flashUntil = System.currentTimeMillis() + 1000;
        handler.removeCallbacks(clearFlash);
        handler.postDelayed(clearFlash, 1400);
        invalidate();
    }

    private final Runnable clearFlash = new Runnable() {
        @Override
        public void run() {
            flashText = null;
            invalidate();
        }
    };

    /** 笔画键盘只有 3 行，为与 4 行的拼音键盘等高，行高按 4 行总高均分。 */
    private boolean isStrokeKb() {
        return keyboard != null && "笔画".equals(keyboard.name);
    }

    private float rowHeight() {
        if (isStrokeKb()) {
            float total = 4 * keyH + 3 * gap;
            return (total - 2 * gap) / 3f;
        }
        return keyH;
    }

    private float keyboardHeight() {
        int rows = keyboard == null ? 4 : keyboard.rowCount();
        if (isStrokeKb()) {
            return 4 * keyH + 3 * gap;
        }
        return rows * keyH + (rows - 1) * gap;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int rows = keyboard == null ? 4 : keyboard.rowCount();
        float gaps = Math.max(0, rows - 1) * gap;
        // 组字气泡是悬浮层，不占任何布局高度：按键高度、动作栏、候选栏、键盘总高与无字条时完全一致。
        keyH = dp(52) * keyScale;
        float fixed = actH + gridHeight() + barH + pad * 2 + rowGapExtra + bottomPad;
        float desired = fixed + rows * keyH + gaps;
        setMeasuredDimension(w, (int) Math.ceil(desired));
    }

    private int gridPages() {
        int n = candidates == null ? 0 : candidates.size();
        if (n <= GRID_PER_PAGE) return 1;
        return (n + GRID_PER_PAGE - 1) / GRID_PER_PAGE;
    }

    /** 动作栏顶边：整个键盘的最上沿，不预留任何字条行。 */
    private float actTop() {
        return 0;
    }

    /** 展开面板占的高度：三行候选，翻页靠左右滑动，不再单独占一条分页栏。 */
    private float gridHeight() {
        if (!expanded) return 0;
        return gridRowH * GRID_ROWS;
    }

    /** 展开面板的顶边：动作键行下面、预选框上面。 */
    private float gridTop() {
        return actH;
    }

    /** 预选框的顶边：动作键行 + 展开面板。 */
    private float barTop() {
        return actH + gridHeight();
    }

    /** 组字条是否出现：拼音 / 注音、非英文、非剪贴板、未展开候选面板、串非空。 */
    private boolean stripVisible() {
        return !clipMode && !englishMode && !expanded
                && (schemeIndex == 0 || schemeIndex == 1)
                && composing != null && !composing.isEmpty();
    }

    /** 字条不占行高，恒为 0：气泡只是浮在候选栏上的一层，绝不挤动键盘和动作栏。 */
    private float stripHeight() {
        return 0;
    }

    /** 字条胶囊命中矩形：x, y, w, h。 */
    private final float[] stripChipRect = new float[4];
    private boolean stripChipOn = false;
    /** 气泡触摸状态：长按进入放大拖动态，左右滑动移动光标。 */
    private boolean bubbleTouch;
    private boolean bubbleLong;
    private float bubbleLastX;
    private float bubblePadX;
    private float bubbleTextSize;

    private boolean bubbleBig() {
        return bubbleLong;
    }

    private float bubbleH() {
        return bubbleBig() ? dp(34) : dp(26);
    }

    private float bubbleFont() {
        return bubbleBig() ? dp(17) : dp(14);
    }

    private float bubblePadX() {
        return bubbleBig() ? dp(16) : dp(12);
    }

    /** 气泡宽度：按拼音串文字量换算，封顶到屏宽的 45%。 */
    private float stripChipWidth() {
        if (!stripVisible()) return 0;
        String t = composing == null ? "" : composing;
        textPaint.setTextSize(bubbleFont());
        float pad = bubblePadX();
        float w = textPaint.measureText(t) + pad * 2;
        return Math.min(w, getWidth() * 0.45f);
    }

    private float barBottom() {
        return barTop() + barH;
    }

    private float keyboardTop() {
        return barBottom();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(C_BG);
        // 命中框在任何绘制之前清空，避免展开面板格子的框被候选栏绘制清掉。
        candBoxes.clear();
        candDelBoxes.clear();
        drawActionRow(canvas);
        if (expanded && !candidates.isEmpty()) drawGrid(canvas);
        drawCandidates(canvas);
        drawStrip(canvas);
        drawKeyboard(canvas);
        drawFlash(canvas);
    }

    /** 声调键只在拼音 / 注音、非英文、非剪贴板时出现。 */
    private boolean showTones() {
        return !englishMode && !clipMode && (schemeIndex == 0 || schemeIndex == 1);
    }

    /** 动作键行：设置 · 声调 1–4 · 字词 · 全删 · 全复 · 粘贴板 · ×，从左到右。 */
    private void drawActionRow(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        // 动作栏底色与预选（候选）栏一致；按键自身底色不变。
        paint.setColor(C_BAR);
        float actTop = actTop();
        canvas.drawRect(0, actTop, getWidth(), actTop + actH, paint);
        paint.setColor(C_DIVIDER);
        canvas.drawRect(0, actTop + actH - dp(1), getWidth(), actTop + actH, paint);

        actBoxes.clear();
        List<float[]> spec = new ArrayList<float[]>();
        spec.add(new float[]{ACT_SET, 1.00f});
        if (clipMode) {
            // 剪贴板面板：清空历史 / 撤回 / 收起，其余和文字输入相关的键先收起来
            spec.add(new float[]{ACT_CLIP_CLEAR, 1.05f});
            spec.add(new float[]{ACT_UNDO, 0.86f});
            spec.add(new float[]{ACT_PASTE, 1.20f});
            spec.add(new float[]{ACT_CLOSE, 0.80f});
        } else {
            if (showTones()) {
                for (int t = 1; t <= 4; t++) {
                    spec.add(new float[]{ACT_TONE_BASE + t - 1, 0.52f});
                }
            }
            if (showFilters()) spec.add(new float[]{ACT_WORD, 0.72f});
            spec.add(new float[]{ACT_SEL_ALL, 0.98f});
            spec.add(new float[]{ACT_UNDO, 0.66f});
            spec.add(new float[]{ACT_PASTE, 1.00f});
            spec.add(new float[]{ACT_CLOSE, 0.66f});
        }

        float sum = 0f;
        for (int m = 0; m < spec.size(); m++) sum += spec.get(m)[1];
        float gapAct = dp(3);
        float avail = getWidth() - pad * 2 - gapAct * (spec.size() - 1);
        float unit = avail / sum;
        float x = pad;
        float top = actTop() + dp(5);
        float h = actH - dp(10);
        for (int m = 0; m < spec.size(); m++) {
            int id = (int) spec.get(m)[0];
            float w = unit * spec.get(m)[1];
            drawActButton(canvas, x, top, w, h, id);
            actBoxes.add(new float[]{x, top, w, h, id});
            x += w + gapAct;
        }
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawActButton(Canvas canvas, float x, float top, float w, float h, int id) {
        String label;
        int bg = C_KEY_SPECIAL;
        int fg = C_TEXT;
        switch (id) {
            case ACT_SET: label = "设置"; break;
            case ACT_TONE_BASE:
            case ACT_TONE_BASE + 1:
            case ACT_TONE_BASE + 2:
            case ACT_TONE_BASE + 3:
                label = String.valueOf(id);
                if (toneFilter == id) { bg = C_ACCENT; fg = 0xFFFFFFFF; }
                break;
            case ACT_WORD:
                label = singleFilter ? "字" : "词";
                if (singleFilter) { bg = C_ACCENT; fg = 0xFFFFFFFF; }
                break;
            case ACT_DEL_ALL: label = "全删"; break;
            case ACT_SEL_ALL: label = "全复"; break;
            case ACT_PASTE:
                label = clipMode ? "收起" : "粘板";
                if (clipMode) { bg = C_ACCENT; fg = 0xFFFFFFFF; }
                break;
            case ACT_UNDO: label = "撤"; break;
            case ACT_CLIP_CLEAR:
                label = "清空";
                bg = 0xFFF6DCDC;
                fg = 0xFFB03A3B;
                break;
            case ACT_CLOSE: label = "×"; break;
            default: label = ""; break;
        }
        if (actPressed == id) {
            bg = C_ACCENT;
            fg = 0xFFFFFFFF;
        }
        tmpRect.set(x, top, x + w, top + h);
        paint.setColor(bg);
        canvas.drawRoundRect(tmpRect, dp(5), dp(5), paint);
        textPaint.setTextSize(id == ACT_CLOSE ? dp(16)
                : (label.length() >= 3 ? dp(8.6f) : (label.length() == 2 ? dp(10f) : dp(12.5f))));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(fg);
        canvas.drawText(label, tmpRect.centerX(), b2Text(tmpRect), textPaint);
        if (id == ACT_SEL_ALL) {
            // 「全复」长按 = 全删，右上角标一下
            textPaint.setColor(0xFF8C99A6);
            textPaint.setTextSize(dp(8));
            textPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText("删", tmpRect.right - dp(3), tmpRect.top + dp(10), textPaint);
            textPaint.setTextAlign(Paint.Align.CENTER);
        }
    }

    private int actAt(float x, float y) {
        for (int i = 0; i < actBoxes.size(); i++) {
            float[] b = actBoxes.get(i);
            if (x >= b[0] && x <= b[0] + b[2] && y >= b[1] && y <= b[1] + b[3]) {
                return (int) b[4];
            }
        }
        return -1;
    }

    /** 预选框里的提示文字（空候选、状态、剪贴板标题）。 */
    private void drawBarHint(Canvas canvas, String text, int color, float x) {
        textPaint.setColor(color);
        textPaint.setTextSize(dp(13));
        textPaint.setTextAlign(Paint.Align.LEFT);
        float baseline = (barTop() + barBottom()) / 2 - (textPaint.ascent() + textPaint.descent()) / 2;
        canvas.drawText(fitText(text, getWidth() - x - dp(12)), x, baseline, textPaint);
    }

    /** 组字气泡胶囊：浮在预选栏右端的悬浮气泡；长按放大，手指拖动移动光标。 */
    private void drawStrip(Canvas canvas) {
        if (!stripVisible()) { stripChipOn = false; return; }
        float w = stripChipWidth();
        float h = bubbleH();
        float fs = bubbleFont();
        float pad = bubblePadX();
        float l = getWidth() - dp(7) - w;
        float top = barTop() + (barH - h) / 2f;
        stripChipRect[0] = l; stripChipRect[1] = top;
        stripChipRect[2] = w; stripChipRect[3] = h;
        stripChipOn = true;
        bubblePadX = pad;
        bubbleTextSize = fs;

        float r = h / 2f;
        // 投影：长按放大态影子稍重一点，突出浮层
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(bubbleBig() ? 0x3322384D : 0x1F22384D);
        tmpRect.set(l + dp(1), top + dp(2), l + w + dp(1), top + h + dp(3));
        canvas.drawRoundRect(tmpRect, r, r, paint);
        // 气泡主体
        paint.setColor(0xFFFFFFFF);
        tmpRect.set(l, top, l + w, top + h);
        canvas.drawRoundRect(tmpRect, r, r, paint);
        // 描边
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setColor(bubbleBig() ? C_ACCENT : 0xFFA8C6DE);
        canvas.drawRoundRect(tmpRect, r, r, paint);
        paint.setStyle(Paint.Style.FILL);

        String t = composing;
        int caret = caretActive ? Math.max(0, Math.min(caretPos, t.length())) : t.length();
        String before = t.substring(0, caret);
        String after = t.substring(caret);
        textPaint.setTextSize(fs);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(C_TEXT);
        float baseline = top + h / 2f - (textPaint.ascent() + textPaint.descent()) / 2f;
        float textX = l + pad;
        float wBefore = textPaint.measureText(before);
        canvas.drawText(before, textX, baseline, textPaint);
        if (caretActive) {
            float caretX = textX + wBefore;
            paint.setColor(C_ACCENT);
            canvas.drawRect(caretX - dp(1), top + dp(5), caretX + dp(1), top + h - dp(5), paint);
            canvas.drawText(after, caretX + dp(3), baseline, textPaint);
        } else {
            canvas.drawText(after, textX + wBefore, baseline, textPaint);
        }
    }

    /** 按点击的 x 坐标换算最近的字母下标（光标落在该字母前），与气泡内文字起点一致。 */
    private int caretIndexAt(float x) {
        String t = composing == null ? "" : composing;
        textPaint.setTextSize(bubbleTextSize);
        float textX = (stripChipOn ? stripChipRect[0] : 0) + bubblePadX;
        float acc = 0;
        for (int i = 0; i < t.length(); i++) {
            float cw = textPaint.measureText(t, i, i + 1);
            if (x < textX + acc + cw / 2f) return i;
            acc += cw;
        }
        return t.length();
    }

    private void drawCandidates(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(C_BAR);
        canvas.drawRect(0, barTop(), getWidth(), barBottom(), paint);
        paint.setColor(C_DIVIDER);
        canvas.drawRect(0, barBottom() - dp(1), getWidth(), barBottom(), paint);

        // 组字气泡浮在右端：候选只画到气泡左边为止，不缩进、不右移，超出的部分照样能滑出来
        boolean clipped = stripVisible();
        if (clipped) {
            canvas.save();
            canvas.clipRect(0f, barTop(), candViewportRight(), barBottom());
        }

        if (status != null) {
            drawBarHint(canvas, status, C_HINT, dp(12));
            if (clipped) canvas.restore();
            return;
        }
        if (clipMode) {
            if (clipPreview != null && !clipPreview.isEmpty()) {
                drawBarHint(canvas, clipPreview, C_TEXT, dp(12));
            } else {
                drawBarHint(canvas, "剪贴板 · " + candidates.size() + " 条（双击条目粘贴）",
                        C_HINT, dp(12));
            }
        }
        if (candidates.isEmpty()) {
            // 组字气泡已经显示这串字了，别再在左边重复一遍
            if (!clipMode && !clipped) {
                drawBarHint(canvas, composing.isEmpty() ? modeHint() : composing, C_HINT, dp(12));
            }
            if (clipped) canvas.restore();
            return;
        }
        // 剪贴板面板条目已在格子里展示，候选栏只留预览/提示文字，不再重复画芯片。
        if (!clipMode) {
            layoutCandidates();
            drawCandItems(canvas);
            if (candScroll > dp(1)) drawEdgeArrow(canvas, true);
            if (candScroll < candScrollMax - dp(1)) drawEdgeArrow(canvas, false);
        }
        if (clipped) canvas.restore();
    }

    /** 候选可视区右边界：气泡出现时收到气泡左边，气泡隐藏时就是整行宽。 */
    private float candViewportRight() {
        if (!stripVisible()) return getWidth();
        float bubbleLeft = getWidth() - dp(7) - stripChipWidth();
        return Math.max(dp(80), bubbleLeft - dp(3));
    }

    /** 先量一遍每个候选的宽，得到总宽与可滚动范围。 */
    private void layoutCandidates() {
        int n = candidates.size();
        if (candLeft.length != n) {
            candLeft = new float[n];
            candWide = new float[n];
        }
        float x = dp(7);
        for (int i = 0; i < n; i++) {
            Candidate c = candidates.get(i);
            String reading = readingOf(c);
            textPaint.setTextSize(dp(15));
            float tw = textPaint.measureText(c.word) + dp(18);
            if (reading != null) {
                float rs = dp(9) * readScale;
                if (rs > dp(16)) rs = dp(16);
                textPaint.setTextSize(rs);
                float rw = textPaint.measureText(reading) + dp(14);
                if (rw > tw) tw = rw;
            }
            if (tw < dp(36)) tw = dp(36);
            if (clipMode && tw < dp(64)) tw = dp(64);
            candLeft[i] = x;
            candWide[i] = tw;
            x += tw;
        }
        float contentW = x + dp(7);
        candScrollMax = Math.max(0f, contentW - candViewportRight());
        if (candScroll > candScrollMax) candScroll = candScrollMax;
        if (candScroll < 0f) candScroll = 0f;
    }

    private void drawCandItems(Canvas canvas) {
        float top = barTop() + dp(2);
        float h = barH - dp(5);
        float width = getWidth();
        for (int i = 0; i < candidates.size(); i++) {
            float left = candLeft[i] - candScroll;
            float tw = candWide[i];
            if (left > width) break;
            if (left + tw < 0f) continue;
            Candidate c = candidates.get(i);
            String word = c.word;
            String reading = readingOf(c);

            tmpRect.set(left + dp(1), top, left + tw - dp(1), top + h);
            if (i == selected) {
                paint.setColor(selBgColor);
                canvas.drawRoundRect(tmpRect, dp(6), dp(6), paint);
            }
            boolean hasRead = reading != null;
            float wordBaseline;
            if (hasRead) {
                float rs = dp(9) * readScale;
                if (rs > dp(16)) rs = dp(16);
                textPaint.setTextSize(rs);
                textPaint.setTextAlign(Paint.Align.LEFT);
                String disp = fitText(reading, tw - dp(12));
                int hi = Math.min(readingHiLen(reading), disp.length());
                int strong = i == selected ? selFgColor : hiColor;
                int weak = i == selected ? selectedWeakColor() : C_HINT;
                if (schemeIndex == 3) {
                    strong = i == selected ? selFgColor : strokeReadColor;
                    weak = strong;
                }
                float ry = top + dp(2) - textPaint.ascent();
                if (hi > 0) {
                    textPaint.setColor(strong);
                    canvas.drawText(disp.substring(0, hi), left + dp(7), ry, textPaint);
                }
                if (hi < disp.length()) {
                    float off = textPaint.measureText(disp.substring(0, hi));
                    textPaint.setColor(weak);
                    canvas.drawText(disp.substring(hi), left + dp(7) + off, ry, textPaint);
                }
                textPaint.setTextSize(dp(15));
                wordBaseline = tmpRect.bottom - dp(7) - textPaint.descent();
            } else {
                textPaint.setTextSize(dp(15));
                wordBaseline = tmpRect.centerY() - (textPaint.ascent() + textPaint.descent()) / 2;
            }
            textPaint.setColor(i == selected ? selFgColor : C_TEXT);
            textPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(fitText(word, tw - (clipMode ? dp(24) : dp(14))), left + dp(8),
                    wordBaseline, textPaint);
            candBoxes.add(new float[]{left + dp(1), top, tw - dp(2), h, i});

            if (clipMode) {
                textPaint.setColor(i == selected ? selFgColor : 0xFFB03A3B);
                textPaint.setTextSize(dp(11));
                textPaint.setTextAlign(Paint.Align.RIGHT);
                canvas.drawText("×", left + tw - dp(6), top + dp(13), textPaint);
                textPaint.setTextAlign(Paint.Align.LEFT);
                candDelBoxes.add(new float[]{left + tw - dp(20), top, dp(20), dp(22), i});
            }
        }
    }

    /** 预选框两端的滚动提示箭头（不响应点击）；右端箭头贴在气泡左边。 */
    private void drawEdgeArrow(Canvas canvas, boolean leftSide) {
        textPaint.setTextSize(dp(14));
        textPaint.setColor(0x998FA3B3);
        textPaint.setTextAlign(leftSide ? Paint.Align.LEFT : Paint.Align.RIGHT);
        canvas.drawText(leftSide ? "\u2039" : "\u203a",
                leftSide ? dp(1) : candViewportRight() - dp(1),
                (barTop() + barBottom()) / 2 + dp(5), textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    /** 动作键的具体行为：设置 / 声调 / 字词 / 全删 / 全复 / 粘贴板 / ×。 */
    private void onAct(int id) {
        if (listener == null) return;
        switch (id) {
            case ACT_SET:
                listener.onKey(new Keyboard.Key("设置", null, Keyboard.A_SETTINGS, 1f, null));
                break;
            case ACT_TONE_BASE:
            case ACT_TONE_BASE + 1:
            case ACT_TONE_BASE + 2:
            case ACT_TONE_BASE + 3: {
                int tone = id;                       // 1–4
                toneFilter = (toneFilter == tone) ? 0 : tone;
                listener.onToneFilter(toneFilter);
                break;
            }
            case ACT_WORD:
                listener.onToggleSingle();
                break;
            case ACT_DEL_ALL:
                listener.onSelectAllDelete();
                break;
            case ACT_SEL_ALL:
                listener.onKey(new Keyboard.Key("全复", null, Keyboard.A_SELECT_ALL, 1f, null));
                break;
            case ACT_PASTE:
                listener.onKey(new Keyboard.Key("粘板", null, Keyboard.A_CLIP, 1f, null));
                break;
            case ACT_UNDO:
                listener.onUndo();
                break;
            case ACT_CLIP_CLEAR:
                listener.onClipClear();
                break;
            case ACT_CLOSE:
                listener.onKey(new Keyboard.Key("\u00d7", null, Keyboard.A_CLOSE_IME, 1f, null));
                break;
            default:
                break;
        }
    }

    private String modeHint() {
        if (englishMode) return "English";
        if (schemeIndex == 2) return "日本語入力";
        if (schemeIndex == 1) return "ㄅㄆㄇ 注音輸入";
        if (schemeIndex == 3) return "笔画输入";
        return "拼音輸入";
    }

    /** 切换后 1.4 秒居中提示，明确当前处于哪个模式。 */
    private void drawFlash(Canvas canvas) {
        if (flashText == null) return;
        if (System.currentTimeMillis() > flashUntil) {
            flashText = null;
            return;
        }
        textPaint.setTextSize(dp(13));
        textPaint.setTextAlign(Paint.Align.CENTER);
        float tw = textPaint.measureText(flashText) + dp(24);
        float h = dp(26);
        float cx = getWidth() / 2f;
        // 画在上方的动作键行里，不压候选（切换「字词」时以前正好盖住候选）
        float top = actTop() + (actH - h) / 2f;
        tmpRect.set(cx - tw / 2, top, cx + tw / 2, top + h);
        paint.setColor(0xF23E7CB1);
        canvas.drawRoundRect(tmpRect, h / 2, h / 2, paint);
        textPaint.setColor(0xFFFFFFFF);
        float baseline = tmpRect.centerY() - (textPaint.ascent() + textPaint.descent()) / 2;
        canvas.drawText(flashText, cx, baseline, textPaint);
    }

    /** 把文字裁到能塞进给定宽度，超出部分用省略号。 */
    private String fitText(String s, float maxW) {
        if (s == null) return "";
        if (textPaint.measureText(s) <= maxW) return s;
        int n = s.length();
        while (n > 1) {
            n--;
            String cut = s.substring(0, n) + "…";
            if (textPaint.measureText(cut) <= maxW) return cut;
        }
        return s.substring(0, 1);
    }

    private void drawGrid(Canvas canvas) {
        float top = gridTop();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFECF1F6);
        canvas.drawRect(0, top, getWidth(), top + gridHeight(), paint);
        paint.setColor(C_DIVIDER);
        canvas.drawRect(0, top + gridHeight() - dp(1), getWidth(),
                top + gridHeight(), paint);

        float cellW = getWidth() / GRID_COLS;
        int start = gridPage * GRID_PER_PAGE;
        for (int n = 0; n < GRID_PER_PAGE; n++) {
            int i = start + n;
            if (i >= candidates.size()) break;
            int col = n % GRID_COLS;
            int row = n / GRID_COLS;
            tmpRect.set(col * cellW + dp(2), top + row * gridRowH + dp(2),
                    (col + 1) * cellW - dp(2), top + (row + 1) * gridRowH - dp(2));
            if (i == selected) {
                paint.setColor(selBgColor);
            } else {
                paint.setColor(C_KEY);
            }
            canvas.drawRoundRect(tmpRect, dp(5), dp(5), paint);
            textPaint.setTextSize(dp(15));
            textPaint.setTextAlign(Paint.Align.LEFT);
            textPaint.setColor(i == selected ? selFgColor : C_TEXT);
            canvas.drawText(fitText(candidates.get(i).word,
                            tmpRect.width() - dp(clipMode ? 24 : 8)),
                    tmpRect.left + dp(5), b2Text(tmpRect), textPaint);
            candBoxes.add(new float[]{tmpRect.left, tmpRect.top, tmpRect.width(),
                    tmpRect.height(), i});
            if (clipMode) {
                textPaint.setColor(i == selected ? selFgColor : 0xFFB03A3B);
                textPaint.setTextSize(dp(11));
                textPaint.setTextAlign(Paint.Align.RIGHT);
                canvas.drawText("\u00d7", tmpRect.right - dp(7), tmpRect.top + dp(12), textPaint);
                textPaint.setTextAlign(Paint.Align.LEFT);
                candDelBoxes.add(new float[]{tmpRect.right - dp(18), tmpRect.top,
                        dp(18), dp(20), i});
            }
        }
        if (gridPage > 0 || gridPage + 1 < gridPages()) {
            textPaint.setTextSize(dp(12));
            textPaint.setColor(0x888FA3B3);
            textPaint.setTextAlign(Paint.Align.LEFT);
            if (gridPage > 0) {
                canvas.drawText("\u2039", dp(1), top + gridHeight() / 2, textPaint);
            }
            textPaint.setTextAlign(Paint.Align.RIGHT);
            if (gridPage + 1 < gridPages()) {
                canvas.drawText("\u203a", getWidth() - dp(2),
                        top + gridHeight() / 2, textPaint);
            }
            textPaint.setTextAlign(Paint.Align.LEFT);
        }
    }

    /** 圆角矩形里居中的文字基线。 */
    private float b2Text(RectF r) {
        return r.centerY() - (textPaint.ascent() + textPaint.descent()) / 2;
    }

    private void drawKeyboard(Canvas canvas) {
        keyBoxes.clear();
        if (keyboard == null) return;
        float top = keyboardTop() + pad;
        float left0 = pad;
        float usableW = getWidth() - pad * 2;
        int lastRow = keyboard.rows.size() - 1;
        float rh = rowHeight();
        for (int r = 0; r < keyboard.rows.size(); r++) {
            List<Keyboard.Key> row = keyboard.rows.get(r);
            float sumW = 0;
            for (int i = 0; i < row.size(); i++) {
                sumW += row.get(i).weight;
            }
            float rowW = usableW - gap * (row.size() - 1);
            float x = left0;
            float y = top + r * (rh + gap) + (r == lastRow ? rowGapExtra : 0);
            for (int i = 0; i < row.size(); i++) {
                Keyboard.Key k = row.get(i);
                float w = rowW * (k.weight / sumW);
                if (k.action == Keyboard.A_NONE) {
                    // 数字页左列的占位：这块由下面的运算符条接管绘制与点按
                    x += w + gap;
                    continue;
                }
                tmpRect.set(x, y, x + w, y + rh);
                boolean pressed = pressedKey == k;
                boolean blue = pressed && visualFeedback;
                float bl = tmpRect.left;
                float bt = tmpRect.top;
                float br = tmpRect.right;
                float bb = tmpRect.bottom;
                // 机械回弹：按住下沉 1.8dp，松手用一条衰减曲线弹回（先回冲再停）
                if (keyBounce) {
                    float dy = 0f;
                    float inset = 0f;
                    if (pressed) {
                        dy = dp(1.8f);
                        inset = dp(1f);
                    } else if (bounceKey == k) {
                        float t = (System.currentTimeMillis() - bounceStart) / 190f;
                        if (t >= 1f) {
                            bounceKey = null;
                        } else {
                            dy = dp(1.8f) * (float) (Math.exp(-3.4f * t) * Math.cos(3.4f * t));
                            inset = Math.abs(dy) * 0.55f;
                        }
                    }
                    if (dy != 0f || inset != 0f) {
                        tmpRect.set(bl + inset, bt + dy, br - inset, bb + dy - inset);
                    }
                }

                String label = k.label;
                // 长按已生效：键面直接换成「。」这类长按字符，用户一眼就知道切过去了
                if (pressed && longHeld && k.longInsert != null) {
                    label = k.longInsert;
                }
                int bg;
                int fg;
                boolean strokeKb = "笔画".equals(keyboard.name);
                if (k.disabled) {
                    // 小假名模式下出不了小写假名的键：置灰且不可按
                    bg = 0xFFE7ECF0;
                    fg = 0xFFAFBAC3;
                } else if (k.action == Keyboard.A_SPACE || k.action == Keyboard.A_ENTER) {
                    bg = blue ? C_KEY_PRESSED : C_KEY_SPECIAL;
                    fg = 0xFF2F4F6B;
                } else if (strokeKb && (i == 0 || i == row.size() - 1)) {
                    // 笔画键盘的左右两列（符号栏 / 删除·数字·回车栏）用底色区分出四个区域
                    bg = blue ? C_KEY_PRESSED : C_KEY_SPECIAL;
                    fg = 0xFF2F4F6B;
                } else {
                    bg = blue ? C_KEY_PRESSED : C_KEY;
                    fg = C_TEXT;
                }
                // 注音分区键盘：按声母 / 韵母 / 整体认读 / 声调给底色，一眼分区
                if (!blue && k.zone != 0 && zoneTint) {
                    bg = zoneColor(k.zone);
                }

                // 模式高亮直接落在「拼/注/日」与「中/英」两个键上
                if (k.action == Keyboard.A_SCHEME) {
                    if (englishMode) {
                        label = "拼注日";
                    } else {
                        label = schemeText();
                        bg = schemeIndex == 2 ? C_JP : C_ACCENT;
                        fg = 0xFFFFFFFF;
                    }
                } else if (k.action == Keyboard.A_LANG) {
                    label = englishMode ? "英" : "中";
                    bg = englishMode ? C_ENGLISH : C_ACCENT;
                    fg = 0xFFFFFFFF;
                } else if (k.action == Keyboard.A_JP_MODE) {
                    // 「日語」键（只开日语时占在左下角）：键面显示当前态，点一下 羅 → あ → ア
                    label = jpMode == 0 ? "羅" : (jpMode == 1 ? "あ" : "ア");
                    bg = C_JP;
                    fg = 0xFFFFFFFF;
                } else if (k.action == Keyboard.A_SMALL_KANA) {
                    // 小假名开关：开着蓝底白字，关着灰底
                    bg = smallKana ? C_ACCENT : 0xFFE7ECF0;
                    fg = smallKana ? 0xFFFFFFFF : 0xFF7B8794;
                } else if (k.action == Keyboard.A_SHIFT || k.longAction == Keyboard.A_SHIFT) {
                    // ⇧ 大小写键（以及英文 123 的长按提示）：大写时整键蓝色高亮，白字白箭头
                    bg = englishUpper ? C_ACCENT : (blue ? C_KEY_PRESSED : C_KEY);
                    fg = englishUpper ? 0xFFFFFFFF : C_TEXT;
                } else if (Keyboard.A_CHAR == k.action && "，。".equals(k.label) && englishMode) {
                    label = ".,";
                } else if (Keyboard.A_CHAR == k.action && "，。".equals(k.label) && schemeIndex == 2) {
                    label = "、。";
                } else if (k.action == Keyboard.A_CHAR && schemeIndex == 2 && jpMode == 2) {
                    label = Keyboard.kata(label, true);
                }

                paint.setColor(bg);
                canvas.drawRoundRect(tmpRect, dp(7), dp(7), paint);
                // 分区边框改成「一个区域画一个框」：这里只记下键位，整行画完再按区域合并描边

                textPaint.setColor(fg);
                float textSize = dp(18);
                if (label.length() >= 3) {
                    textSize = dp(12);
                } else if (label.length() == 2) {
                    textSize = dp(15);
                }
                // 挤成 13 键以上的行（旧式注音布局的声调行）字号收一点，不然会顶到边框
                if (row.size() >= 13) {
                    textSize = Math.min(textSize, dp(15));
                }
                // 数字页：数字上方标出长按会出的符号，一眼看得出长按能出什么
                String digitHint = null;
                if (keyboard != null && "数字".equals(keyboard.name)
                        && k.longInsert != null && k.longInsert.length() == 1) {
                    digitHint = k.longInsert;
                }
                // 假名键盘：键上方补一行罗马音，假名字号略小、位置下移给罗马音腾地方
                String romaji = null;
                if (kanaRomaji && k.action == Keyboard.A_CHAR && keyboard != null
                        && "日语".equals(keyboard.name)) {
                    romaji = Keyboard.romajiOf(k.label);
                }
                if (romaji != null) {
                    textSize = Math.min(textSize, dp(16));
                }
                textPaint.setTextSize(textSize);
                textPaint.setTextAlign(Paint.Align.CENTER);
                float baseline = tmpRect.centerY() - (textPaint.ascent() + textPaint.descent()) / 2
                        + (romaji != null || digitHint != null ? dp(5) : 0);
                canvas.drawText(label, tmpRect.centerX(), baseline, textPaint);
                if (digitHint != null) {
                    textPaint.setColor(hintColor);
                    float ds = Math.min(dp(9) * hintScale, textSize * 0.62f);
                    textPaint.setTextSize(ds);
                    textPaint.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText(digitHint, tmpRect.centerX(), tmpRect.top + dp(12), textPaint);
                    textPaint.setColor(fg);
                    textPaint.setTextSize(textSize);
                }
                if (romaji != null) {
                    textPaint.setColor(blue ? 0xFF3B5A75 : romajiColor);
                    float rs = Math.min(dp(9) * hintScale, textSize * 0.62f);
                    if (textPaint.measureText(romaji) > tmpRect.width() - dp(8)) {
                        rs = Math.max(dp(7), rs * 0.85f);
                    }
                    textPaint.setTextSize(rs);
                    textPaint.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText(romaji, tmpRect.centerX(), tmpRect.top + dp(12), textPaint);
                }

                // 右下角小字：注音键对应的拼音字母（颜色可在设置里改）
                if (k.hint != null) {
                    textPaint.setColor(hintColor);
                    // 小字可以放大，但不能超过该键的注音符号 / 假名字号
                    float hs = Math.min(dp(9) * hintScale, textSize * 0.60f);
                    if (textPaint.measureText(k.hint) > tmpRect.width() - dp(8)) {
                        hs = Math.max(dp(7), hs * 0.85f);
                    }
                    textPaint.setTextSize(hs);
                    textPaint.setTextAlign(Paint.Align.RIGHT);
                    canvas.drawText(k.hint, tmpRect.right - dp(4), tmpRect.bottom - dp(3), textPaint);
                    textPaint.setTextAlign(Paint.Align.CENTER);
                }
                // 右上角上箭头：英文 123 键的长按提示，大写时变白
                if (k.longAction == Keyboard.A_SHIFT) {
                    drawShiftArrow(canvas, tmpRect.right - dp(8), tmpRect.top + dp(4),
                            englishUpper ? 0xFFFFFFFF : C_ACCENT);
                }
                // ⇧ 键本身：键面正中画一个上箭头（大写时白箭头配蓝底）
                if (k.action == Keyboard.A_SHIFT) {
                    drawShiftArrow(canvas, tmpRect.centerX(), tmpRect.centerY() - dp(5),
                            englishUpper ? 0xFFFFFFFF : C_TEXT);
                }
                // 右上角小字：123 键标「符」（长按进符号页），拼注日键标罗/あ/ア（长按切日语三态）
                if (k.longAction == Keyboard.A_SYMBOL) {
                    textPaint.setColor(0xFF8C99A6);
                    textPaint.setTextSize(dp(9));
                    textPaint.setTextAlign(Paint.Align.RIGHT);
                    canvas.drawText("符", tmpRect.right - dp(3), tmpRect.top + dp(11), textPaint);
                    textPaint.setTextAlign(Paint.Align.CENTER);
                }
                if (k.action == Keyboard.A_SCHEME && schemeIndex == 2 && !englishMode) {
                    String tag = jpMode == 0 ? "羅" : (jpMode == 1 ? "あ" : "ア");
                    textPaint.setColor(0xFFFFFFFF);
                    textPaint.setTextSize(dp(9));
                    textPaint.setTextAlign(Paint.Align.RIGHT);
                    canvas.drawText(tag, tmpRect.right - dp(3), tmpRect.top + dp(11), textPaint);
                    textPaint.setTextAlign(Paint.Align.CENTER);
                }
                // 右上角小字：长按出标点的键（注音里逗号挂二声、句号挂轻声）标出那个标点
                if (k.longInsert != null && k.longInsert.length() == 1 && schemeIndex == 1
                        && "，。".equals(k.label)) {
                    textPaint.setColor(hintColor);
                    textPaint.setTextSize(dp(9));
                    textPaint.setTextAlign(Paint.Align.RIGHT);
                    canvas.drawText(k.longInsert, tmpRect.right - dp(3), tmpRect.top + dp(11), textPaint);
                    textPaint.setTextAlign(Paint.Align.CENTER);
                }
                if (!k.disabled) {
                    keyBoxes.add(new float[]{bl, bt, br - bl, bb - bt, r, i, k.zone});
                }
                x += w + gap;
            }
        }
        drawZoneRuns(canvas);
        if (keyBounce && bounceKey != null) postInvalidateOnAnimation();
        if (keyboard.opStrip != null) {
            drawOpStrip(canvas);
        }
    }

    /**
     * 分区边框：同区域里相邻的几个键合并成一个圆角框，整块区域只描一圈边，
     * 不声调（声调键只上底色，不画框）。键位来自 keyBoxes 的第 7 项（zone）。
     */
    private void drawZoneRuns(Canvas canvas) {
        if (!zoneBorder || keyBoxes.isEmpty()) return;
        float m = dp(2);
        int curZone = Keyboard.ZONE_NONE;
        int curRow = -1;
        int curCol = -100;
        float l = 0, t = 0, r = 0, b = 0;
        for (int n = 0; n < keyBoxes.size(); n++) {
            float[] box = keyBoxes.get(n);
            int row = (int) box[4];
            int col = (int) box[5];
            int zone = (int) box[6];
            float bl = box[0];
            float bt = box[1];
            float br = bl + box[2];
            float bb = bt + box[3];
            boolean usable = zone != Keyboard.ZONE_NONE && zone != Keyboard.ZONE_TONE;
            if (usable && zone == curZone && row == curRow && col == curCol + 1) {
                l = Math.min(l, bl);
                t = Math.min(t, bt);
                r = Math.max(r, br);
                b = Math.max(b, bb);
                curCol = col;
            } else {
                if (curZone != Keyboard.ZONE_NONE) {
                    drawZoneRun(canvas, curZone, l, t, r, b, m);
                }
                if (usable) {
                    curZone = zone;
                    curRow = row;
                    curCol = col;
                    l = bl;
                    t = bt;
                    r = br;
                    b = bb;
                } else {
                    curZone = Keyboard.ZONE_NONE;
                    curRow = -1;
                    curCol = -100;
                }
            }
        }
        if (curZone != Keyboard.ZONE_NONE) {
            drawZoneRun(canvas, curZone, l, t, r, b, m);
        }
    }

    private void drawZoneRun(Canvas canvas, int zone, float l, float t, float r, float b, float m) {
        paint.setColor(zoneBorderColor(zone));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1.6f));
        canvas.drawRoundRect(l - m, t - m, r + m, b + m, dp(9), dp(9), paint);
        paint.setStyle(Paint.Style.FILL);
    }

    /**
     * 画数字页左列的运算符条（＋ － × ÷ ％ ＝）：只占上面三个数字行的高度，一屏露 4 个，
     * 上下滑动看其余两个；滑动由 onTouchEvent 里的 opScrollPx 驱动。
     */
    private void drawOpStrip(Canvas canvas) {
        List<Keyboard.Key> row0 = keyboard.rows.get(0);
        float sumW = 0;
        for (int i = 0; i < row0.size(); i++) sumW += row0.get(i).weight;
        float usableW = getWidth() - pad * 2;
        float rowW = usableW - gap * (row0.size() - 1);
        float colW = rowW * (row0.get(0).weight / sumW);
        float top = keyboardTop() + pad;
        float rh = rowHeight();
        int lastRow = keyboard.rows.size() - 1;

        opStripL = pad;
        opStripR = pad + colW;
        opStripT = top;
        opStripB = top + lastRow * (rh + gap) - gap;   // 到最底下那行（底栏）的顶边
        float stripH = opStripB - opStripT;
        opCellH = stripH / OP_VISIBLE;
        int cells = keyboard.opStrip.size();
        float maxScroll = Math.max(0, cells * opCellH - stripH);
        if (opScrollPx > maxScroll) opScrollPx = maxScroll;
        if (opScrollPx < 0) opScrollPx = 0;

        canvas.save();
        canvas.clipRect(opStripL, opStripT - dp(3), opStripR, opStripB + dp(3));
        for (int i = 0; i < cells; i++) {
            float y = opStripT + i * opCellH - opScrollPx;
            tmpRect.set(opStripL, y + dp(1), opStripR, y + opCellH - dp(1));
            boolean on = (i == pressedOp);
            paint.setColor(on ? C_ACCENT : C_KEY);
            canvas.drawRoundRect(tmpRect, dp(6), dp(6), paint);
            textPaint.setColor(on ? 0xFFFFFFFF : C_TEXT);
            textPaint.setTextSize(dp(20));
            textPaint.setTextAlign(Paint.Align.CENTER);
            float baseline = tmpRect.centerY() - (textPaint.ascent() + textPaint.descent()) / 2;
            canvas.drawText(keyboard.opStrip.get(i).label, tmpRect.centerX(), baseline, textPaint);
        }
        canvas.restore();
    }

    /** 运算符条的命中检测：落在左列哪一格。 */
    private int opAt(float x, float y) {
        if (keyboard == null || keyboard.opStrip == null || opCellH <= 0) return -1;
        if (x < opStripL || x > opStripR || y < opStripT || y > opStripB) return -1;
        int i = (int) ((y - opStripT + opScrollPx) / opCellH);
        if (i < 0 || i >= keyboard.opStrip.size()) return -1;
        return i;
    }

    /** 按键右上角的小上箭头（⇧），用路径画，不依赖字体是否带这个符号。 */
    private void drawShiftArrow(Canvas canvas, float cx, float top, int color) {
        float w = dp(9);
        float h = dp(6);
        float stem = dp(3);
        float stemH = dp(6);
        Path p = new Path();
        p.moveTo(cx, top);
        p.lineTo(cx - w / 2, top + h);
        p.lineTo(cx - stem / 2, top + h);
        p.lineTo(cx - stem / 2, top + h + stemH);
        p.lineTo(cx + stem / 2, top + h + stemH);
        p.lineTo(cx + stem / 2, top + h);
        p.lineTo(cx + w / 2, top + h);
        p.close();
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(p, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private String schemeText() {
        if (schemeIndex == 2) return "日語";
        if (schemeIndex == 1) return "注音";
        return "拼音";
    }

    private Keyboard.Key keyAt(float x, float y) {
        if (keyboard == null) return null;
        for (int i = 0; i < keyBoxes.size(); i++) {
            float[] b = keyBoxes.get(i);
            if (x >= b[0] && x <= b[0] + b[2] && y >= b[1] && y <= b[1] + b[3]) {
                int row = (int) b[4];
                int col = (int) b[5];
                if (row < keyboard.rows.size() && col < keyboard.rows.get(row).size()) {
                    return keyboard.rows.get(row).get(col);
                }
            }
        }
        return null;
    }

    private int candAt(float x, float y) {
        for (int i = 0; i < candBoxes.size(); i++) {
            float[] b = candBoxes.get(i);
            if (x >= b[0] && x <= b[0] + b[2] && y >= b[1] && y <= b[1] + b[3]) {
                return (int) b[4];
            }
        }
        return -1;
    }

    /** 剪贴板格子右上角 × 的命中检测。 */
    private int delAt(float x, float y) {        for (int i = 0; i < candDelBoxes.size(); i++) {
            float[] b = candDelBoxes.get(i);
            if (x >= b[0] && x <= b[0] + b[2] && y >= b[1] && y <= b[1] + b[3]) {
                return (int) b[4];
            }
        }
        return -1;
    }

    private final Runnable repeatDel = new Runnable() {
        @Override
        public void run() {
            if (pressedKey != null && pressedKey.action == Keyboard.A_BACKSPACE && listener != null) {
                listener.onKey(pressedKey);
                handler.postDelayed(this, 55);
            }
        }
    };

    /** 长按判定：150 毫秒就切到第二档文字（数字键的常用符号也走这一档）。 */
    private static final long LONG_PRESS_MS = 150;

    private final Runnable longPressRun = new Runnable() {
        @Override
        public void run() {
            if (pressedKey != null) {
                longHeld = true;
                invalidate();
            }
        }
    };

    /** 气泡长按：进入放大拖动态，落点光标；之后 MOVE 里跟手移动。 */
    private final Runnable bubbleLongRun = new Runnable() {
        @Override
        public void run() {
            if (bubbleTouch && !bubbleLong) {
                bubbleLong = true;
                if (listener != null) listener.onCaretTap(caretIndexAt(bubbleLastX));
                invalidate();
            }
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        float x = ev.getX();
        float y = ev.getY();
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                longFired = false;
                actPressed = -1;
                candTouchZone = false;
                candDragging = false;
                pressedCand = -1;
                pressedDel = -1;
                if (y < keyboardTop()) {
                    if (y < actTop() + actH) {
                        int id = actAt(x, y);
                        if (id >= 0) {
                            actPressed = id;
                            actDownTime = System.currentTimeMillis();
                            invalidate();
                        }
                        return true;
                    }
                    // 组字气泡：只拦气泡自身的矩形。短按落光标；长按进入放大拖动。
                    if (stripChipOn && x >= stripChipRect[0] && x <= stripChipRect[0] + stripChipRect[2]
                            && y >= stripChipRect[1] && y <= stripChipRect[1] + stripChipRect[3]) {
                        bubbleTouch = true;
                        bubbleLong = false;
                        bubbleLastX = x;
                        handler.removeCallbacks(bubbleLongRun);
                        handler.postDelayed(bubbleLongRun, LONG_PRESS_MS);
                        return true;
                    }
                    if (y < barTop()) {
                        // 展开面板：点候选 / 剪贴板角标；左右划翻页交给 MOVE
                        pressedDel = clipMode ? delAt(x, y) : -1;
                        if (pressedDel < 0) pressedCand = candAt(x, y);
                        candTouchZone = true;
                        candDownX = x;
                        candDownY = y;
                        candDownScroll = 0f;
                        candMoveX = x;
                        candMoveT = System.currentTimeMillis();
                        candFlingV = 0f;
                        invalidate();
                        return true;
                    }
                    // 预选框：横向拖动跟手，纵向一划展开
                    pressedCand = candAt(x, y);
                    candTouchZone = !candidates.isEmpty();
                    candDownX = x;
                    candDownY = y;
                    candDownScroll = candScroll;
                    candMoveX = x;
                    candMoveT = System.currentTimeMillis();
                    candFlingV = 0f;
                    if (candScroller != null) candScroller.forceFinished(true);
                    return true;
                }
                pressedOp = opAt(x, y);
                if (pressedOp >= 0) {
                    opTouching = true;
                    opDragging = false;
                    opNeedSnap = false;
                    opDragY0 = opLastY = y;
                    opLastT = System.currentTimeMillis();
                    opDragScroll0 = opScrollPx;
                    opFlingV = 0f;
                    if (opScroller != null) opScroller.forceFinished(true);
                    invalidate();
                    return true;
                }
                pressedKey = keyAt(x, y);
                pressX = x;
                pressY = y;
                pressDownTime = System.currentTimeMillis();
                slideKey = pressedKey;
                slideStartX = x;
                slideFired = false;
                // 符号页：横向（或竖向）划一下就翻到下一屏 / 上一屏
                symSwipeOn = isSymbolKb();
                symSwiped = false;
                symSwipeX0 = x;
                symSwipeY0 = y;
                if (pressedKey != null) {
                    handler.removeCallbacks(repeatDel);
                    handler.removeCallbacks(longPressRun);
                    longHeld = false;
                    if (pressedKey.action == Keyboard.A_BACKSPACE) {
                        handler.postDelayed(repeatDel, 420);
                    }
                    if (pressedKey.longInsert != null || pressedKey.longAction != 0) {
                        handler.postDelayed(longPressRun, LONG_PRESS_MS);
                    }
                }
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                // 气泡拖动态：手指左右滑动，光标跟手落到最近字母前。
                if (bubbleTouch) {
                    bubbleLastX = x;
                    if (bubbleLong) {
                        if (listener != null) listener.onCaretTap(caretIndexAt(x));
                        invalidate();
                    }
                    return true;
                }
                // 运算符条：上下拖动即滚动（露 4 个，共 6 个运算符），跟手 + 松手惯性
                if (opTouching) {
                    long now = System.currentTimeMillis();
                    long dt = now - opLastT;
                    if (dt >= 1) {
                        float raw = (y - opLastY) / (float) dt;
                        // 平滑一下瞬时速度，免得最后几帧抖动把惯性算飞
                        opFlingV = opFlingV * 0.7f + raw * 0.3f;
                        opLastY = y;
                        opLastT = now;
                    }
                    if (!opDragging && Math.abs(y - opDragY0) > dp(6)) {
                        opDragging = true;
                        pressedOp = -1;
                    }
                    if (opDragging) {
                        float stripH = opStripB - opStripT;
                        float maxScroll = Math.max(0, keyboard.opStrip.size() * opCellH - stripH);
                        opScrollPx = opDragScroll0 - (y - opDragY0);
                        if (opScrollPx < 0) opScrollPx = 0;
                        if (opScrollPx > maxScroll) opScrollPx = maxScroll;
                        invalidate();
                    }
                    return true;
                }
                if (candTouchZone) {
                    float cdx = x - candDownX;
                    float cdy = y - candDownY;
                    boolean horiz = Math.abs(cdx) > dp(20) && Math.abs(cdx) > Math.abs(cdy);
                    boolean vert = Math.abs(cdy) > dp(16) && Math.abs(cdy) > Math.abs(cdx) * 1.4f;
                    if (expanded) {
                        if (horiz) {
                            int pages = gridPages();
                            int np = gridPage + (cdx < 0 ? 1 : -1);
                            if (np < 0) np = 0;
                            if (np > pages - 1) np = pages - 1;
                            if (np != gridPage) {
                                gridPage = np;
                                candTouchZone = false;
                                pressedCand = -1;
                            }
                            invalidate();
                            return true;
                        }
                        if (vert && cdy > 0) {
                            candTouchZone = false;
                            pressedCand = -1;
                            setExpanded(false);
                            return true;
                        }
                        return true;
                    }
                    if (vert) {
                        candTouchZone = false;
                        pressedCand = -1;
                        if (cdy < 0 && !candidates.isEmpty()) setExpanded(true);
                        invalidate();
                        return true;
                    }
                    if (!candDragging && Math.abs(cdx) > dp(7)) {
                        candDragging = true;
                        pressedCand = -1;
                    }
                    if (candDragging) {
                        candScroll = candDownScroll - cdx;
                        if (candScroll < 0f) candScroll = 0f;
                        if (candScroll > candScrollMax) candScroll = candScrollMax;
                        long now = System.currentTimeMillis();
                        long dt = now - candMoveT;
                        if (dt >= 1) {
                            float raw = (x - candMoveX) / (float) dt;
                            candFlingV = candFlingV * 0.7f + raw * 0.3f;
                            candMoveX = x;
                            candMoveT = now;
                        }
                        invalidate();
                        return true;
                    }
                    return true;
                }
                if (symSwipeOn && !symSwiped) {
                    float dx = x - symSwipeX0;
                    float dy = y - symSwipeY0;
                    // 横划超过 40dp 就翻屏；竖向划超过 34dp 也能翻（上划下一屏、下划上一屏）
                    boolean horiz = Math.abs(dx) > dp(40) && Math.abs(dx) > Math.abs(dy);
                    boolean vert = Math.abs(dy) > dp(34) && Math.abs(dy) > Math.abs(dx);
                    if (horiz || vert) {
                        int dir = horiz ? (dx < 0 ? 1 : -1) : (dy < 0 ? 1 : -1);
                        symSwiped = true;
                        pressedKey = null;
                        longHeld = false;
                        handler.removeCallbacks(repeatDel);
                        handler.removeCallbacks(longPressRun);
                        if (listener != null) listener.onSymbolFlip(dir);
                        invalidate();
                        return true;
                    }
                    if (symSwiped) return true;
                }
                // 删除键上横向划动一下就清空整串输入，不用点几十次退格
                if (slideKey != null && !slideFired && slideKey.action == Keyboard.A_BACKSPACE
                        && Math.abs(x - slideStartX) > dp(36)) {
                    slideFired = true;
                    handler.removeCallbacks(repeatDel);
                    pressedKey = null;
                    if (listener != null) {
                        listener.onKey(new Keyboard.Key("清空", null, Keyboard.A_CLEAR_ALL, 1f, null));
                    }
                    invalidate();
                    return true;
                }
                if (pressedKey != null) {
                    Keyboard.Key k = keyAt(x, y);
                    if (k != pressedKey) {
                        pressedKey = null;
                        longHeld = false;
                        handler.removeCallbacks(repeatDel);
                        handler.removeCallbacks(longPressRun);
                        invalidate();
                    }
                }
                if (pressedCand >= 0 && candAt(x, y) != pressedCand) {
                    pressedCand = -1;
                }
                if (pressedDel >= 0 && delAt(x, y) != pressedDel) {
                    pressedDel = -1;
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                handler.removeCallbacks(repeatDel);
                if (bubbleTouch) {
                    handler.removeCallbacks(bubbleLongRun);
                    boolean wasLong = bubbleLong;
                    bubbleTouch = false;
                    bubbleLong = false;
                    // 短按：落点光标；长按拖动：松手保留拖到的光标位置。
                    if (!wasLong && listener != null) {
                        listener.onCaretTap(caretIndexAt(bubbleLastX));
                    }
                    invalidate();
                    return true;
                }
                if (opTouching) {
                    int idx = pressedOp;
                    boolean dragged = opDragging;
                    float v = opFlingV;
                    opTouching = false;
                    opDragging = false;
                    pressedOp = -1;
                    if (dragged) {
                        // 甩得快就顺着惯性再滚一段，甩得慢就直接对齐到整格
                        if (ev.getActionMasked() == MotionEvent.ACTION_UP && Math.abs(v) > 0.55f) {
                            float stripH = opStripB - opStripT;
                            float maxScroll = Math.max(0, keyboard.opStrip.size() * opCellH - stripH);
                            opNeedSnap = true;
                            opScroller.forceFinished(true);
                            opScroller.fling(0, Math.round(opScrollPx), 0, Math.round(-v * 1000f),
                                    0, 0, 0, (int) Math.max(0, maxScroll));
                            postInvalidateOnAnimation();
                        } else {
                            opNeedSnap = true;
                            postInvalidateOnAnimation();
                        }
                    } else if (idx >= 0 && listener != null && idx < keyboard.opStrip.size()) {
                        listener.onKey(keyboard.opStrip.get(idx));
                    }
                    invalidate();
                    return true;
                }
                opDragging = false;
                boolean slideWas = slideFired;
                slideKey = null;
                slideFired = false;
                if (slideWas) {
                    invalidate();
                    return true;
                }
                if (symSwiped) {
                    // 刚刚这一划已经翻过屏了，别再触发按键
                    symSwiped = false;
                    symSwipeOn = false;
                    invalidate();
                    return true;
                }
                symSwipeOn = false;
                if (actPressed >= 0) {
                    int id = actPressed;
                    actPressed = -1;
                    // 全复键长按 = 全删
                    if (id == ACT_SEL_ALL
                            && System.currentTimeMillis() - actDownTime >= ACT_LONG_MS) {
                        id = ACT_DEL_ALL;
                    }
                    onAct(id);
                    invalidate();
                    return true;
                }
                if (candTouchZone) {
                    boolean dragged = candDragging;
                    candTouchZone = false;
                    candDragging = false;
                    if (!expanded && dragged) {
                        if (Math.abs(candFlingV) > 0.5f && candScroller != null) {
                            candScroller.forceFinished(true);
                            candScroller.fling(Math.round(candScroll), 0,
                                    Math.round(-candFlingV * 1100f), 0, 0,
                                    (int) candScrollMax, 0, 0);
                            postInvalidateOnAnimation();
                        } else {
                            candNeedSnap = true;
                            postInvalidateOnAnimation();
                        }
                        return true;
                    }
                }
                if (pressedDel >= 0) {
                    int idx = pressedDel;
                    pressedDel = -1;
                    if (listener != null) listener.onClipDelete(idx);
                    invalidate();
                    return true;
                }
                if (clipMode && pressedCand >= 0) {
                    int idx = pressedCand;
                    pressedCand = -1;
                    long now = System.currentTimeMillis();
                    // 双击同一条才粘贴；单点只浏览条目全文，不写上屏。
                    boolean doubleTap = idx == clipLastTap
                            && now - clipLastTapMs <= CLIP_DOUBLE_MS;
                    if (doubleTap) {
                        clipLastTap = -1;
                        clipPreview = null;
                        if (listener != null) listener.onCandidate(idx);
                    } else {
                        clipLastTap = idx;
                        clipLastTapMs = now;
                        selected = idx;
                        if (idx >= 0 && idx < candidates.size()) {
                            clipPreview = candidates.get(idx).word;
                        }
                    }
                    invalidate();
                    return true;
                }
                if (pressedKey != null) {
                    Keyboard.Key k = pressedKey;
                    boolean held = longHeld || (!longFired
                            && System.currentTimeMillis() - pressDownTime >= LONG_PRESS_MS);
                    pressedKey = null;
                    longHeld = false;
                    handler.removeCallbacks(longPressRun);
                    if (!longFired && held) {
                        longFired = true;
                        if (k.longAction != 0) {
                            // 英文 123 键长按：切大小写，不再输出单字符
                            k = new Keyboard.Key(k.label, null, k.longAction, k.weight, null);
                        } else if (k.longInsert != null) {
                            String ins = k.longInsert;
                            k = new Keyboard.Key(k.label, ins, Keyboard.A_CHAR, 1f, null);
                            // 长按确实生效了：闪一下提示，确认切到了第二档字符
                            flash("长按 · 已输入「" + ins + "」");
                        }
                    }
                    if (keyBounce) {
                        bounceKey = k;
                        bounceStart = System.currentTimeMillis();
                        postInvalidateOnAnimation();
                    }
                    if (listener != null) listener.onKey(k);
                    invalidate();
                    return true;
                }
                if (pressedCand >= 0) {
                    int idx = pressedCand;
                    pressedCand = -1;
                    if (listener != null) listener.onCandidate(idx);
                    invalidate();
                    return true;
                }
                invalidate();
                return true;
            }
            default:
                return super.onTouchEvent(ev);
        }
    }
}

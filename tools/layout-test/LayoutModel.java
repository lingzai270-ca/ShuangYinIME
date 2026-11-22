import java.util.ArrayList;
import java.util.List;

/**
 * ImeView 顶部动作栏 / 候选栏 / 键盘区几何模型（单位 dp，density=1）。
 * 1.0.90 起组字气泡是零占位悬浮层：字条不参与测量、不改按键高度。
 * 与 ImeView.java 的测量与定位方法一一对应；改布局时先改这里和源码，harness 回读源码校验常量。
 */
public class LayoutModel {

    // 对应 ImeView 构造函数里的 dp 常量
    static final float ACT_H = 40;
    static final float BAR_H = 46;
    static final float GRID_ROW_H = 34;
    static final float KEY_H = 52;
    static final float GAP = 5;
    static final float PAD = 4;
    static final float ROW_GAP_EXTRA = 4;
    static final float BOTTOM_PAD = 3;
    static final float BUBBLE_H = 26;
    static final int GRID_ROWS = 3;

    boolean clipMode;
    boolean englishMode;
    boolean expanded;
    int schemeIndex = 0; // 0 拼音 / 1 注音 / 2 日语 / 3 笔画
    String composing = "";
    int rows = 4;

    boolean stripVisible() {
        return !clipMode && !englishMode && !expanded
                && (schemeIndex == 0 || schemeIndex == 1)
                && composing != null && !composing.isEmpty();
    }

    /** 气泡占位恒为 0，只是悬浮绘制。 */
    float stripHeight() {
        return 0f;
    }

    float gridHeight() {
        return expanded ? GRID_ROW_H * GRID_ROWS : 0;
    }

    /** 按键高度恒定，气泡不再从按键高度里匀。 */
    float keyH() {
        return KEY_H;
    }

    float actTop() {
        return 0f;
    }

    float barTop() {
        return ACT_H + gridHeight();
    }

    float barBottom() {
        return barTop() + BAR_H;
    }

    float keyboardTop() {
        return barBottom();
    }

    /** 气泡矩形：贴右端悬浮在候选栏内。 */
    float bubbleTop() {
        return barTop() + (BAR_H - BUBBLE_H) / 2f;
    }

    float bubbleHeight() {
        return BUBBLE_H;
    }

    float totalHeight() {
        float gaps = Math.max(0, rows - 1) * GAP;
        return ACT_H + gridHeight() + BAR_H + PAD * 2
                + ROW_GAP_EXTRA + BOTTOM_PAD + rows * keyH() + gaps;
    }

    /** 快照：总高 / 按键高度 / 候选栏起点 / 键盘顶边 / 气泡高度。 */
    List<Float> snapshot() {
        List<Float> v = new ArrayList<Float>();
        v.add(totalHeight());
        v.add(keyH());
        v.add(barTop());
        v.add(keyboardTop());
        v.add(bubbleHeight());
        return v;
    }
}

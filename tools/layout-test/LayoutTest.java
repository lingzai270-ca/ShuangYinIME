import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 打包前跑的离线布局断言（1.0.90 悬浮气泡版）：
 * 1) 空串 / 打字 / 注音 / 英文 / 剪贴板各态总高、按键高度、候选栏顶边、键盘顶边完全一致；
 * 2) 气泡零占位：不参与测量、不从按键高度里匀、不挤压动作栏；
 * 3) 气泡矩形整体落在候选栏内，且触摸只拦气泡自身矩形；
 * 4) 源码回读：动作栏常显、读音链路在、无 22dp 预留行残留。
 *
 * Usage: java LayoutTest <absolute path to ImeView.java>
 */
public class LayoutTest {

    static int failures = 0;

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            throw new SystemExit("missing ImeView.java path");
        }
        Path src = Path.of(args[0]);
        String code = Files.readString(src, StandardCharsets.UTF_8);

        checkSource(code);

        LayoutModel base = new LayoutModel(); // empty pinyin
        float baseTotal = base.totalHeight();
        float baseKey = base.keyH();
        float baseBarTop = base.barTop();
        float baseKbTop = base.keyboardTop();

        // 打字态：气泡出现，但总高 / 按键高 / 候选栏顶边 / 键盘顶边一个像素都不能变
        LayoutModel typing = new LayoutModel();
        typing.composing = "cankao";
        equal("typing stripVisible", true, typing.stripVisible());
        equal("typing total == empty total", baseTotal, typing.totalHeight());
        equal("typing keyH == empty keyH (no shrink)", baseKey, typing.keyH());
        equal("typing barTop == empty barTop (no push)", baseBarTop, typing.barTop());
        equal("typing keyboardTop == empty keyboardTop", baseKbTop, typing.keyboardTop());
        equal("typing stripHeight == 0 (zero footprint)", 0f, typing.stripHeight());
        assertTrue("typing keyH == 52dp full", typing.keyH() == LayoutModel.KEY_H);

        // 气泡几何：整体落在候选栏里，不越界到动作栏 / 键盘
        assertTrue("bubble fits inside candidate bar",
                typing.bubbleTop() >= typing.barTop() - 0.001f
                        && typing.bubbleTop() + typing.bubbleHeight() <= typing.barBottom() + 0.001f);
        assertTrue("bubble taller than the old 22dp strip", typing.bubbleHeight() >= 26f);

        // 注音打字：同样不变
        LayoutModel zy = new LayoutModel();
        zy.schemeIndex = 1;
        zy.composing = "ㄘㄢ";
        equal("zhuyin total == empty total", baseTotal, zy.totalHeight());
        equal("zhuyin keyH == empty keyH", baseKey, zy.keyH());

        // 英文：气泡不出现
        LayoutModel en = new LayoutModel();
        en.englishMode = true;
        en.composing = "hello";
        equal("english stripVisible", false, en.stripVisible());
        equal("english total == empty total", baseTotal, en.totalHeight());

        // 剪贴板：气泡不出现
        LayoutModel clip = new LayoutModel();
        clip.clipMode = true;
        equal("clip stripVisible", false, clip.stripVisible());
        equal("clip total == empty total", baseTotal, clip.totalHeight());

        // 展开面板：只加面板自身高度，按键高度不变
        LayoutModel exp = new LayoutModel();
        exp.expanded = true;
        float grid = LayoutModel.GRID_ROW_H * LayoutModel.GRID_ROWS;
        equal("expanded panel height", grid, exp.gridHeight());
        equal("expanded total = empty + panel", baseTotal + grid, exp.totalHeight());
        equal("expanded keyH unchanged", baseKey, exp.keyH());

        // 笔画键盘 3 行
        LayoutModel stroke = new LayoutModel();
        stroke.schemeIndex = 3;
        stroke.rows = 3;
        float expectStroke = LayoutModel.ACT_H + LayoutModel.BAR_H + LayoutModel.PAD * 2
                + LayoutModel.ROW_GAP_EXTRA + LayoutModel.BOTTOM_PAD
                + 3 * LayoutModel.KEY_H + 2 * LayoutModel.GAP;
        equal("stroke total formula", expectStroke, stroke.totalHeight());

        if (failures > 0) {
            throw new SystemExit("layout assertions failed: " + failures);
        }
        System.out.println("LAYOUT TEST PASS: bubble is a zero-footprint overlay; total height, key height, "
                + "action row and candidate bar identical across all states.");
    }

    /** 源码回读：常量、测量无字条项、动作栏常显、气泡触摸守卫、读音链路。 */
    static void checkSource(String code) {
        contain(code, "actH = dp(40)");
        contain(code, "barH = dp(46)");
        contain(code, "gridRowH = dp(34)");
        contain(code, "keyH = dp(52)");
        contain(code, "gap = dp(5)");
        contain(code, "pad = dp(4)");
        contain(code, "rowGapExtra = dp(4)");
        contain(code, "bottomPad = dp(3)");
        // 按键高度不再被字条匀掉
        contain(code, "keyH = dp(52) * keyScale;");
        absent(code, "adjKeyH");
        absent(code, "baseKeyH");
        // 测量里不再出现字条项
        absent(code, "float strip = stripHeight();");
        absent(code, "float fixed = strip + actH");
        // 动作栏常显
        contain(code, "drawActionRow(canvas);");
        absent(code, "if (!stripVisible()) drawActionRow");
        // 气泡零占位
        contain(code, "private float stripHeight() {");
        contain(code, "return 0;");
        // 触摸只拦气泡矩形，不吞候选栏
        contain(code, "if (stripChipOn && x >= stripChipRect[0]");
        absent(code, "stripVisible() && y < actTop() + actH");
        // 气泡是圆角胶囊 + 投影
        contain(code, "canvas.drawRoundRect(tmpRect, r, r, paint);");
        // 读音链路
        contain(code, "setReading(Readings r, boolean enabled)");
        contain(code, "private String readingOf(Candidate c)");
    }

    static void contain(String code, String needle) {
        if (!code.contains(needle)) {
            fail("source missing: " + needle);
        }
    }

    static void absent(String code, String needle) {
        if (code.contains(needle)) {
            fail("old logic still present: " + needle);
        }
    }

    static void equal(String name, Object expected, Object actual) {
        if (!expected.equals(actual)) {
            fail(name + " expected=" + expected + " actual=" + actual);
        }
    }

    static void equal(String name, float expected, float actual) {
        if (Math.abs(expected - actual) > 0.001f) {
            fail(name + " expected=" + expected + " actual=" + actual);
        }
    }

    static void assertTrue(String name, boolean ok) {
        if (!ok) fail(name);
    }

    static void fail(String msg) {
        failures++;
        System.out.println("FAIL: " + msg);
    }

    static class SystemExit extends RuntimeException {
        SystemExit(String m) { super(m); }
    }
}

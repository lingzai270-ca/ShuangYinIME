package com.shuangyin.ime;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.shuangyin.ime.core.UserDict;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/** 设置页：方案、模糊音、简拼、候选数量、词库信息。 */
public class SettingsActivity extends Activity {

    private static final int BG = 0xFFF2F4F7;
    private static final int CARD = 0xFFFFFFFF;
    private static final int TEXT = 0xFF22384D;
    private static final int SUB = 0xFF6B7F8F;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Prefs.migrate(this);
        setTitle("霜音输入法 · 设置");

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        addCardTitle(root, "输入方案");
        addCard(root, buildSchemeCard());

        addCardTitle(root, "输入偏好");
        addCard(root, buildPrefCard());

        addCardTitle(root, "用户词库");
        addCard(root, buildUserDictCard());

        addCardTitle(root, "词库");
        addCard(root, buildDictCard());

        addCardTitle(root, "词库分类");
        addCard(root, buildCategoryCard());

        addCardTitle(root, "启用步骤");
        addCard(root, buildHelpCard());

        addCardTitle(root, "重置");
        addCard(root, buildResetCard());

        addCardTitle(root, "关于");
        addCard(root, buildAboutCard());

        setContentView(scroll);
    }

    /** 一键回到出厂默认：清空偏好后按默认值写回。 */
    private View buildResetCard() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        TextView tip = new TextView(this);
        tip.setText("恢复成出厂那一套：拼音 + 笔画开（注音 / 日语 / 假名键盘关）、全拼、模糊音开、简拼开、震动开、"
                + "按下蓝底开、机械回弹关、联想关、注音分区底色与边框关、注音固定大千式、四处候选小字全红（注音右下角 / 假名罗马音 / "
                + "候选拼音高亮 / 笔画候选拼音）、注音候选小字显示拼音、候选 50、四路键盘高度 100、词库分类 5 个频级全开"
                + "（高频 / 常用 / 一般 / 低频 / 长尾）、注音 + 日语词库开。用户词库里的词条不会被删。");
        tip.setTextColor(SUB);
        tip.setTextSize(12);
        tip.setPadding(0, dp(6), 0, dp(8));
        box.addView(tip);
        Button reset = new Button(this);
        reset.setText("恢复默认设置");
        reset.setAllCaps(false);
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("恢复默认设置")
                        .setMessage("所有偏好都会回到出厂默认，确定要恢复吗？")
                        .setPositiveButton("恢复", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                Prefs.resetToDefaults(SettingsActivity.this);
                                Toast.makeText(SettingsActivity.this, "已恢复默认设置", Toast.LENGTH_SHORT).show();
                                recreate();
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        });
        box.addView(reset);
        return box;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    /** 注音候选小字：拼音 / 注音符号 二选一。 */
    private View zyReadRow() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(6), 0, dp(10));

        TextView title = new TextView(this);
        title.setText("注音候选小字");
        title.setTextColor(TEXT);
        title.setTextSize(15);
        box.addView(title);

        TextView desc = new TextView(this);
        desc.setText("注音模式下候选字上方显示什么：拼音（bǎ）或注音符号（ㄅㄚˇ）");
        desc.setTextColor(SUB);
        desc.setTextSize(12);
        desc.setPadding(0, dp(2), 0, dp(6));
        box.addView(desc);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        final Button pin = choiceButton("拼音  bǎ");
        final Button zhy = choiceButton("注音符号  ㄅㄚˇ");
        final int cur = Prefs.zyReadMode(this);
        styleChoice(pin, cur == 0);
        styleChoice(zhy, cur == 1);

        View.OnClickListener l = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int mode = v == pin ? 0 : 1;
                Prefs.get(SettingsActivity.this).edit().putInt(Prefs.KEY_ZY_READ_MODE, mode).apply();
                styleChoice(pin, mode == 0);
                styleChoice(zhy, mode == 1);
            }
        };
        pin.setOnClickListener(l);
        zhy.setOnClickListener(l);

        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp1.rightMargin = dp(8);
        row.addView(pin, lp1);
        row.addView(zhy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        box.addView(row);
        return box;
    }

    private Button choiceButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setPadding(dp(6), dp(10), dp(6), dp(10));
        return b;
    }

    private void styleChoice(Button b, boolean selected) {
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setCornerRadius(dp(8));
        g.setColor(selected ? 0xFF3E7CB1 : 0xFFE6EBF0);
        b.setBackground(g);
        b.setTextColor(selected ? 0xFFFFFFFF : TEXT);
    }

    private void addCardTitle(LinearLayout root, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(SUB);
        tv.setTextSize(13);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(14);
        lp.bottomMargin = dp(6);
        tv.setLayoutParams(lp);
        root.addView(tv);
    }

    private void addCard(LinearLayout root, View content) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(CARD);
        card.setPadding(dp(14), dp(6), dp(14), dp(6));
        card.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(card, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private TextView label(String text, int size, int color) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(size);
        tv.setPadding(0, dp(10), 0, dp(10));
        return tv;
    }

    /** 一个开关写一个布尔偏好，顺手保证「至少留一种输入法」。 */
    private CompoundButton.OnCheckedChangeListener onOff(final String key) {
        return new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean v) {
                Prefs.get(SettingsActivity.this).edit().putBoolean(key, v).apply();
                ensureOneScheme();
            }
        };
    }

    /** 拼音 / 注音 / 日语 全被关掉时把拼音强行打开，免得没有输入法可用。 */
    private void ensureOneScheme() {
        if (!Prefs.pinyinOn(this) && !Prefs.zhuyinOn(this) && !Prefs.japaneseOn(this)) {
            Prefs.get(this).edit().putBoolean(Prefs.KEY_ON_PINYIN, true).apply();
            Toast.makeText(this, "至少保留一种输入法，已重新打开拼音", Toast.LENGTH_LONG).show();
            recreate();
        }
    }

    private View buildSchemeCard() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);

        // 四种输入法的开关：关掉就不在「拼注日」里循环，相关按键（笔画键等）也跟着消失
        addSwitch(box, "拼音输入法", "关掉后「拼注日」不再切到拼音；笔画模式的「拼笔」也回不来", Prefs.pinyinOn(this),
                onOff(Prefs.KEY_ON_PINYIN));
        addSwitch(box, "注音输入法", "关掉后「拼注日」跳到下一种；注音键盘与注音词库都不再参与", Prefs.zhuyinOn(this),
                onOff(Prefs.KEY_ON_ZHUYIN));
        addSwitch(box, "笔画输入法", "关掉后符号页的「笔画」键消失", Prefs.strokeOn(this),
                onOff(Prefs.KEY_ON_STROKE));
        addSwitch(box, "日语输入法", "关掉后「拼注日」不再切到日语，长按也不再切罗马字 / 假名", Prefs.japaneseOn(this),
                onOff(Prefs.KEY_ON_JP));
        addSwitch(box, "日语假名键盘", "开：日语能用五十音键盘（长按「拼注日」循环 罗马字 / 平假名 / 片假名）；"
                        + "关：只用罗马字键盘", Prefs.kanaLayout(this), onOff(Prefs.KEY_KANA_LAYOUT));

        RadioGroup group = new RadioGroup(this);
        RadioButton rbPinyin = new RadioButton(this);
        rbPinyin.setText("全拼（简体，词库：综合词库 + 通用规范汉字表）");
        rbPinyin.setTextColor(TEXT);
        rbPinyin.setId(1001);
        RadioButton rbZhuyin = new RadioButton(this);
        rbZhuyin.setText("注音（繁体，词库：小麥注音）");
        rbZhuyin.setTextColor(TEXT);
        rbZhuyin.setId(1002);
        RadioButton rbJp = new RadioButton(this);
        rbJp.setText("日本語（平假名 / 片假名 / 罗马字，词库：Mozc）");
        rbJp.setTextColor(TEXT);
        rbJp.setId(1003);
        group.addView(rbPinyin);
        group.addView(rbZhuyin);
        group.addView(rbJp);
        int cur = Prefs.scheme(this);
        group.check(cur == 1 ? 1002 : (cur == 2 ? 1003 : 1001));
        group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup g, int id) {
                int s = id == 1002 ? 1 : (id == 1003 ? 2 : 0);
                // 在这里选中某个方案就顺手把它开关打开（默认只开拼音，否则选了也会被弹回去）
                String onKey = s == 1 ? Prefs.KEY_ON_ZHUYIN : (s == 2 ? Prefs.KEY_ON_JP : Prefs.KEY_ON_PINYIN);
                Prefs.get(SettingsActivity.this).edit()
                        .putInt(Prefs.KEY_SCHEME, s)
                        .putBoolean(onKey, true)
                        .apply();
            }
        });
        box.addView(group);
        return box;
    }

    private Switch addSwitch(LinearLayout parent, String title, String desc, boolean value,
                             CompoundButton.OnCheckedChangeListener l) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));
        Switch sw = new Switch(this);
        sw.setText(title);
        sw.setTextColor(TEXT);
        sw.setChecked(value);
        if (l != null) sw.setOnCheckedChangeListener(l);
        row.addView(sw);
        if (desc != null) {
            TextView d = new TextView(this);
            d.setText(desc);
            d.setTextColor(SUB);
            d.setTextSize(12);
            d.setPadding(dp(4), 0, 0, dp(2));
            row.addView(d);
        }
        parent.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return sw;
    }

    private View buildPrefCard() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);

        addSwitch(box, "模糊音", "zh/z、ch/c、sh/s、n/l、f/h、前后鼻音互认", Prefs.fuzzy(this),
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton b, boolean v) {
                        Prefs.get(SettingsActivity.this).edit().putBoolean(Prefs.KEY_FUZZY, v).apply();
                    }
                });

        addSwitch(box, "简拼", "输入声母即可，例如 nh → 你好", Prefs.abbr(this),
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton b, boolean v) {
                        Prefs.get(SettingsActivity.this).edit().putBoolean(Prefs.KEY_ABBR, v).apply();
                    }
                });

        addSwitch(box, "按键震动", null, Prefs.vibrate(this),
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton b, boolean v) {
                        Prefs.get(SettingsActivity.this).edit().putBoolean(Prefs.KEY_VIBRATE, v).apply();
                    }
                });

        addSwitch(box, "按键按下蓝底", "按住任意键时键面变蓝，松手立刻恢复，不再停留 5 秒。",
                Prefs.visual(this),
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton b, boolean v) {
                        Prefs.get(SettingsActivity.this).edit().putBoolean(Prefs.KEY_VISUAL, v).apply();
                    }
                });

        addSwitch(box, "机械键盘回弹", "按住时键面轻微下沉，松手弹回；和上面的「按下蓝底」互不影响，可单独开关。",
                Prefs.keyBounce(this),
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton b, boolean v) {
                        Prefs.get(SettingsActivity.this).edit().putBoolean(Prefs.KEY_KEY_BOUNCE, v).apply();
                    }
                });

        // 注音固定大千式：只说明一句，不再提供布局选择与分区键盘
        TextView zyNote = label("注音布局：固定大千式（ㄅㄉˇˋㄓˊ˙ㄚㄞㄢㄦ 起头，四行各 10~11 键）",
                13, TEXT);
        zyNote.setPadding(0, dp(10), 0, dp(4));
        box.addView(zyNote);

        addSwitch(box, "分区底色", "按「声母 / 韵母 / 声调」给键铺一层底色：声母淡蓝、韵母淡绿、声调淡粉",
                Prefs.zhuyinTint(this),
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton b, boolean v) {
                        Prefs.get(SettingsActivity.this).edit().putBoolean(Prefs.KEY_ZY_TINT, v).apply();
                    }
                });

        addSwitch(box, "分区边框", "同一区域里挨着的几个键合成一整块，整块只描一圈边框，"
                        + "不再一个符号一个框；声调区只上底色，不描边框",
                Prefs.zhuyinBorder(this),
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton b, boolean v) {
                        Prefs.get(SettingsActivity.this).edit().putBoolean(Prefs.KEY_ZY_BORDER, v).apply();
                    }
                });

        box.addView(colorRow("注音右下角小字颜色", Prefs.KEY_HINT_COLOR, Prefs.hintColor(this)));
        box.addView(colorRow("假名上方罗马音颜色", Prefs.KEY_ROMAJI_COLOR, Prefs.romajiColor(this)));
        box.addView(colorRow("候选拼音高亮颜色（已打出的那一截）", Prefs.KEY_HL_COLOR,
                Prefs.highlightColor(this)));
        box.addView(colorRow("笔画候选上方拼音颜色", Prefs.KEY_ST_READ_COLOR,
                Prefs.strokeReadingColor(this)));
        // 选中那条候选的「底色 + 文字色」：默认蓝底白字，色板里深浅都有，可以自己配
        box.addView(zoneColorRow("选中候选的底色", Prefs.KEY_CAND_SEL_BG,
                Prefs.selectedCandidateBg(this)));
        box.addView(zoneColorRow("选中候选的文字颜色", Prefs.KEY_CAND_SEL_FG,
                Prefs.selectedCandidateFg(this)));

        // 注音分区配色：四块区域的底色 + 三块区域的边框，点开可以自己挑色、调透明度
        box.addView(label("注音分区配色", 14, TEXT));
        box.addView(zoneColorRow("声母区底色", Prefs.KEY_ZY_TINT_INITIAL, Prefs.zyTint(this, 0)));
        box.addView(zoneColorRow("韵母区底色", Prefs.KEY_ZY_TINT_FINAL, Prefs.zyTint(this, 1)));
        box.addView(zoneColorRow("整体认读区底色", Prefs.KEY_ZY_TINT_WHOLE, Prefs.zyTint(this, 2)));
        box.addView(zoneColorRow("声调区底色", Prefs.KEY_ZY_TINT_TONE, Prefs.zyTint(this, 3)));
        box.addView(zoneColorRow("声母区边框", Prefs.KEY_ZY_BORDER_INITIAL, Prefs.zyBorder(this, 0)));
        box.addView(zoneColorRow("韵母区边框", Prefs.KEY_ZY_BORDER_FINAL, Prefs.zyBorder(this, 1)));
        box.addView(zoneColorRow("整体认读区边框", Prefs.KEY_ZY_BORDER_WHOLE, Prefs.zyBorder(this, 2)));

        addSwitch(box, "假名上方罗马音", "平假名 / 片假名键盘上，每个假名键顶部显示对应罗马音，"
                        + "例如 あ → a、し → shi、つ → tsu", Prefs.kanaRomaji(this),
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton b, boolean v) {
                        Prefs.get(SettingsActivity.this).edit().putBoolean(Prefs.KEY_KANA_ROMAJI, v).apply();
                    }
                });

        addSwitch(box, "时间 / 日期候选", "打开后简拼直接出结果：rq 日期、sj 时间、xq 星期、"
                        + "tqq 天气晴、tqy 雨天、tqby 暴雨、tqxx 小雪、tqyy 阴云；码要打满才出（打 tqx 不出东西，"
                        + "要 tqxx 才出「天气：小雪」），命中后排最前、空格直接上屏；"
                        + "另外打到「时间」「日期」这两个词时也会跟一条当前的时间 / 日期",
                Prefs.timeCandidate(this),
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton b, boolean v) {
                        Prefs.get(SettingsActivity.this).edit().putBoolean(Prefs.KEY_TIME_CAND, v).apply();
                    }
                });

        addSwitch(box, "联想词", "上屏一个词后，接着给出以它开头的更长的词（中国 → 中国人 / 中国电信，"
                        + "我 → 我们 / 我国），点一下接着上屏", Prefs.assoc(this),
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton b, boolean v) {
                        Prefs.get(SettingsActivity.this).edit().putBoolean(Prefs.KEY_ASSOC, v).apply();
                    }
                });

        addSwitch(box, "候选字注音", "候选字上方显示小字读音：拼音方案显示带调拼音，注音方案按下面选的显示拼音或注音符号，"
                        + "日语显示罗马音", Prefs.reading(this),
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton b, boolean v) {
                        Prefs.get(SettingsActivity.this).edit().putBoolean(Prefs.KEY_READING, v).apply();
                    }
                });

        box.addView(zyReadRow());

        addSwitch(box, "用户词库", "常打的字词自动录入，候选里自动靠前；也可以手动添加词条。"
                        + "关掉后自动录入的词不再生效，但手动添加的词会锁定并继续生效", Prefs.userDict(this),
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton b, boolean v) {
                        Prefs.get(SettingsActivity.this).edit().putBoolean(Prefs.KEY_USERDICT, v).apply();
                        refreshUserDictCard();
                    }
                });

        final int cur = Prefs.count(this);
        TextView title = label("候选词数量：" + cur + "（" + Prefs.COUNT_MIN + "–" + Prefs.COUNT_MAX + "）", 14, TEXT);
        box.addView(title);
        SeekBar bar = new SeekBar(this);
        bar.setMax(Prefs.COUNT_MAX - Prefs.COUNT_MIN);
        bar.setProgress(cur - Prefs.COUNT_MIN);
        final TextView t = title;
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int v = progress + Prefs.COUNT_MIN;
                t.setText("候选词数量：" + v + "（" + Prefs.COUNT_MIN + "–" + Prefs.COUNT_MAX + "）");
                Prefs.get(SettingsActivity.this).edit().putInt(Prefs.KEY_COUNT, v).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        box.addView(bar);

        // 键盘高度：拼音 / 注音 / 日语 / 笔画各存一条，互不影响
        addHeightSlider(box, 0, "拼音键盘高度（含中/英）");
        addHeightSlider(box, 1, "注音键盘高度");
        addHeightSlider(box, 2, "日语键盘高度");
        addHeightSlider(box, 3, "笔画键盘高度");
        addScaleSlider(box, Prefs.KEY_READ_SCALE, Prefs.READ_SCALE_MIN, Prefs.READ_SCALE_MAX,
                Prefs.readScale(this), "候选上方小字大小（拼音 / 注音 / 罗马音）");
        addScaleSlider(box, Prefs.KEY_HINT_SCALE, Prefs.HINT_SCALE_MIN, Prefs.HINT_SCALE_MAX,
                Prefs.hintScale(this), "键面右下角小字大小（注音拼音 / 假名罗马音）");
        return box;
    }

    /** 一条颜色选择：标题 + 十个色块，点哪个就保存哪个色值并给选中的描一圈边框。 */
    private View colorRow(String title, final String prefKey, int current) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(0, dp(8), 0, 0);
        TextView t = label(title, 13, TEXT);
        col.addView(t);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), 0, 0);
        final TextView note = label("当前：" + colorName(current), 12, SUB);
        final int[] selected = {current};
        for (int i = 0; i < Prefs.COLOR_VALUES.length; i++) {
            final int idx = i;
            View sw = new View(this);
            sw.setBackground(swatchBg(Prefs.COLOR_VALUES[i], Prefs.COLOR_VALUES[i] == current));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(24), dp(24));
            lp.rightMargin = dp(6);
            sw.setLayoutParams(lp);
            sw.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int color = Prefs.COLOR_VALUES[idx];
                    Prefs.get(SettingsActivity.this).edit().putInt(prefKey, color).apply();
                    selected[0] = color;
                    note.setText("当前：" + colorName(color));
                    LinearLayout parent = (LinearLayout) v.getParent();
                    for (int k = 0; k < parent.getChildCount(); k++) {
                        int c = Prefs.COLOR_VALUES[k];
                        parent.getChildAt(k).setBackground(swatchBg(c, c == color));
                    }
                }
            });
            row.addView(sw);
        }
        col.addView(row);
        col.addView(note);
        return col;
    }

    /** 色块背景：填充色 + 选中时黑边、未选中时浅灰边。 */
    private GradientDrawable swatchBg(int color, boolean selected) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(4));
        d.setStroke(dp(selected ? 2 : 1), selected ? 0xFF111111 : 0xFFD3DEE7);
        return d;
    }

    /** 色值反查名字，给「当前：」用。 */
    private static String colorName(int color) {
        for (int i = 0; i < Prefs.COLOR_VALUES.length; i++) {
            if (Prefs.COLOR_VALUES[i] == color) return Prefs.COLOR_NAMES[i];
        }
        return "自定义";
    }

    /** 分区配色用的色板（6 列一盘），深浅两档都有，做底色或边框都够挑。 */
    private static final int[] PICK_COLORS = {
            0xFFFFFFFF, 0xFFF6F9FC, 0xFFEAF2FB, 0xFFE6F2F7, 0xFFEBF5EC, 0xFFFCF3E4,
            0xFFFBEDF0, 0xFFF3EDF9, 0xFFD6E6F5, 0xFFCFE3D5, 0xFFF3DFC0, 0xFFF6D2DA,
            0xFFDDD3F0, 0xFFD3EDEC, 0xFFE0E7EE, 0xFF7FA9CE, 0xFF4A80B0, 0xFF2F5F8A,
            0xFF6FAF7C, 0xFF3E8A55, 0xFF2C6B41, 0xFFC79C4E, 0xFFA67A28, 0xFF7C5A18,
            0xFFC98A97, 0xFFB25D6E, 0xFF8E4453, 0xFF9E86C4, 0xFF6A6FA8, 0xFF3B5566,
            0xFF22384D, 0xFF111827, 0xFF000000};

    /** 一条「分区底色 / 分区边框」：左边一块当前色，右边文字说明，点一下打开调色板。 */
    private View zoneColorRow(String title, final String prefKey, int current) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, 0);

        final View sw = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(26), dp(26));
        lp.rightMargin = dp(10);
        sw.setLayoutParams(lp);
        sw.setBackground(swatchBg(current, false));
        row.addView(sw);

        LinearLayout txt = new LinearLayout(this);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(label(title, 13, TEXT));
        final TextView note = label(zoneColorLabel(current), 11, SUB);
        txt.addView(note);
        row.addView(txt);

        final View swatch = sw;
        final TextView noteView = note;
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showZoneColorDialog(title, prefKey,
                        Prefs.get(SettingsActivity.this).getInt(prefKey, readPref(prefKey)),
                        swatch, noteView);
            }
        });
        return row;
    }

    /** 读不到时的兜底（和 Prefs 里的默认值一致）。 */
    private int readPref(String key) {
        if (Prefs.KEY_ZY_TINT_INITIAL.equals(key)) return Prefs.ZY_TINT_DEFAULT[0];
        if (Prefs.KEY_ZY_TINT_FINAL.equals(key)) return Prefs.ZY_TINT_DEFAULT[1];
        if (Prefs.KEY_ZY_TINT_WHOLE.equals(key)) return Prefs.ZY_TINT_DEFAULT[2];
        if (Prefs.KEY_ZY_TINT_TONE.equals(key)) return Prefs.ZY_TINT_DEFAULT[3];
        if (Prefs.KEY_ZY_BORDER_INITIAL.equals(key)) return Prefs.ZY_BORDER_DEFAULT[0];
        if (Prefs.KEY_ZY_BORDER_FINAL.equals(key)) return Prefs.ZY_BORDER_DEFAULT[1];
        if (Prefs.KEY_CAND_SEL_BG.equals(key)) return 0xFF3E7CB1;
        if (Prefs.KEY_CAND_SEL_FG.equals(key)) return 0xFFFFFFFF;
        return Prefs.ZY_BORDER_DEFAULT[2];
    }

    private String zoneColorLabel(int c) {
        return String.format("#%02X%02X%02X", Color.red(c), Color.green(c), Color.blue(c))
                + " · 不透明度 " + Math.round(Color.alpha(c) * 100f / 255f) + "%";
    }

    /**
     * 调色对话框：上面是色板网格（挑色相），下面是透明度滑杆（调深浅），右下确定才存。
     */
    private void showZoneColorDialog(final String title, final String prefKey, int current,
                                     final View swatchView, final TextView noteView) {
        final int[] argb = {current};

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        box.setPadding(pad, dp(6), pad, dp(2));

        final View preview = new View(this);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(26));
        preview.setLayoutParams(plp);
        box.addView(preview);

        final TextView info = label("", 11, SUB);
        info.setPadding(0, dp(6), 0, dp(8));
        box.addView(info);

        final List<View> cells = new java.util.ArrayList<View>();
        final int[] cellRgb = PICK_COLORS.clone();

        // 刷新：选色或拖透明度都走这里（色板按当前透明度重画，预览 / 文字同步）
        final Runnable redo = new Runnable() {
            @Override
            public void run() {
                int a = Color.alpha(argb[0]);
                for (int i = 0; i < cells.size(); i++) {
                    int c = (a << 24) | (cellRgb[i] & 0x00FFFFFF);
                    cells.get(i).setBackground(swatchBg(c, false));
                }
                preview.setBackground(swatchBg(argb[0], false));
                info.setText(zoneColorLabel(argb[0]));
                noteView.setText(zoneColorLabel(argb[0]));
                swatchView.setBackground(swatchBg(argb[0], false));
            }
        };

        final int cols = 6;
        for (int r0 = 0; r0 < PICK_COLORS.length; r0 += cols) {
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            for (int i = r0; i < r0 + cols && i < PICK_COLORS.length; i++) {
                View cell = new View(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(28), 1f);
                lp.setMargins(dp(2), dp(2), dp(2), dp(2));
                cell.setLayoutParams(lp);
                final int rgb = PICK_COLORS[i] & 0x00FFFFFF;
                cell.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int a = Color.alpha(argb[0]);
                        argb[0] = (a << 24) | rgb;
                        redo.run();
                    }
                });
                cells.add(cell);
                line.addView(cell);
            }
            box.addView(line);
        }

        TextView alphaTitle = label("透明度（越小越淡，0% 就是透明）", 12, TEXT);
        alphaTitle.setPadding(0, dp(10), 0, 0);
        box.addView(alphaTitle);

        SeekBar bar = new SeekBar(this);
        bar.setMax(100);
        bar.setProgress(Math.round(Color.alpha(current) * 100f / 255f));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int a = Math.round(progress * 255f / 100f);
                argb[0] = (a << 24) | (argb[0] & 0x00FFFFFF);
                redo.run();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        box.addView(bar);

        redo.run();

        ScrollView sv = new ScrollView(this);
        sv.addView(box);

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(title);
        b.setView(sv);
        b.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Prefs.get(SettingsActivity.this).edit().putInt(prefKey, argb[0]).apply();
                noteView.setText(zoneColorLabel(argb[0]));
                swatchView.setBackground(swatchBg(argb[0], false));
                Toast.makeText(SettingsActivity.this, "已保存，回到键盘即生效",
                        Toast.LENGTH_SHORT).show();
            }
        });
        b.setNegativeButton("取消", null);
        b.show();
    }

    /** 一条高度滑杆：kind 0 拼音 1 注音 2 日语 3 笔画。 */    private void addHeightSlider(LinearLayout box, final int kind, String name) {
        final int curH = Prefs.heightOf(this, kind);
        final TextView t = label(name + "：" + curH + "%（" + Prefs.HEIGHT_MIN + "–"
                + Prefs.HEIGHT_MAX + "%）", 14, TEXT);
        t.setPadding(0, dp(10), 0, 0);
        box.addView(t);
        SeekBar bar = new SeekBar(this);
        bar.setMax(Prefs.HEIGHT_MAX - Prefs.HEIGHT_MIN);
        bar.setProgress(curH - Prefs.HEIGHT_MIN);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int v = progress + Prefs.HEIGHT_MIN;
                t.setText(name + "：" + v + "%（" + Prefs.HEIGHT_MIN + "–" + Prefs.HEIGHT_MAX + "%）");
                Prefs.get(SettingsActivity.this).edit()
                        .putInt(Prefs.heightKey(kind), v).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        box.addView(bar);
    }

    /** 小字百分比滑杆：只能是 100% 起往上放大（100–180 / 100–160）。 */
    private void addScaleSlider(LinearLayout box, final String prefKey, final int min,
                                final int max, int cur, String name) {
        final TextView t = label(name + "：" + cur + "%（" + min + "–" + max + "%）", 14, TEXT);
        t.setPadding(0, dp(10), 0, 0);
        box.addView(t);
        SeekBar bar = new SeekBar(this);
        bar.setMax(max - min);
        bar.setProgress(cur - min);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int v = progress + min;
                t.setText(name + "：" + v + "%（" + min + "–" + max + "%）");
                Prefs.get(SettingsActivity.this).edit().putInt(prefKey, v).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        box.addView(bar);
    }

    // ---------------- 用户词库 ----------------

    private TextView userDictInfo;
    private Button userDictBtn;
    private AlertDialog userDictDialog;

    private UserDict userDict() {
        return UserDict.get(new File(getFilesDir(), "userdict.txt"));
    }

    private View buildUserDictCard() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        userDictInfo = label("", 12, SUB);
        box.addView(userDictInfo);
        userDictBtn = new Button(this);
        userDictBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showUserDictDialog();
            }
        });
        box.addView(userDictBtn);
        LinearLayout btns = new LinearLayout(this);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        Button exp = new Button(this);
        exp.setText("导出到文件");
        exp.setAllCaps(false);
        exp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startExportDict();
            }
        });
        Button imp = new Button(this);
        imp.setText("从文件导入");
        imp.setAllCaps(false);
        imp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startImportDict();
            }
        });
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp1.rightMargin = dp(8);
        btns.addView(exp, lp1);
        btns.addView(imp, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        btns.setPadding(0, dp(6), 0, 0);
        box.addView(btns);
        TextView hint = label("用户词条不设条数上限。导出会存成纯文本（一行一条：编码、词、次数、时间、手动、第二编码）。"
                + "导入支持 txt / csv / tsv / json："
                + "文本用制表符、逗号（含全角）、竖线、空格分隔都行，「编码 + 词」写反了会自动纠正，表头和 # 注释自动跳过；"
                + "JSON 支持 [{\"code\":\"nihao\",\"word\":\"你好\"}]、{\"nihao\":\"你好\"}、[{\"拼音\":\"nihao\",\"词\":\"你好\"}] 这几种写法。"
                + "导入按「编码 + 词」合并，不会清掉原有词条；两个路径都由你在系统文件选择器里挑。", 11, SUB);
        hint.setPadding(0, dp(6), 0, 0);
        box.addView(hint);
        refreshUserDictCard();
        return box;
    }

    // ---------------- 用户词库导入 / 导出 ----------------

    private static final int REQ_EXPORT_DICT = 1001;
    private static final int REQ_IMPORT_DICT = 1002;

    private void startExportDict() {
        int n = userDict().size();
        if (n == 0) {
            toast("词库还是空的，没什么可导出");
            return;
        }
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/plain");
        String day = new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.CHINA)
                .format(new java.util.Date());
        i.putExtra(Intent.EXTRA_TITLE, "霜音用户词库-" + day + ".txt");
        try {
            startActivityForResult(i, REQ_EXPORT_DICT);
        } catch (Exception e) {
            toast("这台设备没有可用的文件选择器");
        }
    }

    private void startImportDict() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/plain", "text/*",
                "text/csv", "text/tab-separated-values", "application/json",
                "application/octet-stream"});
        try {
            startActivityForResult(i, REQ_IMPORT_DICT);
        } catch (Exception e) {
            toast("这台设备没有可用的文件选择器");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        android.net.Uri uri = data.getData();
        if (requestCode == REQ_EXPORT_DICT) {
            java.io.OutputStream os = null;
            try {
                os = getContentResolver().openOutputStream(uri);
                if (os == null) {
                    toast("这个位置写不进去，换个目录试试");
                    return;
                }
                userDict().exportTo(os);
                toast("已导出 " + userDict().size() + " 条到所选文件");
            } catch (Exception e) {
                toast("导出失败：" + e.getMessage());
            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        } else if (requestCode == REQ_IMPORT_DICT) {
            java.io.InputStream is = null;
            try {
                is = getContentResolver().openInputStream(uri);
                if (is == null) {
                    toast("读不到这个文件");
                    return;
                }
                int[] r = userDict().importFrom(is);
                refreshUserDictCard();
                recreate();
                toast("导入完成：新增 " + r[0] + " 条，合并更新 " + r[1] + " 条"
                        + (r[2] > 0 ? "，跳过 " + r[2] + " 行" : ""));
            } catch (Exception e) {
                toast("导入失败：" + e.getMessage());
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        }
    }

    private void refreshUserDictCard() {
        int n = userDict().size();
        int manual = userDict().manualCount();
        int auto = userDict().autoCount();
        boolean on = Prefs.userDict(this);
        if (userDictInfo != null) {
            String head = n == 0
                    ? "还没有记录。打过的字词会自动收进来，也可以手动添加。"
                    : "共 " + n + " 条：手动 " + manual + " 条 · 自动 " + auto + " 条。";
            String tail = on
                    ? "自动录入开启中，越常用越靠前；词库没有的词也会记住。"
                    : "自动录入已关闭：自动录入的词暂不生效，手动添加的 " + manual + " 条已锁定、继续生效。";
            userDictInfo.setText(head + "\n" + tail);
        }
        if (userDictBtn != null) {
            userDictBtn.setText("管理用户词汇（" + n + "）");
        }
    }

    private void showUserDictDialog() {
        final UserDict ud = userDict();
        final List<UserDict.Word> all = ud.all();
        final boolean on = Prefs.userDict(this);
        final int manual = ud.manualCount();
        final int auto = ud.autoCount();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(6), dp(16), dp(4));

        root.addView(label("手动 " + manual + " 条 · 自动 " + auto + " 条", 13, TEXT));
        root.addView(label(on
                ? "自动录入开启中。手动添加的词即使关掉开关也会锁定生效。"
                : "自动录入已关闭：自动词不生效，手动添加的 " + manual + " 条已锁定、仍生效。", 11, SUB));

        Button add = new Button(this);
        add.setText("＋ 添加词汇");
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showUserDictEdit(null);
            }
        });
        root.addView(add);

        // 搜索框：按字、词或编码过滤，边打边筛，方便定位到要改 / 要删的那一条
        final EditText search = new EditText(this);
        search.setHint("搜索词条：字、词、拼音或注音编码都能搜");
        search.setTextSize(14);
        search.setSingleLine(true);
        root.addView(search);

        final TextView hits = label("", 11, SUB);
        root.addView(hits);

        ScrollView sv = new ScrollView(this);
        final LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        sv.addView(list);
        root.addView(sv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(300)));

        fillUserDictRows(list, all, "", on, ud, hits);
        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {
            }

            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
            }

            @Override
            public void afterTextChanged(android.text.Editable e) {
                fillUserDictRows(list, all, e == null ? "" : e.toString(), on, ud, hits);
            }
        });

        if (auto > 0 || manual > 0) {
            LinearLayout ops = new LinearLayout(this);
            ops.setOrientation(LinearLayout.HORIZONTAL);
            if (auto > 0) {
                Button delAuto = new Button(this);
                delAuto.setText("删除自动录入（" + auto + "）");
                delAuto.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ud.removeAuto();
                        refreshUserDictCard();
                        reopenUserDictDialog();
                    }
                });
                ops.addView(delAuto, new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            }
            Button clearAll = new Button(this);
            clearAll.setText("全部清除");
            clearAll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmClearUserDict(ud);
                }
            });
            ops.addView(clearAll, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            root.addView(ops);
        }

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("用户词汇（" + all.size() + "）");
        b.setView(root);
        b.setNegativeButton("关闭", null);
        userDictDialog = b.create();
        userDictDialog.show();
    }

    /** 按搜索词重排词条列表：词、拼音编码、注音编码三者任一命中就算匹配。 */
    private void fillUserDictRows(LinearLayout list, List<UserDict.Word> all, String query,
                                  boolean on, final UserDict ud, TextView hits) {
        list.removeAllViews();
        String q = query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT);
        int shown = 0;
        for (int i = 0; i < all.size(); i++) {
            final UserDict.Word w = all.get(i);
            if (!q.isEmpty() && !userDictHit(w, q)) {
                continue;
            }
            shown++;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(5), 0, dp(5));

            LinearLayout texts = new LinearLayout(this);
            texts.setOrientation(LinearLayout.VERTICAL);
            TextView main = new TextView(this);
            String tag;
            if (w.manual) {
                tag = on ? "手动" : "手动 · 已锁定";
            } else {
                tag = on ? "自动" : "自动 · 已停用";
            }
            main.setText(w.word + "   [" + tag + "]  ×" + w.count);
            main.setTextColor(TEXT);
            main.setTextSize(15);
            texts.addView(main);
            TextView code = new TextView(this);
            String cd = w.code == null ? "" : w.code;
            if (cd.length() > 18) {
                cd = cd.substring(0, 18) + "…";
            }
            String cd2 = w.code2 == null ? "" : w.code2;
            if (cd2.length() > 18) {
                cd2 = cd2.substring(0, 18) + "…";
            }
            code.setText(cd2.isEmpty() ? "编码 " + cd : "拼音 " + cd + " · 注音 " + cd2);
            code.setTextColor(SUB);
            code.setTextSize(11);
            texts.addView(code);
            row.addView(texts, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            Button edit = new Button(this);
            edit.setText("改");
            edit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showUserDictEdit(w);
                }
            });
            row.addView(edit, new LinearLayout.LayoutParams(dp(56),
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            Button del = new Button(this);
            del.setText("删");
            del.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ud.remove(w.code, w.word);
                    refreshUserDictCard();
                    reopenUserDictDialog();
                }
            });
            row.addView(del, new LinearLayout.LayoutParams(dp(56),
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            list.addView(row);
        }
        if (hits != null) {
            if (q.isEmpty()) {
                hits.setText(all.isEmpty() ? "" : "共 " + all.size() + " 条");
            } else {
                hits.setText("匹配 " + shown + " 条 / 共 " + all.size() + " 条");
            }
        }
        if (shown == 0) {
            list.addView(label(q.isEmpty() ? "还没有词条。"
                    : "没有匹配「" + query.trim() + "」的词条。", 13, SUB));
        }
    }

    private static boolean userDictHit(UserDict.Word w, String q) {
        if (w == null) return false;
        if (w.word != null && w.word.toLowerCase(java.util.Locale.ROOT).contains(q)) return true;
        if (w.code != null && w.code.toLowerCase(java.util.Locale.ROOT).contains(q)) return true;
        return w.code2 != null && w.code2.toLowerCase(java.util.Locale.ROOT).contains(q);
    }

    private void reopenUserDictDialog() {
        if (userDictDialog != null) {
            userDictDialog.dismiss();
        }
        showUserDictDialog();
    }

    private void confirmClearUserDict(final UserDict ud) {
        new AlertDialog.Builder(this)
                .setTitle("清空全部用户词汇？")
                .setMessage("手动添加的词也会一并删除，不可恢复。")
                .setPositiveButton("清空", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        ud.clear();
                        refreshUserDictCard();
                        reopenUserDictDialog();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 词的读音转输入编码，返回 {拼音编码, 注音编码}，查不到为 null。 */
    private String[] codesOf(String word) {
        String[] out = new String[]{null, null};
        com.shuangyin.ime.core.Readings r = readings();
        if (r == null || word == null || word.isEmpty()) {
            return out;
        }
        String py = r.word(word, 1);
        String zhy = r.word(word, 2);
        out[0] = py == null ? null : stripToneMarks(py).replace(" ", "");
        out[1] = zhy == null ? null : stripToneMarks(zhy).replace(" ", "");
        return out;
    }

    private com.shuangyin.ime.core.Readings readings;

    private com.shuangyin.ime.core.Readings readings() {
        if (readings == null) {
            try {
                File dir = new File(getFilesDir(), "dicts");
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                copyAssetIfMissing(dir, "reading_pin.txt");
                copyAssetIfMissing(dir, "reading_zhy.txt");
                readings = com.shuangyin.ime.core.Readings.load(dir);
            } catch (Exception e) {
                readings = null;
            }
        }
        return readings;
    }

    /** 设置页可能比输入法先被打开，读音表还没落盘时自己从 assets 拷一份。 */
    private void copyAssetIfMissing(File dir, String name) {
        File f = new File(dir, name);
        if (f.isFile() && f.length() > 0) {
            return;
        }
        InputStream in = null;
        java.io.FileOutputStream out = null;
        try {
            in = getAssets().open(name);
            out = new java.io.FileOutputStream(f);
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            out.flush();
        } catch (Exception e) {
            // 拷失败就让用户手填编码
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    // ignore
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    /** 去掉声调符号：拼音 á→a、ǖ→ü，注音去掉 ˉˊˇˋ˙。 */
    private static String stripToneMarks(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case 'ā': case 'á': case 'ǎ': case 'à': sb.append('a'); break;
                case 'ō': case 'ó': case 'ǒ': case 'ò': sb.append('o'); break;
                case 'ē': case 'é': case 'ě': case 'è': sb.append('e'); break;
                case 'ī': case 'í': case 'ǐ': case 'ì': sb.append('i'); break;
                case 'ū': case 'ú': case 'ǔ': case 'ù': sb.append('u'); break;
                case 'ǖ': case 'ǘ': case 'ǚ': case 'ǜ': sb.append('ü'); break;
                case 'ń': case 'ň': case 'ǹ': sb.append('n'); break;
                case 'ˉ': case 'ˊ': case 'ˇ': case 'ˋ': case '˙': break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    /** 新增或修改一条用户词。target 为 null 表示新增。 */
    private void showUserDictEdit(final UserDict.Word target) {
        final LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(6), dp(16), dp(2));

        box.addView(label("词汇（要打出来的字词）", 12, SUB));
        final android.widget.EditText wordEt = new android.widget.EditText(this);
        wordEt.setHint("例如 霜音");
        wordEt.setSingleLine(true);
        if (target != null) {
            wordEt.setText(target.word);
        }
        box.addView(wordEt);

        box.addView(label("拼音编码（可留空，按读音自动生成）", 12, SUB));
        final android.widget.EditText codeEt = new android.widget.EditText(this);
        codeEt.setHint("例如 shuangyin；留空自动生成");
        codeEt.setSingleLine(true);
        if (target != null && target.code != null) {
            codeEt.setText(target.code);
        }
        box.addView(codeEt);

        box.addView(label("注音编码会按读音自动生成；读音查不到时请手动填写拼音编码。", 11, SUB));

        new AlertDialog.Builder(this)
                .setTitle(target == null ? "添加用户词汇" : "修改用户词汇")
                .setView(box)
                .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        saveUserDict(target, wordEt.getText().toString().trim(),
                                codeEt.getText().toString().trim());
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void saveUserDict(UserDict.Word target, String word, String manualCode) {
        if (word.isEmpty()) {
            toast("请填写词汇");
            return;
        }
        UserDict ud = userDict();
        // 修改时如果编码框里还是旧词自动生成的码，就当作「留空」，按新词的读音重算
        if (target != null && !manualCode.isEmpty()) {
            String[] oldGen = codesOf(target.word);
            if (oldGen[0] != null && manualCode.equals(oldGen[0])) {
                manualCode = "";
            }
        }
        String[] gen = codesOf(word);
        String py = manualCode.isEmpty() ? gen[0] : manualCode;
        String zhy = gen[1];
        if (py == null || py.isEmpty()) {
            toast("「" + word + "」查不到读音，请手动填写拼音编码");
            return;
        }
        zhy = zhy == null ? "" : zhy;
        boolean ok;
        if (target == null) {
            ok = ud.addManual(py, zhy, word);
            if (!ok) {
                toast("这条词已经在词库里了");
                return;
            }
        } else {
            ok = ud.update(target.code, target.word, py, zhy, word);
            if (!ok) {
                toast("保存失败：目标词条已存在");
                return;
            }
        }
        refreshUserDictCard();
        reopenUserDictDialog();
    }

    private void toast(String s) {
        android.widget.Toast.makeText(this, s, android.widget.Toast.LENGTH_SHORT).show();
    }

    private View buildDictCard() {        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        String info = readStats();
        TextView tv = label(info, 13, TEXT);
        tv.setLineSpacing(dp(4), 1f);
        box.addView(tv);
        return box;
    }

    private View buildCategoryCard() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int mask = Prefs.catMask(this);
        TextView hint = label("拼音词库按「用得多不多」分 5 档，整张表按频率从高到低排，每档各占一段名次；"
                + "关掉哪一档，那一档的词就不再出现在候选里（默认 5 档全开，跟以前一样都能打）。", 12, SUB);
        box.addView(hint);
        catSwitch(box, "高频词（前 10%）", "最常用的 1 万条：全部单字和日常高频词，关掉基本就打不了字了",
                Prefs.CAT_HIGH, mask);
        catSwitch(box, "常用词（10%–30%）", "次常用的 2 万条，日常口语与常用词组", Prefs.CAT_COMMON, mask);
        catSwitch(box, "一般词（30%–55%）", "中间那 2.6 万条，专业一点的词、行业小类都在这里",
                Prefs.CAT_MID, mask);
        catSwitch(box, "低频词（55%–80%）", "用得少的 2.6 万条；想候选干净点可以先关这一档", Prefs.CAT_LOW, mask);
        catSwitch(box, "长尾词（80%–100%）", "收尾的 2 万条，四字成语几乎全在这档；关掉候选项最干净",
                Prefs.CAT_TAIL, mask);
        catSwitch(box, "注音词库（小麥注音）", "注音方案专用的繁体词库", Prefs.CAT_ZHUYIN, mask);
        catSwitch(box, "日语词库（Mozc 常用部分）", "Mozc 常用前 30% 的假名与汉字词条", Prefs.CAT_JP, mask);
        return box;
    }

    private void catSwitch(LinearLayout box, String title, String desc, final int bit, int mask) {
        addSwitch(box, title, desc, (mask & bit) != 0,
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton b, boolean v) {
                        int cur = Prefs.catMask(SettingsActivity.this);
                        if (v) {
                            cur |= bit;
                        } else {
                            cur &= ~bit;
                        }
                        Prefs.get(SettingsActivity.this).edit()
                                .putInt(Prefs.KEY_CAT_MASK, cur & Prefs.CAT_ALL).apply();
                    }
                });
    }

    /** 「关于」卡片：一行摘要 + 一个按钮，点开是完整信息（软件 / 开发 / 平台 / 许可 / 数据来源 / 声明）。 */
    private View buildAboutCard() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        TextView sum = label("霜音输入法 " + versionName() + "  ·  " + getPackageName()
                + "\n完全离线、0 权限、无广告 / 无统计 / 无网络库", 13, TEXT);
        sum.setLineSpacing(dp(3), 1f);
        box.addView(sum);
        TextView free = label("免费使用 · 如果你为它花了钱，说明你被骗了", 13, 0xFFC0392B);
        free.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams lpf = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpf.topMargin = dp(4);
        box.addView(free, lpf);
        Button btn = new Button(this);
        btn.setText("关于本软件");
        btn.setAllCaps(false);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAbout();
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(6);
        lp.bottomMargin = dp(8);
        box.addView(btn, lp);

        // 第三方许可全文：放在 assets/licenses 里，分享安装包时对方能在应用内看到
        Button lic = new Button(this);
        lic.setText("第三方许可全文");
        lic.setAllCaps(false);
        lic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLicenses();
            }
        });
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp2.bottomMargin = dp(4);
        box.addView(lic, lp2);
        return box;
    }

    /** 把 assets/licenses 下的许可与来源说明拼成一页给用户看。 */
    private void showLicenses() {
        StringBuilder sb = new StringBuilder();
        sb.append("【本软件】免费使用：不收费、无内购、无广告。任何向你收费的渠道都与作者无关，"
                + "如果你为它花了钱，说明你被骗了。\n\n"
                + "【本软件代码】MIT License，全文见本页 MIT-ShuangYinIME.txt。\n"
                + "【以下许可】是随包词库上游项目的许可与来源说明，只约束对应的词库数据。\n\n");
        try {
            String[] names = getAssets().list("licenses");
            if (names != null) {
                java.util.Arrays.sort(names);
                for (String n : names) {
                    sb.append("—— ").append(n).append(" ——\n");
                    java.io.InputStream in = getAssets().open("licenses/" + n);
                    java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                    byte[] buf = new byte[4096];
                    int len;
                    while ((len = in.read(buf)) > 0) bos.write(buf, 0, len);
                    in.close();
                    sb.append(new String(bos.toByteArray(), "UTF-8")).append("\n\n");
                }
            }
        } catch (Exception e) {
            sb.append("读取许可文件失败：").append(e).append("\n");
        }
        TextView tv = new TextView(this);
        tv.setText(sb.toString());
        tv.setTextSize(11f);
        tv.setTextColor(TEXT);
        tv.setLineSpacing(dp(1), 1f);
        tv.setTextIsSelectable(true);
        int pad = dp(16);
        tv.setPadding(pad, dp(10), pad, dp(10));
        ScrollView sv = new ScrollView(this);
        sv.addView(tv);
        new AlertDialog.Builder(this)
                .setTitle("第三方许可与数据来源")
                .setView(sv)
                .setPositiveButton("关闭", null)
                .show();
    }

    /** 读安装包的 versionName，读不到就退回写死的号。 */
    private String versionName() {
        try {
            String v = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            if (v != null && v.length() > 0) {
                return v;
            }
        } catch (Exception e) {
            // 忽略，用兜底值
        }
        return "1.0.92";
    }

    /** 读安装包的 versionCode，读不到返回 0。 */
    private int versionCode() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (Exception e) {
            return 0;
        }
    }

    private void showAbout() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        box.setPadding(pad, dp(4), pad, dp(4));
        TextView banner = label("免费使用 · 如果你为它花了钱，说明你被骗了", 14, 0xFFC0392B);
        banner.setTypeface(null, android.graphics.Typeface.BOLD);
        banner.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams lpb = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpb.topMargin = dp(6);
        lpb.bottomMargin = dp(2);
        box.addView(banner, lpb);
        aboutSection(box, "软件",
                "名称：霜音输入法（ShuangYinIME）\n"
                        + "包名：com.shuangyin.ime\n"
                        + "版本：" + versionName() + "（versionCode " + versionCode() + "）\n"
                        + "类型：Android 中文输入法（IME），内置全拼 / 笔画 / 注音 / 日本語 四套输入方案\n"
                        + "安装包：约 9.9 MB，词库在包内压缩，首次启动解压到应用私有目录");
        aboutSection(box, "开发信息",
                "个人项目，非商业发行；应用代码以 MIT 开源，源码见下方「开源信息」。\n"
                        + "反馈渠道：lingzai270@gmail.com（问题、建议、兼容性反馈都发这里）。\n构建方式：纯本地命令行构建——JDK 17 + Android SDK 34（aapt2 / javac / d8 / zipalign / apksigner），"
                        + "不使用 Gradle。\n"
                        + "代码组成：只有本工程 Java 源码 + 系统 android.jar；没有第三方 SDK，没有广告 / 统计 / 崩溃上报组件，"
                        + "没有网络库。");
        aboutSection(box, "平台",
                "Android 5.0（API 21）及以上，targetSdk 34 / compileSdk 34。\n"
                        + "适配手机竖屏；四路键盘（拼音 / 注音 / 日本語 / 笔画）高度可各自在 70%–150% 之间调整，默认 100%。");
        aboutSection(box, "状态与许可",
                "免费使用：本软件免费提供，不收费、无内购、无广告，也没有任何激活码、会员或付费解锁。\n"
                        + "如果你为它花了钱（例如在网店买安装包、被人收「安装费」、买所谓激活码），"
                        + "说明你被骗了——请停止付款并向卖家追回。作者不从本软件收取任何费用。\n"
                        + "个人之间免费转发安装包、自用、学习研究都没有限制。\n"
                        + "状态：开发中。未上架任何应用商店，安装包由作者本机编译签名后直接提供。\n"
                        + "代码版权：本应用代码采用 MIT License，源码公开（详见下方「开源信息」）。\n"
                        + "词库许可：随包词库的版权与许可归各自上游项目（MIT / BSD-3-Clause / LGPL-3.0），"
                        + "按各上游许可随包使用，不做二次销售、不单独售卖词库。\n"
                        + "本应用按「现状」提供，不附带任何明示或默示担保。");
        aboutSection(box, "开源信息",
                "许可证：MIT License —— 允许使用、复制、修改、合并、发布、再分发、再授权，也允许商用；"
                        + "唯一要求是在副本或实质性部分里保留版权声明与许可声明。\n"
                        + "版权行：Copyright (c) 2026 kunling。\n反馈与联系：lingzai270@gmail.com（问题、建议、兼容性反馈都发这里）。\n"
                        + "许可全文：随包附在「第三方许可全文」里的 MIT-ShuangYinIME.txt；仓库根目录与工程根目录也有同一份 LICENSE。\n"
                        + "源码：全部 Java 源码在 GitHub 公开 —— " + "https://github.com/lingzai270-ca/ShuangYinIME" + "，本版对应 app\\java\\com\\shuangyin\\ime 下"
                        + "的 FrostIME / ImeView / Keyboard / Prefs / SettingsActivity 与 core 下各引擎。\n"
                        + "范围：MIT 只覆盖本应用代码；随包词库仍是各自上游的许可（MIT / BSD-3-Clause / LGPL-3.0），"
                        + "改词库再分发请对照「第三方许可全文」里的条款。");
        aboutSection(box, "随包数据的来源",
                "全拼：作者整理词表 103,530 条 +《通用规范汉字表》8105 字补充 464 条，入库 103,994 条（权重按频率重排）\n"
                        + "注音：小麥注音 McBopomofo（BPMFBase / BPMFMappings，167,833 条）\n"
                        + "日本語：Mozc（google/mozc），保留代价最低的常用前 30%，324,611 条\n"
                        + "笔画：rime-stroke（源自 CNS11643 全字庫），27,584 字\n"
                        + "数据体积：pinyin.bin 7.1 MB / zhuyin.bin 6.7 MB / japanese.bin 13.2 MB / stroke.bin 0.33 MB");
        aboutSection(box, "声明",
                "完全不联网：安装包不申请任何权限（没有 INTERNET），代码里没有 HTTP / Socket / WebView 调用，"
                        + "清单显式写 usesCleartextTraffic=false；时间 / 日期 / 星期 / 天气都是本机计算或固定文案，不请求网络。\n"
                        + "输入内容只在本机处理：不进日志、不上传、不写外部存储；用户词库与设置存在应用私有目录。\n"
                        + "剪贴板：不会在后台读系统剪贴板——只有你点「📋」打开历史面板、或按「粘贴」时才读一次，"
                        + "并入本地「最近 12 条」历史，只存在内存与本机，点剪贴板页的「清空」可一次删掉，不会上传。\n"
                        + "词库由第三方开源项目整理，可能存在缺词、错词、注音不准；请勿把本软件的输出直接用于法律、"
                        + "医疗等专业场景的正式文本。\n"
                        + "因使用本软件造成的输入错误、数据丢失等后果，由使用者自行承担。");

        ScrollView sv = new ScrollView(this);
        sv.addView(box);
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("关于霜音输入法");
        b.setView(sv);
        b.setPositiveButton("知道了", null);
        b.show();
    }

    private void aboutSection(LinearLayout box, String title, String body) {
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(TEXT);
        t.setTextSize(14);
        t.setPadding(0, dp(12), 0, dp(2));
        box.addView(t);
        TextView b = new TextView(this);
        b.setText(body);
        b.setTextColor(SUB);
        b.setTextSize(12);
        b.setLineSpacing(dp(4), 1f);
        box.addView(b);
    }

    private View buildHelpCard() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        // 只讲「怎么装起来」和「怎么用」，别的一律不写
        box.addView(label("启用（三步）", 14, TEXT));
        TextView tv = label(
                "1. 点下方「打开输入法设置」，进入系统的「语言和输入法」\n"
                        + "2. 在输入法列表里把「霜音输入法」开关打开\n"
                        + "3. 在任意输入框里长按，或点键盘图标，选「霜音输入法」即可开始打字",
                13, TEXT);
        tv.setLineSpacing(dp(4), 1f);
        tv.setPadding(0, dp(4), 0, dp(8));
        box.addView(tv);

        box.addView(label("使用", 14, TEXT));
        TextView use = label(
                "· 底行左边「拼注日」：点一下在 拼音 → 笔画 → 注音 → 日本語 之间循环（开着的才会轮到）；只开了其中一种方案时它自动隐藏\n"
                        + "· 底行右边「中/英」：点一下在中文输入与英文字母之间切换\n"
                        + "· 「123」：短按是数字键盘，长按是符号键盘（键右上角有「符」角标）\n"
                        + "· 候选：点一下上屏，左右滑动看后面几屏，右下「▽」展开全部候选，旁边「全选复制」点一下全选输入框里的文字并复制到剪贴板\n"
                        + "· 「📋」打开剪贴板历史：点开的这一刻才读一次系统剪贴板，格子里是最多 12 条，点右上角红叉删单条；"
                        + "左上角「清空」一键清空——历史和系统剪贴板一起清掉\n"
                        + "· 注音：直接敲注音符号，声调用 ˇ ˋ ˊ ˙，一声用底部宽键（长按出空格）；"
                        + "多模式时长按 ˊ 出「，」、长按 ˙ 出「。」\n"
                        + "· 日本語：开了假名键盘时用「日語」键或长按「拼注日」，在 罗马字 → 平假名 → 片假名 之间切换\n"
                        + "· 笔画：横 一、竖 丨、撇 丿、捺 丶、折 ㄥ、通配，右侧「分词」可断开一个字一个字地选",
                13, TEXT);
        use.setLineSpacing(dp(4), 1f);
        use.setPadding(0, dp(4), 0, dp(6));
        box.addView(use);

        Button btn = new Button(this);
        btn.setText("打开输入法设置");
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
            }
        });
        box.addView(btn);

        Button pick = new Button(this);
        pick.setText("选择输入法");
        pick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.showInputMethodPicker();
            }
        });
        box.addView(pick);
        return box;
    }

    private String readStats() {
        StringBuilder sb = new StringBuilder();
        try {
            InputStream in = getAssets().open("dict_stats.json");
            BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            StringBuilder raw = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) raw.append(line);
            r.close();
            JSONObject o = new JSONObject(raw.toString());
            JSONObject py = o.getJSONObject("pinyin");
            JSONObject zy = o.getJSONObject("zhuyin");
            sb.append("全拼：").append(py.getInt("entries_unique")).append(" 条词，" )
                    .append(py.getInt("keys")).append(" 个编码，简拼 ")
                    .append(py.getInt("abbr_keys")).append(" 组\n");
            sb.append("注音：").append(zy.getInt("entries_unique")).append(" 条词，")
                    .append(zy.getInt("keys")).append(" 个编码\n");
            if (o.has("japanese")) {
                JSONObject jp = o.getJSONObject("japanese");
                sb.append("日本語：").append(jp.getInt("entries_unique")).append(" 条词，")
                        .append(jp.getInt("keys")).append(" 个读音\n");
            }
            sb.append("词库来源：作者自备词表（本地导入 103,530 条）· 通用规范汉字表 8105 字 · 小麥注音 McBopomofo · Mozc · rime-stroke\n");
            sb.append("各词库遵循其上游许可证，详见「第三方许可全文」。");
        } catch (Exception e) {
            sb.append("词库信息读取失败：").append(e.getMessage());
        }
        return sb.toString();
    }
}

package com.shuangyin.ime.core;

public final class Candidate {
    public final String word;
    public final int weight;
    /** 读音（拼音声调串 / 注音 / 日语假名），用于候选栏上方的小字注释。 */
    public String tones;
    public float score;
    /** 词库分类位（1 常用字 2 基础词 4 扩展 8 网络 16 生僻 32 注音 64 日语），-1 表示不限。 */
    public int cat = -1;
    /** 命中的词库键：拼音词库为拼音串，注音词库为注音符号串。 */
    public String key = "";
    /** 拼读补全后的拼音（不带调，ü 写作 v），用于排序与联想。 */
    public String pinyin = "";

    public Candidate(String word, int weight, String tones, float score) {
        this.word = word;
        this.weight = weight;
        this.tones = tones == null ? "" : tones;
        this.score = score;
    }
}

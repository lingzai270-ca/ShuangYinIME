package com.shuangyin.ime.core;

/** 注音规范音节表 + 注音→拼音对照 + 拼读结构分类。
 *  结构：1 声母+韵母 / 2 整体认读 / 3 零声母韵母 / 4 介母开头。
 *  由 tools/gen_zhuyin_table.py 生成，勿手改。 */
public final class ZhuyinSyllables {
    private ZhuyinSyllables() {
    }

    public static final int KIND_INITIAL_FINAL = 1;
    public static final int KIND_INTEGRAL = 2;
    public static final int KIND_ZERO_FINAL = 3;
    public static final int KIND_MEDIAL_FINAL = 4;

    private static final String RAW =
            "ㄇ ㄋ ㄓ ㄔ ㄕ ㄖ ㄗ ㄘ ㄙ ㄚ ㄛ ㄜ ㄞ ㄠ ㄡ ㄢ " +
            "ㄣ ㄤ ㄦ ㄧ ㄨ ㄩ ㄅㄚ ㄅㄛ ㄅㄞ ㄅㄟ ㄅㄠ ㄅㄢ ㄅㄣ ㄅㄤ ㄅㄥ ㄅㄧ " +
            "ㄅㄨ ㄆㄚ ㄆㄛ ㄆㄞ ㄆㄟ ㄆㄠ ㄆㄡ ㄆㄢ ㄆㄣ ㄆㄤ ㄆㄥ ㄆㄧ ㄆㄨ ㄇㄚ ㄇㄛ ㄇㄜ " +
            "ㄇㄞ ㄇㄟ ㄇㄠ ㄇㄡ ㄇㄢ ㄇㄣ ㄇㄤ ㄇㄥ ㄇㄧ ㄇㄨ ㄈㄚ ㄈㄛ ㄈㄟ ㄈㄡ ㄈㄢ ㄈㄣ " +
            "ㄈㄤ ㄈㄥ ㄈㄨ ㄉㄚ ㄉㄜ ㄉㄞ ㄉㄟ ㄉㄠ ㄉㄡ ㄉㄢ ㄉㄣ ㄉㄤ ㄉㄥ ㄉㄧ ㄉㄨ ㄊㄚ " +
            "ㄊㄜ ㄊㄞ ㄊㄠ ㄊㄡ ㄊㄢ ㄊㄤ ㄊㄥ ㄊㄧ ㄊㄨ ㄋㄚ ㄋㄜ ㄋㄞ ㄋㄟ ㄋㄠ ㄋㄡ ㄋㄢ " +
            "ㄋㄣ ㄋㄤ ㄋㄥ ㄋㄧ ㄋㄨ ㄋㄩ ㄌㄚ ㄌㄛ ㄌㄜ ㄌㄞ ㄌㄟ ㄌㄠ ㄌㄡ ㄌㄢ ㄌㄤ ㄌㄥ " +
            "ㄌㄧ ㄌㄨ ㄌㄩ ㄍㄚ ㄍㄜ ㄍㄞ ㄍㄟ ㄍㄠ ㄍㄡ ㄍㄢ ㄍㄣ ㄍㄤ ㄍㄥ ㄍㄨ ㄎㄚ ㄎㄜ " +
            "ㄎㄞ ㄎㄟ ㄎㄠ ㄎㄡ ㄎㄢ ㄎㄣ ㄎㄤ ㄎㄥ ㄎㄨ ㄏㄚ ㄏㄜ ㄏㄞ ㄏㄟ ㄏㄠ ㄏㄡ ㄏㄢ " +
            "ㄏㄣ ㄏㄤ ㄏㄥ ㄏㄨ ㄐㄧ ㄐㄩ ㄑㄧ ㄑㄩ ㄒㄧ ㄒㄩ ㄓㄚ ㄓㄜ ㄓㄞ ㄓㄠ ㄓㄡ ㄓㄢ " +
            "ㄓㄣ ㄓㄤ ㄓㄥ ㄓㄨ ㄔㄚ ㄔㄜ ㄔㄞ ㄔㄠ ㄔㄡ ㄔㄢ ㄔㄣ ㄔㄤ ㄔㄥ ㄔㄨ ㄕㄚ ㄕㄜ " +
            "ㄕㄞ ㄕㄟ ㄕㄠ ㄕㄡ ㄕㄢ ㄕㄣ ㄕㄤ ㄕㄥ ㄕㄨ ㄖㄜ ㄖㄠ ㄖㄡ ㄖㄢ ㄖㄣ ㄖㄤ ㄖㄥ " +
            "ㄖㄨ ㄗㄚ ㄗㄜ ㄗㄞ ㄗㄟ ㄗㄠ ㄗㄡ ㄗㄢ ㄗㄣ ㄗㄤ ㄗㄥ ㄗㄨ ㄘㄚ ㄘㄜ ㄘㄞ ㄘㄠ " +
            "ㄘㄡ ㄘㄢ ㄘㄣ ㄘㄤ ㄘㄥ ㄘㄨ ㄙㄚ ㄙㄜ ㄙㄞ ㄙㄠ ㄙㄡ ㄙㄢ ㄙㄣ ㄙㄤ ㄙㄥ ㄙㄨ " +
            "ㄧㄚ ㄧㄛ ㄧㄝ ㄧㄠ ㄧㄡ ㄧㄢ ㄧㄣ ㄧㄤ ㄧㄥ ㄨㄚ ㄨㄛ ㄨㄞ ㄨㄟ ㄨㄢ ㄨㄣ ㄨㄤ " +
            "ㄨㄥ ㄩㄝ ㄩㄢ ㄩㄣ ㄩㄥ ㄅㄧㄝ ㄅㄧㄠ ㄅㄧㄢ ㄅㄧㄣ ㄅㄧㄥ ㄆㄧㄝ ㄆㄧㄠ ㄆㄧㄢ ㄆㄧㄣ ㄆㄧㄥ ㄇㄧㄝ " +
            "ㄇㄧㄠ ㄇㄧㄡ ㄇㄧㄢ ㄇㄧㄣ ㄇㄧㄥ ㄉㄧㄚ ㄉㄧㄝ ㄉㄧㄠ ㄉㄧㄡ ㄉㄧㄢ ㄉㄧㄥ ㄉㄨㄛ ㄉㄨㄟ ㄉㄨㄢ ㄉㄨㄣ ㄉㄨㄥ " +
            "ㄊㄧㄝ ㄊㄧㄠ ㄊㄧㄢ ㄊㄧㄥ ㄊㄨㄛ ㄊㄨㄟ ㄊㄨㄢ ㄊㄨㄣ ㄊㄨㄥ ㄋㄧㄝ ㄋㄧㄠ ㄋㄧㄡ ㄋㄧㄢ ㄋㄧㄣ ㄋㄧㄤ ㄋㄧㄥ " +
            "ㄋㄨㄛ ㄋㄨㄢ ㄋㄨㄥ ㄋㄩㄝ ㄌㄧㄚ ㄌㄧㄝ ㄌㄧㄠ ㄌㄧㄡ ㄌㄧㄢ ㄌㄧㄣ ㄌㄧㄤ ㄌㄧㄥ ㄌㄨㄛ ㄌㄨㄢ ㄌㄨㄣ ㄌㄨㄥ " +
            "ㄌㄩㄝ ㄍㄨㄚ ㄍㄨㄛ ㄍㄨㄞ ㄍㄨㄟ ㄍㄨㄢ ㄍㄨㄣ ㄍㄨㄤ ㄍㄨㄥ ㄎㄨㄚ ㄎㄨㄛ ㄎㄨㄞ ㄎㄨㄟ ㄎㄨㄢ ㄎㄨㄣ ㄎㄨㄤ " +
            "ㄎㄨㄥ ㄏㄨㄚ ㄏㄨㄛ ㄏㄨㄞ ㄏㄨㄟ ㄏㄨㄢ ㄏㄨㄣ ㄏㄨㄤ ㄏㄨㄥ ㄐㄧㄚ ㄐㄧㄝ ㄐㄧㄠ ㄐㄧㄡ ㄐㄧㄢ ㄐㄧㄣ ㄐㄧㄤ " +
            "ㄐㄧㄥ ㄐㄩㄝ ㄐㄩㄢ ㄐㄩㄣ ㄐㄩㄥ ㄑㄧㄚ ㄑㄧㄝ ㄑㄧㄠ ㄑㄧㄡ ㄑㄧㄢ ㄑㄧㄣ ㄑㄧㄤ ㄑㄧㄥ ㄑㄩㄝ ㄑㄩㄢ ㄑㄩㄣ " +
            "ㄑㄩㄥ ㄒㄧㄚ ㄒㄧㄝ ㄒㄧㄠ ㄒㄧㄡ ㄒㄧㄢ ㄒㄧㄣ ㄒㄧㄤ ㄒㄧㄥ ㄒㄩㄝ ㄒㄩㄢ ㄒㄩㄣ ㄒㄩㄥ ㄓㄨㄚ ㄓㄨㄛ ㄓㄨㄞ " +
            "ㄓㄨㄟ ㄓㄨㄢ ㄓㄨㄣ ㄓㄨㄤ ㄓㄨㄥ ㄔㄨㄚ ㄔㄨㄛ ㄔㄨㄞ ㄔㄨㄟ ㄔㄨㄢ ㄔㄨㄣ ㄔㄨㄤ ㄔㄨㄥ ㄕㄨㄚ ㄕㄨㄛ ㄕㄨㄞ " +
            "ㄕㄨㄟ ㄕㄨㄢ ㄕㄨㄣ ㄕㄨㄤ ㄖㄨㄛ ㄖㄨㄟ ㄖㄨㄢ ㄖㄨㄣ ㄖㄨㄥ ㄗㄨㄛ ㄗㄨㄟ ㄗㄨㄢ ㄗㄨㄣ ㄗㄨㄥ ㄘㄨㄛ ㄘㄨㄟ " +
            "ㄘㄨㄢ ㄘㄨㄣ ㄘㄨㄥ ㄙㄨㄛ ㄙㄨㄟ ㄙㄨㄢ ㄙㄨㄣ ㄙㄨㄥ";

    /** 每个音节的结构类别，字符顺序与 RAW 一致。 */
    private static final String KINDRAW =
            "1122222223333333333222111111111111111111111111111111111111111111" +
            "1111111111111111111111111111111111111111111111111111111111111111" +
            "1111111111111111111111111111111111111111111111111111111111111111" +
            "1111111111111111111111111111111144244424244444444222411111111111" +
            "1111111111111111111111111111111111111111111111111111111111111111" +
            "1111111111111111111111111111111111111111111111111111111111111111" +
            "111111111111111111111111";

    /** 注音→拼音（不带调，ü 写作 v，j/q/x 后写作 u）。 */
    private static final String PINRAW =
            "ㄇ=m ㄋ=n ㄓ=zhi ㄔ=chi ㄕ=shi ㄖ=ri ㄗ=zi ㄘ=ci ㄙ=si ㄚ=a ㄛ=o ㄜ=e ㄞ=ai ㄠ=ao ㄡ=ou ㄢ=an " +
            "ㄣ=en ㄤ=ang ㄦ=er ㄧ=yi ㄨ=wu ㄩ=yu ㄅㄚ=ba ㄅㄛ=bo ㄅㄞ=bai ㄅㄟ=bei ㄅㄠ=bao ㄅㄢ=ban ㄅㄣ=ben " +
            "ㄅㄤ=bang ㄅㄥ=beng ㄅㄧ=bi ㄅㄨ=bu ㄆㄚ=pa ㄆㄛ=po ㄆㄞ=pai ㄆㄟ=pei ㄆㄠ=pao ㄆㄡ=pou ㄆㄢ=pan " +
            "ㄆㄣ=pen ㄆㄤ=pang ㄆㄥ=peng ㄆㄧ=pi ㄆㄨ=pu ㄇㄚ=ma ㄇㄛ=mo ㄇㄜ=me ㄇㄞ=mai ㄇㄟ=mei ㄇㄠ=mao " +
            "ㄇㄡ=mou ㄇㄢ=man ㄇㄣ=men ㄇㄤ=mang ㄇㄥ=meng ㄇㄧ=mi ㄇㄨ=mu ㄈㄚ=fa ㄈㄛ=fo ㄈㄟ=fei ㄈㄡ=fou " +
            "ㄈㄢ=fan ㄈㄣ=fen ㄈㄤ=fang ㄈㄥ=feng ㄈㄨ=fu ㄉㄚ=da ㄉㄜ=de ㄉㄞ=dai ㄉㄟ=dei ㄉㄠ=dao ㄉㄡ=dou " +
            "ㄉㄢ=dan ㄉㄣ=den ㄉㄤ=dang ㄉㄥ=deng ㄉㄧ=di ㄉㄨ=du ㄊㄚ=ta ㄊㄜ=te ㄊㄞ=tai ㄊㄠ=tao ㄊㄡ=tou " +
            "ㄊㄢ=tan ㄊㄤ=tang ㄊㄥ=teng ㄊㄧ=ti ㄊㄨ=tu ㄋㄚ=na ㄋㄜ=ne ㄋㄞ=nai ㄋㄟ=nei ㄋㄠ=nao ㄋㄡ=nou " +
            "ㄋㄢ=nan ㄋㄣ=nen ㄋㄤ=nang ㄋㄥ=neng ㄋㄧ=ni ㄋㄨ=nu ㄋㄩ=nv ㄌㄚ=la ㄌㄛ=lo ㄌㄜ=le ㄌㄞ=lai " +
            "ㄌㄟ=lei ㄌㄠ=lao ㄌㄡ=lou ㄌㄢ=lan ㄌㄤ=lang ㄌㄥ=leng ㄌㄧ=li ㄌㄨ=lu ㄌㄩ=lv ㄍㄚ=ga ㄍㄜ=ge " +
            "ㄍㄞ=gai ㄍㄟ=gei ㄍㄠ=gao ㄍㄡ=gou ㄍㄢ=gan ㄍㄣ=gen ㄍㄤ=gang ㄍㄥ=geng ㄍㄨ=gu ㄎㄚ=ka ㄎㄜ=ke " +
            "ㄎㄞ=kai ㄎㄟ=kei ㄎㄠ=kao ㄎㄡ=kou ㄎㄢ=kan ㄎㄣ=ken ㄎㄤ=kang ㄎㄥ=keng ㄎㄨ=ku ㄏㄚ=ha ㄏㄜ=he " +
            "ㄏㄞ=hai ㄏㄟ=hei ㄏㄠ=hao ㄏㄡ=hou ㄏㄢ=han ㄏㄣ=hen ㄏㄤ=hang ㄏㄥ=heng ㄏㄨ=hu ㄐㄧ=ji ㄐㄩ=ju " +
            "ㄑㄧ=qi ㄑㄩ=qu ㄒㄧ=xi ㄒㄩ=xu ㄓㄚ=zha ㄓㄜ=zhe ㄓㄞ=zhai ㄓㄠ=zhao ㄓㄡ=zhou ㄓㄢ=zhan ㄓㄣ=zhen " +
            "ㄓㄤ=zhang ㄓㄥ=zheng ㄓㄨ=zhu ㄔㄚ=cha ㄔㄜ=che ㄔㄞ=chai ㄔㄠ=chao ㄔㄡ=chou ㄔㄢ=chan " +
            "ㄔㄣ=chen ㄔㄤ=chang ㄔㄥ=cheng ㄔㄨ=chu ㄕㄚ=sha ㄕㄜ=she ㄕㄞ=shai ㄕㄟ=shei ㄕㄠ=shao " +
            "ㄕㄡ=shou ㄕㄢ=shan ㄕㄣ=shen ㄕㄤ=shang ㄕㄥ=sheng ㄕㄨ=shu ㄖㄜ=re ㄖㄠ=rao ㄖㄡ=rou ㄖㄢ=ran " +
            "ㄖㄣ=ren ㄖㄤ=rang ㄖㄥ=reng ㄖㄨ=ru ㄗㄚ=za ㄗㄜ=ze ㄗㄞ=zai ㄗㄟ=zei ㄗㄠ=zao ㄗㄡ=zou ㄗㄢ=zan " +
            "ㄗㄣ=zen ㄗㄤ=zang ㄗㄥ=zeng ㄗㄨ=zu ㄘㄚ=ca ㄘㄜ=ce ㄘㄞ=cai ㄘㄠ=cao ㄘㄡ=cou ㄘㄢ=can ㄘㄣ=cen " +
            "ㄘㄤ=cang ㄘㄥ=ceng ㄘㄨ=cu ㄙㄚ=sa ㄙㄜ=se ㄙㄞ=sai ㄙㄠ=sao ㄙㄡ=sou ㄙㄢ=san ㄙㄣ=sen ㄙㄤ=sang " +
            "ㄙㄥ=seng ㄙㄨ=su ㄧㄚ=ya ㄧㄛ=yo ㄧㄝ=ye ㄧㄠ=yao ㄧㄡ=you ㄧㄢ=yan ㄧㄣ=yin ㄧㄤ=yang ㄧㄥ=ying " +
            "ㄨㄚ=wa ㄨㄛ=wo ㄨㄞ=wai ㄨㄟ=wei ㄨㄢ=wan ㄨㄣ=wen ㄨㄤ=wang ㄨㄥ=weng ㄩㄝ=yue ㄩㄢ=yuan ㄩㄣ=yun " +
            "ㄩㄥ=yong ㄅㄧㄝ=bie ㄅㄧㄠ=biao ㄅㄧㄢ=bian ㄅㄧㄣ=bin ㄅㄧㄥ=bing ㄆㄧㄝ=pie ㄆㄧㄠ=piao ㄆㄧㄢ=pian " +
            "ㄆㄧㄣ=pin ㄆㄧㄥ=ping ㄇㄧㄝ=mie ㄇㄧㄠ=miao ㄇㄧㄡ=miu ㄇㄧㄢ=mian ㄇㄧㄣ=min ㄇㄧㄥ=ming ㄉㄧㄚ=dia " +
            "ㄉㄧㄝ=die ㄉㄧㄠ=diao ㄉㄧㄡ=diu ㄉㄧㄢ=dian ㄉㄧㄥ=ding ㄉㄨㄛ=duo ㄉㄨㄟ=dui ㄉㄨㄢ=duan ㄉㄨㄣ=dun " +
            "ㄉㄨㄥ=dong ㄊㄧㄝ=tie ㄊㄧㄠ=tiao ㄊㄧㄢ=tian ㄊㄧㄥ=ting ㄊㄨㄛ=tuo ㄊㄨㄟ=tui ㄊㄨㄢ=tuan ㄊㄨㄣ=tun " +
            "ㄊㄨㄥ=tong ㄋㄧㄝ=nie ㄋㄧㄠ=niao ㄋㄧㄡ=niu ㄋㄧㄢ=nian ㄋㄧㄣ=nin ㄋㄧㄤ=niang ㄋㄧㄥ=ning ㄋㄨㄛ=nuo " +
            "ㄋㄨㄢ=nuan ㄋㄨㄥ=nong ㄋㄩㄝ=nve ㄌㄧㄚ=lia ㄌㄧㄝ=lie ㄌㄧㄠ=liao ㄌㄧㄡ=liu ㄌㄧㄢ=lian ㄌㄧㄣ=lin " +
            "ㄌㄧㄤ=liang ㄌㄧㄥ=ling ㄌㄨㄛ=luo ㄌㄨㄢ=luan ㄌㄨㄣ=lun ㄌㄨㄥ=long ㄌㄩㄝ=lve ㄍㄨㄚ=gua ㄍㄨㄛ=guo " +
            "ㄍㄨㄞ=guai ㄍㄨㄟ=gui ㄍㄨㄢ=guan ㄍㄨㄣ=gun ㄍㄨㄤ=guang ㄍㄨㄥ=gong ㄎㄨㄚ=kua ㄎㄨㄛ=kuo ㄎㄨㄞ=kuai " +
            "ㄎㄨㄟ=kui ㄎㄨㄢ=kuan ㄎㄨㄣ=kun ㄎㄨㄤ=kuang ㄎㄨㄥ=kong ㄏㄨㄚ=hua ㄏㄨㄛ=huo ㄏㄨㄞ=huai ㄏㄨㄟ=hui " +
            "ㄏㄨㄢ=huan ㄏㄨㄣ=hun ㄏㄨㄤ=huang ㄏㄨㄥ=hong ㄐㄧㄚ=jia ㄐㄧㄝ=jie ㄐㄧㄠ=jiao ㄐㄧㄡ=jiu ㄐㄧㄢ=jian " +
            "ㄐㄧㄣ=jin ㄐㄧㄤ=jiang ㄐㄧㄥ=jing ㄐㄩㄝ=jue ㄐㄩㄢ=juan ㄐㄩㄣ=jun ㄐㄩㄥ=jiong ㄑㄧㄚ=qia ㄑㄧㄝ=qie " +
            "ㄑㄧㄠ=qiao ㄑㄧㄡ=qiu ㄑㄧㄢ=qian ㄑㄧㄣ=qin ㄑㄧㄤ=qiang ㄑㄧㄥ=qing ㄑㄩㄝ=que ㄑㄩㄢ=quan ㄑㄩㄣ=qun " +
            "ㄑㄩㄥ=qiong ㄒㄧㄚ=xia ㄒㄧㄝ=xie ㄒㄧㄠ=xiao ㄒㄧㄡ=xiu ㄒㄧㄢ=xian ㄒㄧㄣ=xin ㄒㄧㄤ=xiang " +
            "ㄒㄧㄥ=xing ㄒㄩㄝ=xue ㄒㄩㄢ=xuan ㄒㄩㄣ=xun ㄒㄩㄥ=xiong ㄓㄨㄚ=zhua ㄓㄨㄛ=zhuo ㄓㄨㄞ=zhuai " +
            "ㄓㄨㄟ=zhui ㄓㄨㄢ=zhuan ㄓㄨㄣ=zhun ㄓㄨㄤ=zhuang ㄓㄨㄥ=zhong ㄔㄨㄚ=chua ㄔㄨㄛ=chuo ㄔㄨㄞ=chuai " +
            "ㄔㄨㄟ=chui ㄔㄨㄢ=chuan ㄔㄨㄣ=chun ㄔㄨㄤ=chuang ㄔㄨㄥ=chong ㄕㄨㄚ=shua ㄕㄨㄛ=shuo ㄕㄨㄞ=shuai " +
            "ㄕㄨㄟ=shui ㄕㄨㄢ=shuan ㄕㄨㄣ=shun ㄕㄨㄤ=shuang ㄖㄨㄛ=ruo ㄖㄨㄟ=rui ㄖㄨㄢ=ruan ㄖㄨㄣ=run " +
            "ㄖㄨㄥ=rong ㄗㄨㄛ=zuo ㄗㄨㄟ=zui ㄗㄨㄢ=zuan ㄗㄨㄣ=zun ㄗㄨㄥ=zong ㄘㄨㄛ=cuo ㄘㄨㄟ=cui ㄘㄨㄢ=cuan " +
            "ㄘㄨㄣ=cun ㄘㄨㄥ=cong ㄙㄨㄛ=suo ㄙㄨㄟ=sui ㄙㄨㄢ=suan ㄙㄨㄣ=sun ㄙㄨㄥ=song";

    /** 合法拼音音节（用于把拼音键切音节）。 */
    private static final String PYRAW =
            "a ai an ang ao ba bai ban bang bao bei ben beng bi bian biao " +
            "bie bin bing bo bu ca cai can cang cao ce cen ceng cha chai chan " +
            "chang chao che chen cheng chi chong chou chu chua chuai chuan chuang chui chun chuo " +
            "ci cong cou cu cuan cui cun cuo da dai dan dang dao de dei den " +
            "deng di dia dian diao die ding diu dong dou du duan dui dun duo e " +
            "en er fa fan fang fei fen feng fo fou fu ga gai gan gang gao " +
            "ge gei gen geng gong gou gu gua guai guan guang gui gun guo ha hai " +
            "han hang hao he hei hen heng hng hong hou hu hua huai huan huang hui " +
            "hun huo ji jia jian jiang jiao jie jin jing jiong jiu ju juan jue jun " +
            "ka kai kan kang kao ke kei ken keng kong kou ku kua kuai kuan kuang " +
            "kui kun kuo la lai lan lang lao le lei leng li lia lian liang liao " +
            "lie lin ling liu lo long lou lu luan lun luo lv lve m ma mai " +
            "man mang mao me mei men meng mi mian miao mie min ming miu mo mou " +
            "mu n na nai nan nang nao ne nei nen neng ng ni nian niang niao " +
            "nie nin ning niu nong nou nu nuan nuo nv nve o ou pa pai pan " +
            "pang pao pei pen peng pi pian piao pie pin ping po pou pu qi qia " +
            "qian qiang qiao qie qin qing qiong qiu qu quan que qun ran rang rao re " +
            "ren reng ri rong rou ru ruan rui run ruo sa sai san sang sao se " +
            "sen seng sha shai shan shang shao she shei shen sheng shi shou shu shua shuai " +
            "shuan shuang shui shun shuo si song sou su suan sui sun suo ta tai tan " +
            "tang tao te teng ti tian tiao tie ting tong tou tu tuan tui tun tuo " +
            "wa wai wan wang wei wen weng wo wu xi xia xian xiang xiao xie xin " +
            "xing xiong xiu xu xuan xue xun ya yan yang yao ye yi yin ying yo " +
            "yong you yu yuan yue yun za zai zan zang zao ze zei zen zeng zha " +
            "zhai zhan zhang zhao zhe zhen zheng zhi zhong zhou zhu zhua zhuai zhuan zhuang zhui " +
            "zhun zhuo zi zong zou zu zuan zui zun zuo";

    private static final java.util.HashMap<String, String> PIN =
            new java.util.HashMap<String, String>(816);
    private static final java.util.HashMap<String, Integer> KIND =
            new java.util.HashMap<String, Integer>(816);
    private static final java.util.HashSet<String> SET =
            new java.util.HashSet<String>(816);
    private static final java.util.HashSet<String> PYSET =
            new java.util.HashSet<String>(820);

    static {
        String[] raw = RAW.split(" ");
        String kinds = KINDRAW.replace(" ", "");
        for (int i = 0; i < raw.length; i++) {
            SET.add(raw[i]);
            KIND.put(raw[i], kinds.charAt(i) - '0');
        }
        for (String p : PINRAW.split(" ")) {
            int eq = p.indexOf('=');
            if (eq > 0) PIN.put(p.substring(0, eq), p.substring(eq + 1));
        }
        for (String p : PYRAW.split(" ")) {
            PYSET.add(p);
        }
    }

    /** 是否为完整的合法注音音节（含整体认读音节）。 */
    public static boolean isSyllable(String s) {
        return s != null && SET.contains(s);
    }

    /** 该注音音节的结构类别；未知返回 0。 */
    public static int kind(String s) {
        Integer k = s == null ? null : KIND.get(s);
        return k == null ? 0 : k.intValue();
    }

    /** 注音音节对应的拼音（不带调）；未知返回 null。 */
    public static String pinyin(String s) {
        return s == null ? null : PIN.get(s);
    }

    /** 是否整体认读音节（ㄓㄔㄕㄖㄗㄘㄙㄧㄨㄩㄧㄝㄩㄝㄩㄢㄧㄣㄩㄣㄧㄥ）。 */
    public static boolean isIntegral(String s) {
        return kind(s) == KIND_INTEGRAL;
    }

    /** 是否为拼音的合法音节（用于切拼音键）。 */
    public static boolean isPinyinSyllable(String s) {
        return s != null && PYSET.contains(s);
    }

    /** 声母符号（21 个）。 */
    public static boolean isInitial(char c) {
        return "ㄅㄆㄇㄈㄉㄊㄋㄌㄍㄎㄏㄐㄑㄒㄓㄔㄕㄖㄗㄘㄙ".indexOf(c) >= 0;
    }

    /** 介母符号（ㄧ ㄨ ㄩ）。 */
    public static boolean isMedial(char c) {
        return c == 'ㄧ' || c == 'ㄨ' || c == 'ㄩ';
    }

    /** 声母符号对应的拼音字母；未知返回空串。 */
    public static String initialLetters(char c) {
        switch (c) {
            case 'ㄅ': return "b";
            case 'ㄆ': return "p";
            case 'ㄇ': return "m";
            case 'ㄈ': return "f";
            case 'ㄉ': return "d";
            case 'ㄊ': return "t";
            case 'ㄋ': return "n";
            case 'ㄌ': return "l";
            case 'ㄍ': return "g";
            case 'ㄎ': return "k";
            case 'ㄏ': return "h";
            case 'ㄐ': return "j";
            case 'ㄑ': return "q";
            case 'ㄒ': return "x";
            case 'ㄓ': return "zh";
            case 'ㄔ': return "ch";
            case 'ㄕ': return "sh";
            case 'ㄖ': return "r";
            case 'ㄗ': return "z";
            case 'ㄘ': return "c";
            case 'ㄙ': return "s";
            default: return "";
        }
    }

    /** 介母单独起头（零声母）时的拼音字母。 */
    public static String medialLetters(char c) {
        if (c == 'ㄧ') return "y";
        if (c == 'ㄨ') return "w";
        if (c == 'ㄩ') return "yu";
        return "";
    }

    public static int size() {
        return SET.size();
    }
}

霜音输入法 · 第三方许可与数据来源

本软件免费使用：不收费、无内购、无广告。任何向你收费的渠道都与作者无关，
如果你为它花了钱，说明你被骗了。

本页说明随包词库的来源。版权归各上游作者，分发本安装包时请同时保留本页内容。
下面列的是随包词库上游的许可，只约束对应的词库数据。

· 本软件代码：MIT License（Copyright (c) 2026 kunling），全文见 MIT-ShuangYinIME.txt；
  源码：https://github.com/lingzai270-ca/ShuangYinIME　反馈邮箱：lingzai270@gmail.com
  允许使用、修改、再分发与商用，保留版权与许可声明即可。

· 全拼词库 pinyin.bin（103,994 条）
  作者自备整理词表（tools/data/词库_输入法.tsv，103,530 行）+《通用规范汉字表》8105 字补充层
  （2013 年国务院公布，464 字缺补）。

· 注音词库 zhuyin.bin（167,833 条）
  小麥注音 McBopomofo（BPMFBase 25,470 / BPMFMappings 146,183）— MIT License。

· 日本語词库 japanese.bin（324,611 条，取 Mozc 全库前 30%）
  Mozc（google/mozc）— BSD 3-Clause License。

· 笔画词库 stroke.bin（27,584 字）
  rime-stroke 五笔画字表（源编码出自 CNS11643 全字庫）— LGPL-3.0；
  其 fork 声明 GPL-3.0-or-later；数据另含 Conway Stroke Data（CC0 / CC-BY-4.0）
  与单手笔顺输入法（GPL-3.0-or-later）。

· 音节表 syllables.txt（410 音节）由作者整理，随上述词库一并使用。

处理方式：词库只做格式转换、权重排序、去重裁剪，不做二次销售、不单独外售词库。
应用 Java 代码版权归作者，未使用上游代码。
GPL / LGPL 两份许可只约束笔画字表的上游数据，不涉及本应用代码与全拼词库。

再分发注意：
1. 随包附上 MIT（本软件与 McBopomofo 各一份）、BSD 3-Clause、LGPL-3.0、GPL-3.0 全文（本应用「第三方许可全文」页可查看）。
2. 不要删除本页与「关于」里的来源说明。
3. 若修改笔画字表再分发，需按 LGPL-3.0 标注修改并提供对应源码。
4. 不要把 .bin 词库单独外传（那属于再分发上游数据，须同样遵守上游许可）。

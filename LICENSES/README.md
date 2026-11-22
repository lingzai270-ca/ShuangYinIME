# 第三方数据与许可

本目录收录霜音输入法（`com.shuangyin.ime`）随包词库所用上游数据的许可证全文与来源说明。
分享、再分发安装包时，请把这几个文件一起给对方（应用内「设置 → 关于 → 第三方许可全文」也能看到同一份内容）。

## 一、随包数据与上游

| 随包文件 | 内容 | 来源 | 许可 |
| --- | --- | --- | --- |
| `pinyin.bin` | 全拼词库 103,994 条（`词库_输入法.tsv` 103,530 行 + 8105 缺字补 464，keys 81,015 / 简拼 35,184 / 联想 4,559） | 作者自备整理词表 `tools/data/词库_输入法.tsv`；补充层为《通用规范汉字表》8105 字（2013 年国务院公布） | 词表由作者自行整理（不使用 rime 系数据）；8105 字表为公开发布的国家标准，非著作权客体 |
| `zhuyin.bin` | 注音词库 167,833 条（BPMFBase 25,470 + BPMFMappings 146,183） | 小麥注音 McBopomofo | MIT（全文：`MIT-McBopomofo.txt`，版权行 `Copyright (c) 2011-2026 Mengjuei Hsieh et al.`） |
| `japanese.bin` | 日语词库 324,611 条（Mozc 全库 1,081,857 条按权重取前 30%） | Mozc（google/mozc） | BSD 3-Clause（全文：`BSD-3-Clause-Mozc.txt`，版权行 `Copyright 2010-2026 Google LLC`） |
| `stroke.bin` | 笔画词库 27,584 字 | rime-stroke 五笔画字表（`stroke.dict.yaml` + `graphics.txt`，编码源自 CNS11643 全字庫） | rime-stroke 主仓为 LGPL-3.0（全文：`LGPL-3.0.txt`）；其 fork 声明 GPL-3.0-or-later（全文：`GPL-3.0.txt`，LGPL 也要求随附 GPL 副本）；数据另含 Conway Stroke Data（CC0 / CC-BY-4.0）、单手笔顺输入法（GPL-3.0-or-later） |
| `syllables.txt` | 410 个拼音音节表 | 作者整理 | 随上述词库一起使用 |

> 关于 GPL / LGPL：这两份许可**只约束 `stroke.bin` 的上游字表**，不涉及本应用 Java 代码，也不涉及全拼词库。

## 二、本应用自身的许可

本应用代码以 **MIT License** 发布，版权行 `Copyright (c) 2026 kunling <lingzai270@gmail.com>`，全文见 `LICENSES\MIT-ShuangYinIME.txt`（工程根目录同名 `LICENSE`）。源码仓库：https://github.com/lingzai270-ca/ShuangYinIME ；问题反馈：lingzai270@gmail.com。
允许使用、修改、合并、发布、再分发、再授权与商业使用，唯一要求是保留版权声明与许可声明。
MIT 只覆盖本应用 Java 代码，不改变下面第三方词库各自的许可。

## 三、本应用的处理方式

- 词库以**二进制派生物**形式随安装包内置（`app/assets/*.bin`），只做格式转换、权重排序、去重裁剪，不做二次销售、不单独售卖词库。
- 词库的版权与许可归各上游作者；应用 Java 代码按 MIT 发布（见下节），未采用上游代码。

## 四、分享 / 再分发时必须做的事

1. **随包附许可**：把本目录的 `MIT-ShuangYinIME.txt`、`MIT-McBopomofo.txt`、`BSD-3-Clause-Mozc.txt`、`LGPL-3.0.txt`、`GPL-3.0.txt` 与安装包放在一起，或让对方在应用内打开「第三方许可全文」。
2. **保留署名**：不要删掉应用内「关于 → 随包数据的来源」与许可页。
3. **LGPL 词库的义务（仅笔画）**：若修改了笔画字表再分发，需按 LGPL-3.0 第 2、4 条标注修改并提供对应源码；只原样转换格式、随应用一起分发，保留许可与署名即可。
4. **不要再单独分发 `.bin`**：词库单独外传属于再分发上游数据，须同样遵守上游许可。
5. **注明改动**：若修改了词库内容，注明修改内容与日期。

> 本目录的许可判断基于各上游仓库公开的 LICENSE（2026-11 核对）。上游若后续改协议，以仓库最新 LICENSE 为准。

## 五、文件清单

- `MIT-ShuangYinIME.txt` — 本应用代码的 MIT 许可全文（1,093 字节）
- `MIT-McBopomofo.txt` — McBopomofo 的 MIT 许可全文（1,083 字节）
- `BSD-3-Clause-Mozc.txt` — Mozc 的 BSD 3-Clause 许可全文（1,483 字节）
- `LGPL-3.0.txt` — GNU LGPL v3 全文（gnu.org 原文）
- `GPL-3.0.txt` — GNU GPL v3 全文（gnu.org 原文，35,149 字节；LGPL-3.0 要求随附）
- `README.md` — 本文件

以上四份许可原文同样打进安装包 `assets/licenses/`，按文件名字典序在应用内展示。

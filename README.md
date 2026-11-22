# 霜音输入法（ShuangYinIME）

一个完全离线的 Android 中文输入法，内置四套输入方案：全拼、注音、日本語、笔画。
不申请任何权限、不联网、无广告、无统计 SDK、无网络库，词库全部随安装包内置。

- 包名：`com.shuangyin.ime`
- 最低系统：Android 5.0（API 21），targetSdk 34
- 安装包体积：约 9.9 MB（词库在包内压缩，首次启动解压到应用私有目录）
- 许可证：MIT（见 [LICENSE](LICENSE)），版权行 `Copyright (c) 2026 kunling <lingzai270@gmail.com>`
- 作者：kunling　·　反馈邮箱：lingzai270@gmail.com

## 特性

- **四套输入方案**：全拼（简拼 / 模糊音）、注音（小麥注音词库，繁体）、日本語（Mozc 词库，罗马字或五十音）、笔画（五笔画，`0` 通配）
- **连打整句**：按「音节个数最少」切分再逐段挑词，`zhonghuarenmingongheguo` → 中华人民共和国
- **单 / 词切换**：候选面板可切到单字模式，逐字上屏、按字频排序
- **符号页三屏**：左右滑动或上下滑动翻屏，不额外占用键位
- **候选 50 条**、四路键盘高度各自可调（70%–150%）
- **组字气泡**：拼音 / 注音串浮在候选栏右端；短按落光标，长按进入放大态并可左右拖动光标
- **剪贴板**：只在点开面板或粘贴时读取，本地保留最近 12 条；单击浏览、双击粘贴，可单条删除或一键清空
- **时间 / 日期 / 星期 / 天气**候选为纯本机计算，无网络请求
- **隐私**：0 权限、`allowBackup=false`，输入内容不上传、不写日志（见 [PRIVACY.md](PRIVACY.md)）

## 安装

1. 从 [Releases](https://github.com/lingzai270-ca/ShuangYinIME/releases) 下载 `ShuangYinIME-<版本>.apk`（唯一的官方发布渠道）
2. 允许「安装未知来源应用」后安装
3. 系统设置 → 语言与输入法 → 虚拟键盘，勾选「霜音输入法」
4. 输入框唤出键盘后，用系统键盘切换键切到霜音

> 1.0.58 起改用自签正式证书。装过 1.0.52–1.0.57（调试证书签名）的用户需要卸载一次再装，之后升级不必反复卸载。

**下载后请校验**（防二次打包 / 篡改）：

- 当前版本 `1.0.92`（10,397,248 字节）的 SHA-256：`E6CEAC0DA1D7616BCC343FF588153ADD6B37B1A66A77C5070F41FC7F209CA89A`
- 同一文件的 MD5 / SHA-1（手机上如果只给出这两种算法，用它们也能核对）：
  - MD5：`B99EEFE3D0A8CFE5524C00EA15725CC4`
  - SHA-1：`EC8FDD1B0C67C6491B7876D23D2D4AA6DE7C9C68`
- 签名证书 SHA-256 指纹：`9D:71:B3:75:55:3C:C4:6C:C1:4D:1A:17:D2:02:69:20:10:CD:42:18:54:CA:38:B9:3D:FE:98:CF:24:AB:A2:3C`
- 指纹不一致 = 不是原版，别装；方法见 [SECURITY.md](SECURITY.md)。

## 构建

不使用 Gradle，纯命令行：`aapt2 → javac → d8 → zipalign → apksigner`。
需要 JDK 17、Android SDK（platform android-34 + build-tools 34.0.0）、Python 3。
详见 [BUILDING.md](BUILDING.md)。

```bash
export SY_BUILDTOOLS=/path/to/buildtools     # 内含 jdk-17.0.2/ 与 android-sdk/
export SY_KS_PASS=your-keystore-password     # 或把口令写进 keystore 同目录的 pass.txt
python tools/build_apk.py
```

## 目录结构

```
app/
  AndroidManifest.xml        清单（0 权限、allowBackup=false）
  java/com/shuangyin/ime/    Java 源码：FrostIME / ImeView / Keyboard / Prefs / SettingsActivity
    core/                    引擎：BinDict / PinyinEngine / ZhuyinEngine / JapaneseEngine / StrokeEngine / UserDict …
  res/                       图标与输入法 method.xml
  assets/                    词库（pinyin.bin / zhuyin.bin / japanese.bin / stroke.bin）与 LICENSES 文本
tools/
  build_apk.py               打包脚本
  build_dict.py              由 TSV / 上游词表生成 pinyin.bin
  build_stroke.py            生成 stroke.bin
  build_readings.py          生成读音表
  fix_word_tones.py          词库条目声调按上下文校正
  layout-test/               离线布局几何自测（打包前把关各态高度一致）
  data/词库_输入法.tsv        全拼词库源表（作者整理）
  data/tghz2013.txt           《通用规范汉字表》8105 字补充层源表
LICENSES/                    第三方许可全文与来源对照
SECURITY.md                  安全问题上报渠道、包校验方法
CODE_OF_CONDUCT.md           社区行为准则
.github/                     CI（语法 + 密钥扫描 + 清单 0 权限检查）、Issue / PR 模板
```

## 许可证与数据来源

- **本应用代码**：MIT License，`Copyright (c) 2026 kunling <lingzai270@gmail.com>`。允许使用、修改、再分发与商业使用，保留版权与许可声明即可。
- **随包词库**（不适用 MIT，按各自上游许可）：

| 文件 | 来源 | 许可 |
| --- | --- | --- |
| `pinyin.bin` | 作者整理词表 +《通用规范汉字表》8105 字补充 | 作者整理 |
| `zhuyin.bin` | McBopomofo 小麥注音 | MIT |
| `japanese.bin` | Mozc（取常用前 30%） | BSD 3-Clause |
| `stroke.bin` | rime-stroke 五笔画字表（源自 CNS11643 全字庫） | LGPL-3.0 / GPL-3.0-or-later |

完整许可文本见 [LICENSES/](LICENSES/) 与 `app/assets/licenses/`，应用内「设置 → 关于 → 第三方许可全文」也能查看。

## 免费使用

本软件不收费、无内购、无激活码。任何向你收费的渠道都与作者无关；如果你为它花了钱，说明你被骗了。

## 免责声明

本软件按「现状」（AS IS）提供，不附带任何明示或默示的担保，包括但不限于适销性、特定用途适用性与不侵权担保。

- 词库由第三方开源项目与作者整理的词表组成，可能存在缺词、错词、注音不准；**不要把本软件的输出直接用于法律、医疗、金融等专业场景的正式文本**。
- 因使用或无法使用本软件造成的输入错误、数据丢失、设备异常、业务损失等后果，由使用者自行承担；作者不承担赔偿责任。
- 本软件不联网、不收集数据（见 [PRIVACY.md](PRIVACY.md)），但**你安装的任何第三方改包**都不在本项目控制范围内——请只从 Releases 下载并按上面的哈希与指纹校验。
- 本免责声明不排除法律强制规定不得排除的责任。

## 反馈与联系

问题、建议、兼容性反馈、词库缺词都可以发到 **lingzai270@gmail.com**，或在本仓库 [Issues](https://github.com/lingzai270-ca/ShuangYinIME/issues) 里提。

- 仓库：https://github.com/lingzai270-ca/ShuangYinIME
- 许可：MIT，`Copyright (c) 2026 kunling <lingzai270@gmail.com>`
- 说明：本输入法不联网、不收集任何数据；反馈写邮件内容即可，不必附日志——它本来也不产生日志。
- **安全问题**不要开公开 Issue，请按 [SECURITY.md](SECURITY.md) 发邮件到 lingzai270@gmail.com（主题带 `[SECURITY]`），3 个工作日内会回复。

## 分发说明

欢迎 fork、改、再分发（MIT 只要求保留版权与许可声明）。

- 自己发布改包时，**换个包名**（`com.shuangyin.ime` 是原版的），别用同一个包名签自己的证书——那会让用户以为能覆盖升级，实际会被系统拦下或顶掉原版数据。
- 不要用「霜音输入法」原名与图标发布修改版，避免用户误认（名称与图标不作为商标授权使用）。

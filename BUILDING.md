# 构建说明（不使用 Gradle）

构建链是纯命令行：`aapt2 compile / link → javac → jar → d8 → 塞进 APK → zipalign → apksigner`。
所有产物落在 `tools/build/`，最终 APK 复制到仓库根目录。

## 环境要求

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 17（`javac` / `jar` / `keytool`） | 编译用 `--release 8`，产物是 Java 8 字节码 |
| Android SDK | platform `android-34`、build-tools `34.0.0` | 需要 `aapt2`、`d8`、`zipalign`、`apksigner` |
| Python | 3.9+ | 只用于跑打包脚本 |

`build_apk.py` 默认按这个布局找工具链：

```
$SY_BUILDTOOLS/
  jdk-17.0.2/bin/{javac,jar,keytool}
  android-sdk/platforms/android-34/android.jar
  android-sdk/build-tools/34.0.0/{aapt2,d8,zipalign,apksigner}
```

脚本按运行平台自动选可执行名：Windows 找 `javac.exe` / `d8.bat` / `apksigner.bat`，
Linux 与 macOS 找同名的无后缀命令（`javac` / `d8` / `apksigner`）。

分别对应 `JDK`、`PLATFORM`、`BUILD_TOOLS` 三个常量，路径布局不同就改这三行。

## 环境变量

| 变量 | 必填 | 作用 |
| --- | --- | --- |
| `SY_BUILDTOOLS` | 是 | 工具链根目录（含 `jdk-17.0.2` 与 `android-sdk`） |
| `SY_KS` | 否 | 签名 keystore 路径；默认 `$SY_BUILDTOOLS/keystore/shuangyin-release.keystore` |
| `SY_KS_PASS` | 是（或 pass.txt） | keystore 口令；没设就读 `$SY_KS` 同目录下 `pass.txt` 首行 |
| `SY_ARCHIVE` | 否 | 打完包额外复制一份到这个目录（作者自用，别人可留空） |

## 打包

```bash
# Linux / macOS
export SY_BUILDTOOLS=/opt/im-buildtools
export SY_KS_PASS='your-password'
python3 tools/build_apk.py

# Windows PowerShell
$env:SY_BUILDTOOLS='D:\im-buildtools'
$env:SY_KS_PASS='your-password'
python tools\build_apk.py
```

脚本会依次输出 `APK: <路径> <体积>` 与 `apksigner` 签名信息。

`SY_KS_PASS` 在脚本 import 时就会读取（没设就找 `pass.txt`），所以**必须先给口令**。
在这个前提下，如果 `SY_KS` 指向的 keystore 不存在，脚本会用 `keytool -genkeypair`
现场生成一个（PKCS12 / RSA 2048 / 10000 天，DN 为 `CN=ShuangYin IME,O=ShuangYin,…`），
方便本地自测；要发版就把它换成自己的密钥并永久备份。

## 版本号

两个地方要一起改，否则覆盖安装会被拦：

1. `tools/build_apk.py` 里的 `VERSION`（生成文件名）
2. `tools/build_apk.py` 传给 aapt2 link 的 `--version-code`（必须大于已安装版本的 versionCode）

`SettingsActivity` 里的版本号是运行时读安装包，不需要手动改（仅在读不到时有兜底字符串）。

## 词库重建（可选）

`app/assets/*.bin` 已随仓库提供，正常构建不需要重建。若要改词库：

```bash
python tools/build_dict.py     # 由 tools/data/词库_输入法.tsv + 上游词表 → pinyin.bin
python tools/build_stroke.py   # 由 rime-stroke 字表 → stroke.bin
python tools/build_readings.py # 生成 reading_pin.txt / reading_zhy.txt
```

其中 `build_dict.py`、`build_stroke.py` 需要上游词表（小麥注音、Mozc、rime-stroke 等），
这些数据不在仓库内。按各上游许可自行获取后，用环境变量告诉脚本：

```bash
# 目录里放 frost/ ice/ mcbopomofo/ mozc/ stroke/ 等子目录，结构见各脚本顶部注释
export SY_DICT_SRC=/path/to/dicts      # Windows: $env:SY_DICT_SRC='D:\dicts'
```

## 自检

`tools/` 下的 `SingleChk.java`、`PinyinChk.java`、`ZhyChk.java` 是纯 JVM 自检（不依赖 Android 运行时）：

```bash
# 先跑一次打包，tools/build/classes 里才有编译好的类
javac -encoding UTF-8 --release 8 -classpath tools/build/classes -d tools/build/tst tools/SingleChk.java
java -Dfile.encoding=UTF-8 -cp "tools/build/classes;tools/build/tst" com.shuangyin.ime.SingleChk   # Windows 分隔符 ;
java -Dfile.encoding=UTF-8 -cp "tools/build/classes:tools/build/tst" com.shuangyin.ime.SingleChk   # Linux/macOS 分隔符 :
```

输出 `ALL PASS` 表示整句、单字模式、候选排序都正常。

## 签名注意事项

- 发布用的 keystore **不要提交到仓库**，`.gitignore` 已排除 `*.keystore`、`*.jks`、`*.p12`、`keystore/` 与 `pass.txt`。
- 换口令不影响证书指纹，已安装用户可以直接覆盖升级；换 keystore 则不行，必须让用户卸载重装。
- keystore 丢失 = 以后无法给已安装用户升级，请离线备份两份。

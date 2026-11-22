# -*- coding: utf-8 -*-
"""不依赖 Gradle 的 APK 构建脚本：aapt2 -> javac -> d8 -> zipalign -> apksigner。"""
import os
import shutil
import subprocess
import sys
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
PROJ = os.path.dirname(HERE)
APP = os.path.join(PROJ, "app")
BUILD = os.path.join(HERE, "build")
# 工具链根目录：可用环境变量 SY_BUILDTOOLS 覆盖（仓库里不写死任何机器路径）
BT = os.environ.get("SY_BUILDTOOLS", "")
if not BT or not os.path.isdir(BT):
    raise SystemExit("请先设置环境变量 SY_BUILDTOOLS，指向含 jdk-17.0.2 与 android-sdk 的目录")
JDK = os.path.join(BT, "jdk-17.0.2")
SDK = os.path.join(BT, "android-sdk")
PLATFORM = os.path.join(SDK, "platforms", "android-34", "android.jar")
BUILD_TOOLS = os.path.join(SDK, "build-tools", "34.0.0")

JAVAC = os.path.join(JDK, "bin", "javac.exe")
KEYTOOL = os.path.join(JDK, "bin", "keytool.exe")
AAPT2 = os.path.join(BUILD_TOOLS, "aapt2.exe")
D8 = os.path.join(BUILD_TOOLS, "d8.bat")
ZIPALIGN = os.path.join(BUILD_TOOLS, "zipalign.exe")
APKSIGNER = os.path.join(BUILD_TOOLS, "apksigner.bat")

VERSION = "1.0.92"

# 正式签名（自签）：密钥放在 buildtools 目录，不随工程分发。
# 口令不写进代码：优先读环境变量 SY_KS_PASS，其次读 keystore 同目录下的 pass.txt。
# 口令与备份注意事项见工具链 keystore 目录下的 README.txt；keystore 丢了就无法给老用户升级。
# 签名密钥：可用环境变量 SY_KS 指定；默认放在工具链目录下的 keystore\
KEYSTORE = os.environ.get("SY_KS") or os.path.join(BT, "keystore", "shuangyin-release.keystore")
KS_ALIAS = "shuangyin"

# 每打完一个包，额外存档一份到这个目录（E 盘共享目录，供手机/他人取用）。
# 可用环境变量 SY_ARCHIVE 覆盖；目录不存在时只跳过并提示，不影响打包。
ARCHIVE_DIR = os.environ.get("SY_ARCHIVE") or r"E:\文件共享\新建文件夹"


def ks_pass():
    p = os.environ.get("SY_KS_PASS")
    if p:
        return p
    f = os.path.join(os.path.dirname(KEYSTORE), "pass.txt")
    if os.path.exists(f):
        with open(f, "r", encoding="utf-8") as fp:
            p = fp.read().strip()
        if p:
            return p
    raise SystemExit("签名口令缺失：请设置环境变量 SY_KS_PASS，或写入 "
                     + f + "（首行为口令）")


KS_PASS = ks_pass()


def run(cmd, cwd=None):
    print(">>", " ".join(cmd))
    env = dict(os.environ)
    env["PATH"] = os.path.join(JDK, "bin") + os.pathsep + env.get("PATH", "")
    env["JAVA_HOME"] = JDK
    p = subprocess.run(cmd, cwd=cwd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                       universal_newlines=True, encoding="utf-8", errors="replace",
                       env=env)
    if p.stdout:
        print(p.stdout.rstrip())
    if p.returncode != 0:
        raise SystemExit(f"命令失败({p.returncode}): {' '.join(cmd)}")
    return p.stdout


def java_sources():
    out = []
    for root, dirs, files in os.walk(os.path.join(APP, "java")):
        for f in files:
            if f.endswith(".java"):
                out.append(os.path.join(root, f))
    return out


def main():
    if not os.path.exists(PLATFORM):
        raise SystemExit("缺少 android.jar，请先安装 SDK platform android-34")

    # 0. 打包前离线布局自测：各态几何必须一致、按键不得被压缩，不通过则中止打包。
    layout_test = os.path.join(HERE, "layout-test", "run_layout_test.py")
    if os.path.exists(layout_test):
        run([sys.executable, layout_test])

    if os.path.isdir(BUILD):
        shutil.rmtree(BUILD)
    os.makedirs(BUILD)

    # 1. 编译资源
    res_zip = os.path.join(BUILD, "res.zip")
    run([AAPT2, "compile", "--dir", os.path.join(APP, "res"), "-o", res_zip])

    # 2. 链接资源 + 清单
    base_apk = os.path.join(BUILD, "base.apk")
    gen = os.path.join(BUILD, "gen")
    os.makedirs(gen)
    run([AAPT2, "link", "-o", base_apk, "-I", PLATFORM,
         "--manifest", os.path.join(APP, "AndroidManifest.xml"),
         "-A", os.path.join(APP, "assets"),
         "--java", gen,
         "--min-sdk-version", "21",
         "--target-sdk-version", "34",
         "--version-code", "94",
         "--version-name", VERSION,
         res_zip])

    # 3. 编译 Java
    classes = os.path.join(BUILD, "classes")
    os.makedirs(classes)
    srcs = java_sources()
    r_java = os.path.join(gen, "com", "shuangyin", "ime", "R.java")
    if os.path.exists(r_java):
        srcs.append(r_java)
    argfile = os.path.join(BUILD, "sources.txt")
    with open(argfile, "w", encoding="utf-8") as f:
        f.write("\n".join('"%s"' % s.replace("\\", "/") for s in srcs))
    run([JAVAC, "-encoding", "UTF-8", "--release", "8", "-Xlint:-options",
         "-classpath", PLATFORM, "-d", classes, "@" + argfile])

    # 4. dex：先把 class 目录打成 jar 再交给 d8，逐个列 class 文件会超出 Windows 命令行长度上限
    dex_out = os.path.join(BUILD, "dex")
    os.makedirs(dex_out)
    jar_path = os.path.join(BUILD, "classes.jar")
    if os.path.exists(jar_path):
        os.remove(jar_path)
    run([os.path.join(JDK, "bin", "jar.exe"), "cf", jar_path, "-C", classes, "."])
    run([D8, "--release", "--min-api", "21", "--lib", PLATFORM,
         "--output", dex_out, jar_path])

    # 5. 打包：base.apk + classes.dex
    unsigned = os.path.join(BUILD, "unsigned.apk")
    shutil.copyfile(base_apk, unsigned)
    with zipfile.ZipFile(unsigned, "a", zipfile.ZIP_DEFLATED) as z:
        z.write(os.path.join(dex_out, "classes.dex"), "classes.dex")

    # 6. 对齐 + 签名
    aligned = os.path.join(BUILD, "aligned.apk")
    run([ZIPALIGN, "-f", "-p", "4", unsigned, aligned])

    ks = KEYSTORE
    if not os.path.exists(ks):
        os.makedirs(os.path.dirname(ks), exist_ok=True)
        run([KEYTOOL, "-genkeypair", "-keystore", ks, "-storetype", "PKCS12",
             "-storepass", KS_PASS, "-alias", KS_ALIAS, "-keypass", KS_PASS,
             "-keyalg", "RSA", "-keysize", "2048", "-validity", "10000",
             "-dname", "CN=ShuangYin IME,O=ShuangYin,L=Guangzhou,ST=Guangdong,C=CN"])

    signed = os.path.join(BUILD, f"shuangyin-ime-{VERSION}.apk")
    run([APKSIGNER, "sign", "--ks", ks, "--ks-pass", "pass:" + KS_PASS,
         "--key-pass", "pass:" + KS_PASS, "--out", signed, aligned])

    final = os.path.join(PROJ, f"霜音输入法-{VERSION}.apk")
    shutil.copyfile(signed, final)
    print("APK:", final, round(os.path.getsize(final) / 1048576.0, 2), "MB")

    # 存档一份到 E 盘共享目录（每次打包自动执行）
    if os.path.isdir(ARCHIVE_DIR):
        dst = os.path.join(ARCHIVE_DIR, os.path.basename(final))
        try:
            shutil.copyfile(final, dst)
            print("ARCHIVE:", dst, round(os.path.getsize(dst) / 1048576.0, 2), "MB")
        except Exception as e:
            print("ARCHIVE: 复制失败 -", e)
    else:
        print("ARCHIVE: 跳过（目录不存在）", ARCHIVE_DIR)

    run([AAPT2, "dump", "badging", final])


if __name__ == "__main__":
    main()

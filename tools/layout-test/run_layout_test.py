# -*- coding: utf-8 -*-
"""打包前离线布局自测：纯 JVM 编译运行 LayoutTest，断言各态几何一致、按键未被压缩。
退出码非 0 表示断言失败，build_apk.py 会中止打包。"""
import os
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
PROJ = os.path.dirname(os.path.dirname(HERE))
SRC = os.path.join(PROJ, "app", "java", "com", "shuangyin", "ime", "ImeView.java")

BT = os.environ.get("SY_BUILDTOOLS", "")
if not BT or not os.path.isdir(BT):
    raise SystemExit("请先设置环境变量 SY_BUILDTOOLS")
JDK = os.path.join(BT, "jdk-17.0.2")
JAVAC = os.path.join(JDK, "bin", "javac.exe")
JAVA = os.path.join(JDK, "bin", "java.exe")

def main():
    if not os.path.exists(SRC):
        raise SystemExit("找不到源码: " + SRC)
    out = tempfile.mkdtemp(prefix="layout-test-")
    env = dict(os.environ)
    env["PATH"] = os.path.join(JDK, "bin") + os.pathsep + env.get("PATH", "")
    env["JAVA_HOME"] = JDK

    javac = [JAVAC, "-encoding", "UTF-8", "-d", out,
             os.path.join(HERE, "LayoutModel.java"),
             os.path.join(HERE, "LayoutTest.java")]
    p = subprocess.run(javac, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                       universal_newlines=True, encoding="utf-8", errors="replace", env=env)
    if p.stdout:
        print(p.stdout.rstrip())
    if p.returncode != 0:
        raise SystemExit("布局自测编译失败(%d)" % p.returncode)

    run = [JAVA, "-cp", out, "LayoutTest", SRC]
    p = subprocess.run(run, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                       universal_newlines=True, encoding="utf-8", errors="replace", env=env)
    if p.stdout:
        print(p.stdout.rstrip())
    if p.returncode != 0:
        raise SystemExit("布局自测未通过(%d)，已中止打包。" % p.returncode)

if __name__ == "__main__":
    main()

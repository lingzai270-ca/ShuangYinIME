# -*- coding: utf-8 -*-
import io
import os
import sys

F = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                 "data", "词库_输入法.tsv")
want = ["中国", "中华", "谢谢", "北京", "你好", "患难", "黄牛", "女孩", "时间", "银行"]
hit = {}
with io.open(F, encoding="utf-8") as fh:
    fh.readline()
    for line in fh:
        p = line.rstrip("\r\n").split("\t")
        if not p or not p[0]:
            continue
        if p[0] in want and p[0] not in hit:
            hit[p[0]] = line.strip()
for w in want:
    print(w, "=>", hit.get(w, "(缺)"))

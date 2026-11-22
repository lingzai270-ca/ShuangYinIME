# -*- coding: utf-8 -*-
import io
import os

F = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                 "data", "词库_输入法.tsv")
sub = ["中国", "中华", "谢谢", "北京", "你好"]
cnt = dict((s, 0) for s in sub)
samples = dict((s, []) for s in sub)
with io.open(F, encoding="utf-8") as fh:
    fh.readline()
    for line in fh:
        p = line.rstrip("\r\n").split("\t")
        if not p or not p[0]:
            continue
        w = p[0].strip()
        for s in sub:
            if s in w:
                cnt[s] += 1
                if len(samples[s]) < 5:
                    samples[s].append(w)
for s in sub:
    print(s, "contain:", cnt[s], samples[s])

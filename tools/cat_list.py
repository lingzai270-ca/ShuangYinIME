# -*- coding: utf-8 -*-
import collections
import io
import os

F = os.path.join(os.path.dirname(os.path.abspath(__file__)), "data", "词库_输入法.tsv")
cats = collections.Counter()
lens = {}
with io.open(F, encoding="utf-8") as fh:
    fh.readline()
    for line in fh:
        p = line.rstrip("\r\n").split("\t")
        if len(p) < 7:
            continue
        cats[p[6]] += 1
        lens.setdefault(p[6], set()).add(len(p[0]))
for k, v in cats.most_common():
    print("%-12s %6d  len=%s" % (k, v, sorted(lens[k])))
print("total cats", len(cats))

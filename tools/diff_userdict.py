# -*- coding: utf-8 -*-
import collections
import io
import os

BASE = os.path.dirname(os.path.abspath(__file__))
NEW = os.path.join(BASE, "..", "build", "userdict_src2", "词库_输入法.tsv")
OLD = os.path.join(BASE, "data", "词库_输入法.tsv")


def load(path):
    rows = []
    with io.open(path, encoding="utf-8") as fh:
        head = fh.readline().rstrip("\n").split("\t")
        for line in fh:
            p = line.rstrip("\r\n").split("\t")
            if len(p) < 7:
                continue
            rows.append(p)
    return head, rows


h1, old = load(OLD)
h2, new = load(NEW)
print("old rows", len(old), "new rows", len(new), "head same", h1 == h2)
c = collections.Counter(r[6] for r in new)
print("cats", c.most_common(8))
print("wordlen", sorted(collections.Counter(len(r[0]) for r in new).items()))
w = [float(r[5]) for r in new]
print("weight min/max", min(w), max(w))
ow = dict((r[0], r) for r in old)
added = [r for r in new if r[0] not in ow]
print("added words", len(added))
for r in added[:15]:
    print("  +", " | ".join(r))
nw = dict((x[0], x) for x in new)
lost = [r for r in old if r[0] not in nw]
print("removed words", len(lost))
for r in lost[:10]:
    print("  -", " | ".join(r))
for s in ["中国", "中华", "谢谢", "你好", "北京", "时间", "银行"]:
    hit = [r for r in new if r[0] == s]
    print(s, "=>", hit[0] if hit else "(缺)")

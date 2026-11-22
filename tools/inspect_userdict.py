# -*- coding: utf-8 -*-
import collections
import io
import os

F = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                 "build", "userdict_src", "词库_输入法.tsv")

rows = []
with io.open(F, encoding="utf-8") as fh:
    head = fh.readline().rstrip("\n").split("\t")
    for line in fh:
        p = line.rstrip("\r\n").split("\t")
        if len(p) < 7:
            continue
        rows.append(p)

print("rows", len(rows), "head", head)
cats = collections.Counter(r[6] for r in rows)
print("cats", cats.most_common())
ln = collections.Counter(len(r[0]) for r in rows)
print("wordlen", sorted(ln.items()))
w = [float(r[5]) for r in rows]
print("weight min/max", min(w), max(w))
print("weight top10", collections.Counter(r[5] for r in rows).most_common(6))
for r in rows[:3] + rows[3000:3005] + rows[50000:50005] + rows[-6:]:
    print(" | ".join(r))
mc = [r for r in rows if len(r[0]) > 1][:8]
for r in mc:
    print("MC:", " | ".join(r))
# 拼音串格式
syls = collections.Counter(len(r[1].split(" ")) if " " in r[1] else -1 for r in rows)
print("pinyin-space-split", syls.most_common(6))
print("sample pinyin", rows[0][1], "|", rows[8000][1], "|", rows[60000][1])
print("sample jianpin", rows[0][2], "|", rows[8000][2], "|", rows[60000][2])

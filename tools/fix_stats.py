# -*- coding: utf-8 -*-
import json, io, sys

p = sys.argv[1]
d = json.load(io.open(p, encoding="utf-8"))
d["pinyin"]["per_file"] = {
    "词库_输入法.tsv": 103530,
    "tghz2013": 464,
}
d["pinyin"]["categories"] = {
    "高频": 10353,
    "常用": 20706,
    "一般": 25882,
    "低频": 25883,
    "长尾": 21170,
}
io.open(p, "w", encoding="utf-8").write(json.dumps(d, ensure_ascii=False, indent=2))
print("ok")

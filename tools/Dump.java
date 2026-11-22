package com.shuangyin.ime;

import com.shuangyin.ime.core.BinDict;
import com.shuangyin.ime.core.Candidate;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/** 直接看词库里某个编码下存了哪些词、权重多少，用来比对两次构建的差异。 */
public class Dump {
    public static void main(String[] args) throws IOException {
        BinDict d = new BinDict(new File(args[0], "pinyin.bin"));
        String key = args[1];
        List<Candidate> out = new ArrayList<Candidate>();
        d.exact(0, key, 30, 1.0f, 511, out);
        StringBuilder sb = new StringBuilder(key + ": ");
        for (Candidate c : out) {
            sb.append(c.word).append('[').append(c.weight).append('/').append((int) c.score).append("] ");
        }
        System.out.println(sb.toString());
    }
}

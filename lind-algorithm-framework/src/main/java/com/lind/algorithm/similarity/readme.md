# similarity 包（com.lind.algorithm.similarity）

| 类 | 说明 |
|---|---|
| `SimHash` | 64 位文本指纹 + 汉明距离 |

```java
long fp = SimHash.hash64("推荐标题与正文...");
int d = SimHash.hammingDistance(fp, otherFp);
boolean near = SimHash.similar(textA, textB, 3);
```

可供 `bloom.recommend` 的内容指纹去重：先算 SimHash，再交给 Bloom / 精确相似度判断。分词为空白切分 + 字符 bigram，适合中英文短文本粗判。

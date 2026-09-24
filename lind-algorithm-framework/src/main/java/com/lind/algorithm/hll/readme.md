# hll 包（com.lind.algorithm.hll）

| 类 | 说明 |
|---|---|
| `HyperLogLog` | 概率型基数估计（UV / 去重计数） |

```java
HyperLogLog hll = new HyperLogLog(14);
hll.add("user:1");
hll.add("user:2");
hll.cardinality();
hll.merge(other); // 同精度合并
```

适合海量 UV、日活粗估；与 Bloom / Count-Min Sketch 同属概率结构，固定内存、有误差但无假阴性式「漏计」问题（估计偏高/偏低在理论误差带内）。

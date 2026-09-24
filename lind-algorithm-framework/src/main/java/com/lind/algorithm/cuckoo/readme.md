# cuckoo 包（com.lind.algorithm.cuckoo）

| 类 | 说明 |
|---|---|
| `CuckooFilter` | 可删除的概率型集合（相对标准 Bloom） |

```java
CuckooFilter filter = new CuckooFilter(1_000_000, 0.01);
filter.put("user:1");
filter.mightContain("user:1");
filter.delete("user:1");
```

适合需要解禁/过期单条元素、又希望比精确 Set 更省内存的场景。仍可能误判存在，删除后不会漏删已插入的指纹。

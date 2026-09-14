# bloom 包（com.lind.algorithm.bloom）

Bloom 过滤器：可能误判存在，不会漏判「一定不存在」。

| 场景 | 说明 |
|---|---|
| 缓存穿透防护 | 先判「一定不在」 |
| LSM / 字典粗筛 | 减少磁盘点查 |
| 海量去重粗判 | 内存远小于 HashSet |

```java
BloomFilter filter = new BloomFilter(1_000_000, 0.01);
filter.put("user:1");
filter.mightContain("user:1");
```

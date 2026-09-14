# skiplist 包（com.lind.algorithm.skiplist）

跳表有序 KV（`SkipListMap`），期望 O(log n)。

| 场景 | 说明 |
|---|---|
| Redis ZSET 思想 | 有序集合教学与轻量实现 |
| 内存有序索引 | 插入/查找/删除 |

```java
SkipListMap<Integer, String> map = new SkipListMap<>();
map.put(2, "b");
map.put(1, "a");
map.keys(); // [1, 2]
```

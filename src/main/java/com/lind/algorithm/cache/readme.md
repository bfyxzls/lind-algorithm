# cache 包（com.lind.algorithm.cache）

本地缓存淘汰策略。

| 类 | 说明 |
|---|---|
| `LruCache` | 最久未访问淘汰 |
| `LfuCache` | 最低频次淘汰 |

```java
LruCache<String, Object> cache = new LruCache<>(1024);
cache.put("k", "v");
cache.get("k");
```

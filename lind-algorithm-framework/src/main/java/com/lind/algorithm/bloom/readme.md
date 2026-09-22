# bloom 包（com.lind.algorithm.bloom）

Bloom 过滤器：可能误判存在，不会漏判「一定不存在」，标准 Bloom 不能单点删除。

| 场景 | 说明 |
|---|---|
| 缓存穿透防护 | 先判「一定不在」 |
| LSM / 字典粗筛 | 减少磁盘点查 |
| 海量去重粗判 | 内存远小于 HashSet |

## 业务场景子包

| 子包 | 场景 | 入口类 |
|---|---|---|
| `crawler` | 短链爬虫 URL 去重 | `BloomUrlDeduplicator` |
| `recommend` | 推荐系统重复推荐 / 内容入库去重 | `BloomRecommendDeduplicator` |
| `ads` | 广告曝光频控 | `BloomAdFrequencyControl` |
| `blacklist` | 黑名单前置判断 | `BloomBlacklist` |

详细流程见 [三大使用场景说明.md](三大使用场景说明.md)。

```java
BloomFilter filter = new BloomFilter(1_000_000, 0.01);
filter.put("user:1");
filter.mightContain("user:1");
```

### 1. 短链爬虫 URL 去重（`crawler`）

```java
UrlDedupStore store = new InMemoryUrlDedupStore();
BloomUrlDeduplicator dedup = new BloomUrlDeduplicator(store, 1_000_000, 0.01);

if (dedup.tryClaim(shortOrLongUrl)) {
    // 一定/确认未抓过：入队抓取
}
// 抓取完成后写入最终长链与内容指纹
dedup.markFetched(finalUrl, contentFingerprint);
```

- 写入前用 `UrlNormalizer` 去掉 fragment、统一 host、默认端口、排序 query。
- Bloom 未命中一定没抓过；命中后回源，假阳性仍可抓，避免漏抓。

### 2. 推荐去重（`recommend`）

```java
RecommendDedupStore store = new InMemoryRecommendDedupStore();
BloomRecommendDeduplicator dedup = new BloomRecommendDeduplicator(store, 1_000_000, 0.01);

List<String> candidates = dedup.filterUnseen(userId, recalledIds); // 或带 windowKey 按天
dedup.markRecommended(userId, shownContentId);
dedup.tryAdmitContent(simHashFingerprint); // 内容库去重
```

- 用户维度 key：`userId + contentId`（可选时间窗口）。
- 内容维度 key：内容指纹；命中后应交精确相似度判断。

### 3. 广告曝光频控（`ads`）

```java
AdImpressionStore store = new InMemoryAdImpressionStore();
BloomAdFrequencyControl freq = new BloomAdFrequencyControl(store, FrequencyWindow.DAY, 1_000_000, 0.01);

List<String> eligible = freq.filterEligible(userId, candidateAds, System.currentTimeMillis());
freq.markImpressed(userId, shownAdId, System.currentTimeMillis());
```

- Key：`userId + adId + 小时/天分片`。分片过期后整片丢弃，规避标准 Bloom 无法删除。
- 假阳性会少曝光，命中必须回源（Redis 计数 / 精确频控）。

### 4. 黑名单（`blacklist`）

```java
BlacklistStore store = new InMemoryBlacklistStore();
BloomBlacklist blacklist = new BloomBlacklist(store, 1_000_000, 0.01);

blacklist.block("user:1");
if (blacklist.isBlocked("user:1")) {
    // 拒绝
}
blacklist.unblock("user:1"); // 删库后重建
```

1. **写入**：先落真相源，再 `put`。
2. **启动**：构造时全量灌入 Bloom。
3. **查询**：一定不在 → 不回源；可能在 → 回源确认。
4. **删除**：标准 Bloom 不能单点删，解禁后 `reload` 重建。

### 持久化

- 文件序列化 / RedisBloom（`BF.ADD` / `BF.EXISTS`）/ Redis Bitmap。
- 各场景的 `*Store` 换成 DB 或 Redis 即可，入口类 API 不变。
- 建议定期快照或按时间分片，控制误判率与过期。

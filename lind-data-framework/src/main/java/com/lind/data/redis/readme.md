# redis 包（com.lind.data.redis）

基于业界流行客户端 **[Jedis](https://github.com/redis/jedis)** 的 Redis 场景封装（无 Spring 依赖）。

## 入口

```java
try (LindRedis redis = LindRedis.connect("127.0.0.1", 6379)) {
    redis.cache().set("user:1", "{\"name\":\"lind\"}", Duration.ofMinutes(10));
}
```

也可注入自定义 `RedisCommands`（便于单测 / 多机房封装）。

## 场景一览

| 组件 | Redis 结构 / 命令 | 场景 |
|---|---|---|
| `RedisCache` | String + EX | 分布式缓存 |
| `RedisLock` | SET NX PX + Lua | 分布式锁 |
| `RedisRateLimiter` | ZSET 时间戳滑动窗口 | 分布式限流 |
| `RedisBloomFilter` | SETBIT / GETBIT | 布隆过滤（无需 RedisBloom 模块） |
| `RedisGeo` | GEOADD / GEORADIUS | 附近的人 / 门店 |
| `RedisRanking` | ZSET | 排行榜 Top-N |
| `RedisListQueue` | List / BRPOPLPUSH | 简易消息队列 |
| `RedisDelayQueue` | ZSET score=毫秒时间戳 | 延迟队列 |
| `RedisPubSub` | PUBLISH / SUBSCRIBE | 发布订阅 |
| `RedisBitmap` | BIT | 签到 / 标记 |
| `RedisHyperLogLog` | PFADD / PFCOUNT | UV 近似统计 |
| `RedisSocialGraph` | Set 交并差 | 关注 / 共同关注 / 推荐雏形 |
| `RedisIdGenerator` | INCR / INCRBY | 分布式 ID / 全局序列 |

## 示例片段

```java
// 分布式锁
RedisLock lock = redis.lock("order:pay:1001", Duration.ofSeconds(10));
if (lock.tryLock()) {
    try { /* business */ } finally { lock.unlock(); }
}

// 限流：1 分钟 100 次
redis.rateLimiter("api:/order", 100, Duration.ofMinutes(1)).tryAcquire();

// 延迟队列
redis.delayQueue("jobs:delay").scheduleAfterMillis("close-order-1", 30_000);
List<String> due = redis.delayQueue("jobs:delay").pollDue();

// 共同关注
redis.social().follow("u1", "u2");
redis.social().commonFollowing("u1", "u3");

// 全局序列
long id = redis.idGenerator("seq:order").nextId();
```

## 依赖

父 POM 已管理 `redis.clients:jedis`。本模块直接引入 Jedis 连接池（`JedisPooled`）。

## 注意

- Pub/Sub 的 `subscribe` 会阻塞当前线程，建议独立线程/进程。
- 分布式锁默认不自动续期；长任务请自行看门狗或加大 lease。
- Bloom 为 BIT 自实现，兼容原版 Redis；若已装 RedisBloom 模块可后续扩展 `BF.*`。
- 单测通过 mock `RedisCommands`，不强制本机启动 Redis。

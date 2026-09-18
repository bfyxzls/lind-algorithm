# redis 包（com.lind.data.starter.redis）

基于 **Spring Data Redis**（`StringRedisTemplate`）的场景封装，由 `RedisStarterAutoConfiguration` 自动装配 `LindSpringRedis`。

## 使用场景

| 组件 | Redis 结构 / 命令 | 典型场景 |
|---|---|---|
| `SpringRedisCache` | String + TTL | 会话 / 热点数据缓存 |
| `SpringRedisLock` | SET NX PX + Lua 解锁 | 下单、支付等防并发 |
| `SpringRedisRateLimiter` | ZSET 滑动窗口 | 接口 / IP 限流 |
| `SpringRedisDelayQueue` | ZSET score=执行时间 | 延时关单、延时通知 |
| `SpringRedisIdGenerator` | INCR / INCRBY | 全局订单号、序列 |

## 启用条件

1. classpath 存在 `StringRedisTemplate`（通常引入 `spring-boot-starter-data-redis`）
2. 容器中已有 `StringRedisTemplate` Bean（Boot 自动配置）
3. `lind.data.redis.enabled=true`（默认开启）

```yaml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
lind:
  data:
    redis:
      enabled: true
```

## 调用方法

```java
@Autowired
LindSpringRedis redis;

// 缓存
redis.cache().set("user:1", "{\"name\":\"lind\"}", Duration.ofMinutes(10));
Optional<String> v = redis.cache().get("user:1");
redis.cache().delete("user:1");

// 分布式锁
SpringRedisLock lock = redis.lock("order:pay:1001", Duration.ofSeconds(30));
if (lock.tryLock()) {
    try {
        // business
    } finally {
        lock.unlock();
    }
}

// 限流：每秒最多 100 次
boolean ok = redis.rateLimiter("api:/order", 100, Duration.ofSeconds(1)).tryAcquire();

// 延迟队列
redis.delayQueue("jobs:delay").scheduleAfterMillis("close-order-1", 30_000);
List<String> due = redis.delayQueue("jobs:delay").pollDue();

// 分布式 ID
long id = redis.idGenerator("seq:order").nextId();
long batch = redis.idGenerator("seq:order").nextId(100);
```

也可直接注入后取原生模板：`redis.template()`。

## 注意

- 锁默认不自动续期；长任务请加大 lease 或自建看门狗。
- 限流与延迟队列依赖本机时钟；多机请保证 NTP。
- 单测 mock `StringRedisTemplate` 及其 `opsForValue` / `opsForZSet`，无需本机 Redis。

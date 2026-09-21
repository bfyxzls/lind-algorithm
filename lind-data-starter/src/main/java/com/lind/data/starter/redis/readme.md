# redis 包（com.lind.data.starter.redis）

基于 **Spring Data Redis**（`StringRedisTemplate`）的场景封装，由 `RedisStarterAutoConfiguration` 自动装配 `LindSpringRedis`。

## 使用场景

| 组件 | Redis 结构 / 命令 | 典型场景 |
|---|---|---|
| `SpringRedisCache` | String + TTL | 会话 / 热点数据缓存 |
| `SpringRedisLock` | SET NX PX + Lua 解锁 / 看门狗续期 | 下单、支付等防并发；长任务用看门狗 |
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

// 分布式锁 —— 推荐：run/tryRun 自动 unlock，无需 finally
redis.tryRun("order:pay:1001", () -> {
    // 业务；方法结束（含异常）自动释放锁，看门狗同步停止
});

redis.run("order:pay:1001", () -> { /* 拿不到锁抛 IllegalStateException */ });

long orderId = redis.lock("seq:order").supply(() -> {
    return redis.idGenerator("seq:order").nextId();
});

// 仍支持手动 tryLock（有 Cleaner / 持有线程死亡 双重兜底）
SpringRedisLock lock = redis.lock("order:pay:1002");
if (lock.tryLock()) {
    // 忘记 unlock 时：① 锁对象被 GC 后 Cleaner 释放；② 持有线程已死亡则看门狗释放
    // 线程池场景请优先用 tryRun/run
}

// 固定租约（不续期，到期自动释放）
redis.lock("order:pay:1003", Duration.ofSeconds(30)).tryRun(() -> { /* ... */ });

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

- **推荐 API**：`tryRun` / `run` / `call` / `supply`，作用域结束自动 unlock，不必写 finally。
- **看门狗**：`redis.lock(key)` 默认租约 30s、按 lease/3 续期；`lock(key, lease)` 为固定租约不续期。
- **自动兜底**：① 持有线程已死亡 → 看门狗停续期并释放；② 锁对象被 GC 且未 unlock → Cleaner 释放（防无限续期）。
- **线程池注意**：Web 容器等复用线程上，持有线程不会死亡，请用 `tryRun`/`run`，不要只 `tryLock` 后丢掉引用却指望线程死亡检测。
- 限流与延迟队列依赖本机时钟；多机请保证 NTP。
- 单测 mock `StringRedisTemplate`，无需本机 Redis。

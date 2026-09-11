# lind-algorithm

通用算法与数据结构库，纯 JDK（17+），无 Spring 运行时依赖。

原隶属 [lind-framework](https://github.com/bfyxzls/lind-framework)，现已独立维护。

## 模块一览

| 包 | 内容 |
|---|---|
| `tree` | Trie / 二叉树 / 红黑树 / B+ / R 树 / AST / CST / 决策树 / 流程树 / 多叉树 / LSM / 菜单树 |
| `fsm` | 有限状态机（Guard / Action / Entry-Exit） |
| `wheel` | 层级哈希时间轮（延时任务） |
| `loadbalance` | 随机 / 轮询 / LRU / LFU / 一致性哈希 / 平滑加权轮询 |
| `ratelimit` | 令牌桶 / 漏桶 / 滑动窗口 |
| `circuit` | 熔断器（Closed / Open / Half-Open） |
| `retry` | 指数退避重试（可选抖动） |
| `bloom` | Bloom 过滤器 |
| `skiplist` | 跳表有序 KV |
| `cache` | LRU / LFU 本地缓存 |
| `id` | 雪花算法 ID |
| `bitmap` | 位图与集合运算 |
| `topk` | Top-K 计数 / Count-Min Sketch |
| `schedule` | 简化 Cron 下次触发时间 |
| `graph` | Dijkstra / 拓扑排序 / 环检测 |
| `unionfind` | 并查集 |
| `stringmatch` | KMP / AC 自动机 |

## 依赖

```xml
<dependency>
    <groupId>com.lind</groupId>
    <artifactId>lind-algorithm</artifactId>
    <version>1.0.0</version>
</dependency>
```

安装到本地：

```bash
mvn clean install
```

## 快速示例

### 状态机

```java
StateMachine<OrderState, OrderEvent, OrderContext> sm = StateMachine
    .<OrderState, OrderEvent, OrderContext>builder()
    .initial(OrderState.CREATED)
    .context(ctx)
    .from(OrderState.CREATED).on(OrderEvent.PAY).to(OrderState.PAID)
    .build();
sm.fire(OrderEvent.PAY);
```

### 时间轮延时任务

```java
try (HashedWheelTimer timer = new HashedWheelTimer()) {
    timer.newTimeout(t -> cancelUnpaidOrder(orderId), 30, TimeUnit.MINUTES);
}
```

### 负载均衡

```java
String node = LoadBalancers.ROUND_ROBIN.balancer()
    .select("OrderService", List.of("a:8080", "b:8080", "c:8080"));
```

### 限流 / 熔断 / 重试

```java
RateLimiter limiter = new TokenBucketRateLimiter(100, 50);
CircuitBreaker breaker = new CircuitBreaker(5, 2, 10, TimeUnit.SECONDS);
RetryTemplate retry = RetryTemplate.builder().maxAttempts(3).jitter(true).build();

if (limiter.tryAcquire()) {
    retry.execute(() -> breaker.execute(() -> callRemote()));
}
```

## 构建与测试

```bash
mvn clean test
```

## License

Apache License 2.0

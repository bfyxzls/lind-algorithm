# ratelimit 包（com.lind.algorithm.ratelimit）

限流算法，纯 JDK。

| 类 | 说明 | 适合 |
|---|---|---|
| `TokenBucketRateLimiter` | 令牌桶，允许突发 | API 网关、接口 QPS |
| `LeakyBucketRateLimiter` | 漏桶，出水更平滑 | 需要整形流量 |
| `SlidingWindowRateLimiter` | 滑动窗口计数 | 精确窗口内次数限制 |

```java
RateLimiter limiter = new TokenBucketRateLimiter(100, 50);
if (limiter.tryAcquire()) {
    // 放行
}
```

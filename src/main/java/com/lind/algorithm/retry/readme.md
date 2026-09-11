# retry 包（com.lind.algorithm.retry）

重试模板：最大次数 + 指数退避 + 可选抖动。

```java
RetryTemplate retry = RetryTemplate.builder()
    .maxAttempts(3)
    .initialBackoff(100, TimeUnit.MILLISECONDS)
    .multiplier(2.0)
    .jitter(true)
    .build();
retry.execute(() -> callRemote());
```

可与时间轮组合做延迟重试；与熔断器组合避免重试风暴。

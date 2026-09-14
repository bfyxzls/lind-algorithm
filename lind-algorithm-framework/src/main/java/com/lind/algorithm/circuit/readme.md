# circuit 包（com.lind.algorithm.circuit）

熔断器：`CLOSED → OPEN → HALF_OPEN`。

| 场景 | 说明 |
|---|---|
| 下游超时/失败保护 | 连续失败达阈值后开闸 |
| 自动半开试探 | 冷却后放行少量请求恢复 |

```java
CircuitBreaker breaker = new CircuitBreaker(5, 2, 10, TimeUnit.SECONDS);
breaker.execute(() -> callRemote());
```

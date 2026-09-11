# wheel 包（com.lind.algorithm.wheel）

可落地的**层级哈希时间轮**，面向大批量**延时任务**（订单超时、延迟消息、会话过期等）。纯 JDK，无 Spring 依赖。

模型对齐业界常见实现：**Kafka TimingWheel**（层级 + DelayQueue 精准推进）+ **Netty HashedWheelTimer** 风格的 `Timeout` 句柄 API。

## 核心类型

| 类型 | 说明 |
|---|---|
| `HashedWheelTimer` | 定时器：`newTimeout` / `stop` / `pendingTasks` |
| `TimerTask` | 到期回调 `run(Timeout)` |
| `Timeout` | 调度句柄：`cancel` / `isCancelled` / `isExpired` |

## 示例

```java
try (HashedWheelTimer timer = new HashedWheelTimer()) {
    Timeout timeout = timer.newTimeout(t -> {
        // 订单超时关单等业务
        cancelUnpaidOrder(orderId);
    }, 30, TimeUnit.MINUTES);

    // 用户已支付则取消延时任务
    // timeout.cancel();
}
```

自定义精度与执行线程池：

```java
ExecutorService workers = Executors.newFixedThreadPool(4);
HashedWheelTimer timer = new HashedWheelTimer(
    10,   // tickMs：调度精度约 10ms
    20,   // wheelSize：每层 20 格，interval = 200ms，更长延时进上层轮
    workers
);
```

## 为何适合延时任务

| 对比 | 说明 |
|---|---|
| vs `ScheduledThreadPoolExecutor` | 海量延时任务时 DelayQueue 堆操作更重；时间轮按槽批量到期，摊还成本更低 |
| vs 单层巨大时间轮 | 层级溢出轮覆盖小时级延时，无需百万级槽数组 |
| 精度 | 取决于 `tickMs`，不适合亚毫秒硬实时 |

## 与 lind-common 现有实现

`lind-common` 中另有 `hashwheel` / `wheel4j` 示例实现。本模块提供**独立、可测试、可关闭**的算法组件，便于非 Spring / 纯算法复用；业务侧可按需择一使用。

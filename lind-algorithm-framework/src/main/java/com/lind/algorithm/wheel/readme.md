# wheel 包（com.lind.algorithm.wheel）

可落地的**层级哈希时间轮**，面向大批量**延时任务**（订单超时、延迟消息、会话过期等）。纯 JDK，无 Spring 依赖。

模型对齐业界常见实现：**Kafka TimingWheel**（层级 + DelayQueue 精准推进）+ **Netty HashedWheelTimer** 风格的 `Timeout` 句柄 API。

## 核心类型

| 类型 | 说明 |
|---|---|
| `HashedWheelTimer` | 定时器：`newTimeout` / `scheduledTasks` / `stop` / `pendingTasks` |
| `ScheduledTask` | 快照项：任务句柄 + 计划执行时间 `deadline()` |
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

## bean组件，业务层方便插入新任务
生命周期大致是：
```
new HashedWheelTimer()     ← 初始化（空轮）
        │
        ▼
newTimeout / newTimeout…   ← 运行期可反复写入（API 插件就走这里）
        │
        ▼
stop() / close()           ← 之后不能再写
```
把 进程级单例 的时间轮注入插件，HTTP/RPC 里只调 newTimeout：
```java
@Component
public class DelayTaskPlugin {

    private final HashedWheelTimer timer; // 应用启动时建好，全局共用

    public DelayTaskPlugin(HashedWheelTimer timer) {
        this.timer = timer;
    }

    /** 插件 API：频繁写入延时任务 */
    public Timeout schedule(String bizId, long delayMs, Runnable action) {
        return timer.newTimeout(t -> action.run(), delayMs, TimeUnit.MILLISECONDS);
    }

    /** 查看轮上尚未到期的任务及其执行时间点 */
    public List<ScheduledTask> pending() {
        return timer.scheduledTasks();
    }
}
```
web api调用组件
```
// Controller / 插件入口 —— 每次请求写一条即可
timeoutHandle = delayTaskPlugin.schedule(orderId, 30 * 60_000L, () -> cancelOrder(orderId));
```

# 时间轮是否要持久化

**业界通常不持久化时间轮本身**，而是把「要不要恢复」交给业务状态或外部可靠存储。时间轮是内存调度器，不是任务的权威数据源。

### 为什么一般不持久化时间轮

1. **职责不对**：时间轮负责「什么时候触发」，不负责「业务事实是什么」。进程挂了，内存里的指针、槽位、回调对象本来就会丢；真正该活下来的是业务事实（未支付订单、未发消息等）。
2. **回调难序列化**：任务往往是 lambda / 闭包，带着 DB 连接、Spring Bean、本地状态，没法可靠落盘再反序列化执行。
3. **重启后时钟语义变了**：停机 10 分钟再起来，原来「还有 5 分钟到期」的任务该立刻执行还是按原绝对时间？这要业务定，不是时间轮能统一解决的。
4. **实现成本高、收益低**：持久化槽位、层级溢出、取消、去重、幂等，复杂度接近再做一个延迟队列中间件。

你们库里的 `HashedWheelTimer` 也是这个定位：进程内高性能调度，`stop` 后任务自然清空。

### 业界常见做法

| 场景 | 做法 |
|---|---|
| 订单超时关单、会话过期 | 权威数据在 DB；重启后扫「未支付且超时」的订单，再 `newTimeout`，或干脆定时扫库 |
| 延迟消息、可靠延时 | Kafka / RocketMQ 延迟消息、Redis ZSET、DB 任务表 + 调度器 |
| Netty / 本地缓存过期 | 不持久化；丢了就重建连接或重新加载 |
| 分布式、可恢复的定时任务 | XXL-JOB、Quartz + DB，或自建 `task` 表（id、execute_at、payload、status） |

模式可以概括成：

```text
持久层（真相） ──重启扫描/订阅──► 时间轮（加速触发）
     ▲                                    │
     └──────── 任务执行 / 幂等写回 ─────────┘
```

时间轮只是热路径上的加速器；冷启动时从持久层把「未来要执行」的任务重新灌进去即可。

### 什么时候才考虑「持久化延时」

需要同时满足大致这些条件时，才值得上可靠延时，而不是给时间轮加磁盘：

- 任务丢失会造成资损或违规（关单、扣款、履约）
- 进程会频繁重启 / 滚动发布
- 需要跨实例故障转移

即便如此，持久化的通常是**任务记录**（绝对到期时间 + 业务主键 + 状态），而不是时间轮的槽位结构。启动时：读未完成任务 → 算剩余 delay → 再丢进时间轮。

### 推荐状态机（生产最小集）

```text
PENDING --claim--> RUNNING --ok--> DONE
   |                  |
 cancel            fail/retry
   v                  v
CANCELLED         PENDING(新 execute_at) 或 DEAD
```

| 步骤 | 正确做法 |
|---|---|
| 写入 | **先写库**（`PENDING` + 绝对时间 `execute_at`），成功后再 `newTimeout` |
| 启动 | 查 `PENDING`（及卡住的 `RUNNING`），`delay = max(0, execute_at - now)` 灌轮 |
| 到期 | **先 CAS** `PENDING → RUNNING`（抢到才执行），**不要**一出轮就删库 |
| 完成 | 业务成功后再 `DONE` / 删除；失败则改回 `PENDING` 并推迟 `execute_at` 再入轮 |
| 取消 | 库改为 `CANCELLED`，并 `Timeout.cancel()` |
| 列表 | 以 DB 为准；轮上 `scheduledTasks()` 仅运维辅助 |

**错误示范**：从时间轮取出任务就删除持久化 → 取出后、业务跑完前进程崩溃 = 任务永久丢失。

出轮只表示「该触发了」；删库/完成看**业务结果**，不看出轮动作。

本仓库演示实现见独立模块 `lind-delay-task-web`（MySQL 表 + 启动灌轮 + claim + 管理页）。

### 结论

- **不需要、也不建议持久化时间轮本体。**
- 要可靠：持久化业务任务，重启后重新调度；执行端做幂等。
- 要超大规模可靠延时：用消息队列 / 任务表，时间轮只做单机内存优化。

一句话：**时间轮管快，持久层管准；删库看业务结果，不看出轮动作。**

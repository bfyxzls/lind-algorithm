# lind-algorithm

通用算法与数据结构库，纯 JDK（17+），无 Spring 运行时依赖。

原隶属 [lind-framework](https://github.com/bfyxzls/lind-framework)，现已独立维护。

## 模块一览

| 包 | 内容 |
|---|---|
| `com.lind.algorithm.tree` | Trie / BinaryTreeNode / RedBlackTree / 磁盘 LsmTree / Tree |
| `com.lind.algorithm.fsm` | 有限状态机 StateMachine（Fluent API：Guard / Action / Entry-Exit） |
| `com.lind.algorithm.wheel` | 层级哈希时间轮 HashedWheelTimer（延时任务 / 可取消） |
| `com.lind.algorithm.loadbalance` | 负载均衡：随机 / 轮询 / LRU / LFU / 一致性哈希 / 平滑加权轮询 |

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

## 构建与测试

```bash
mvn clean test
```

## License

Apache License 2.0

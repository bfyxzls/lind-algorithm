# fsm 包（com.lind.algorithm.fsm）

业界常见的**有限状态机（FSM）**实现，API 风格参考 Stateless4j / Squirrel Fluent 配置，纯 JDK、无 Spring 依赖。

## 核心类型

| 类型 | 说明 |
|---|---|
| `StateMachine` | 状态机实例：`fire` / `canFire` / `tryFire` |
| `StateMachine.Builder` | Fluent 配置：`from().on().when().action().to()` |
| `TransitionGuard` | 守卫条件 |
| `TransitionAction` | 转换动作 |
| `StateAction` | onEntry / onExit |
| `TransitionException` | 非法转换 |

## 示例（订单）

```java
enum OrderState { CREATED, PAID, SHIPPED, COMPLETED, CANCELLED }
enum OrderEvent { PAY, SHIP, COMPLETE, CANCEL }

StateMachine<OrderState, OrderEvent, OrderContext> sm = StateMachine
    .<OrderState, OrderEvent, OrderContext>builder()
    .initial(OrderState.CREATED)
    .context(ctx)
    .from(OrderState.CREATED)
        .on(OrderEvent.PAY).action((f, t, e, c) -> pay(c)).to(OrderState.PAID)
        .on(OrderEvent.CANCEL).to(OrderState.CANCELLED)
    .from(OrderState.PAID)
        .on(OrderEvent.SHIP).to(OrderState.SHIPPED)
        .on(OrderEvent.CANCEL).when(c -> c.isCancelable()).to(OrderState.CANCELLED)
    .build();

sm.fire(OrderEvent.PAY);
```

## 与框架选型

| 方案 | 场景 |
|---|---|
| 本模块 `lind-algorithm` FSM | 轻量嵌入、无 Spring、订单/设备/工单等业务状态 |
| Spring State Machine | 深度 Spring 集成、持久化、分布式 |
| Squirrel / Stateless4j | 需要层次状态、并行状态等高级特性时 |

当前实现覆盖：**扁平 FSM + Guard + Action + Entry/Exit + Listener**。层次 / 并行状态可按需后续扩展。

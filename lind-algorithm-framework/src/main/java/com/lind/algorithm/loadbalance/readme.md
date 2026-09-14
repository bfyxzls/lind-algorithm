# loadbalance 包（com.lind.algorithm.loadbalance）

从 `lind-common` 的 `lb` 包迁移并修复后的负载均衡算法，纯 JDK。

## 策略

| 类型 | 类 | 说明 |
|---|---|---|
| 随机 | `RandomLoadBalancer` | `ThreadLocalRandom` |
| 轮询 | `RoundRobinLoadBalancer` | 按 key 计数 + `floorMod` |
| LRU | `LruLoadBalancer` | 优先选最近最少被选中的节点 |
| LFU | `LfuLoadBalancer` | 优先选累计次数最少的节点 |
| 一致性哈希 | `ConsistentHashLoadBalancer` | MD5 环 + 虚拟节点 |
| 平滑加权轮询 | `WeightedRoundRobinLoadBalancer` | Nginx 算法（原 common 文档有提但未实现） |

## 示例

```java
LoadBalancer lb = LoadBalancers.ROUND_ROBIN.balancer();
String node = lb.select("OrderService", List.of("a:8080", "b:8080", "c:8080"));

WeightedRoundRobinLoadBalancer wrr = new WeightedRoundRobinLoadBalancer();
String n = wrr.select("api", List.of(
    new WeightedNode("a", 5),
    new WeightedNode("b", 1),
    new WeightedNode("c", 1)
));
```

## 相对原 `lind-common.lb` 的修复

1. **空节点集**：统一校验，避免 NPE / 负下标  
2. **轮询溢出**：`count % length` 在 `AtomicInteger` 溢出为负时会越界 → 改为 `Math.floorMod`  
3. **LRU / LFU 竞态**：`putIfAbsent` 失败后仍写本地 map；`LinkedHashMap`/`HashMap` 未同步 → `computeIfAbsent` + 按 map 加锁；LRU 同步节点禁用 `Map.putIfAbsent`（access-order 会 afterNodeAccess）  
4. **LFU**：去掉 slf4j；并列最小次数时按地址字典序稳定选择  
5. **一致性哈希**：UTF-8 用 `StandardCharsets`；空节点校验  
6. **命名**：去掉 XXL-RPC 遗留字段名 `xxlRpcInvokerRouter`

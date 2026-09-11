# graph 包（com.lind.algorithm.graph）

有向加权图：`Dijkstra`、拓扑排序、环检测。

| 场景 | 说明 |
|---|---|
| 依赖构建顺序 | 拓扑排序 |
| 最短路径 | 路由 / 代价最小 |
| 流程环检测 | `hasCycle` |

```java
DirectedGraph<String> g = new DirectedGraph<>();
g.addEdge("A", "B", 1);
g.shortestPath("A", "B");
```

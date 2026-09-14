# unionfind 包（com.lind.algorithm.unionfind）

并查集：路径压缩 + 按秩合并。

| 场景 | 说明 |
|---|---|
| 连通分量 | 社交关系、集群合并 |
| 等价类合并 | 账号打通 |

```java
UnionFind<Integer> uf = new UnionFind<>();
uf.union(1, 2);
uf.connected(1, 2);
```

# 场景二：mmap + 量化检索（`com.lind.depth.mmap`）

对应 [`docs/工程化分析.md`](../../../../../../docs/工程化分析.md) **场景二**。

## 问题

千万级向量全量 `List<float[]>` 进堆 → 几十 GB → GC 崩溃。瓶颈往往是**堆内存**，不是 QPS。

## 本包做法（教学简化版）

| 组件 | 作用 |
|---|---|
| `HeapVectorIndex` | 玩具：全量 float 进堆，暴力扫 |
| `MmapFloatMatrix` | 冷数据：float 矩阵落盘 + `MappedByteBuffer` 按需读 |
| `ProductQuantizer` | PQ：高维切段，每段 1 字节码本编号 |
| `IvfIndex` | IVF：粗聚类分桶，查询只扫近邻桶 |
| `QuantizedMmapIndex` | 工程组合：码本驻内存，量化码走 mmap |

> 不是完整 Faiss；目标是把「不进堆 + 压缩 + 分桶」三条思想跑通，并能用测试对比内存语义与检索路径。

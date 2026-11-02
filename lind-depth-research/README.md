# lind-depth-research

对照设计文档 [`docs/工程化分析.md`](../docs/工程化分析.md)，把「玩具服务 → 工程级后端」拆成可运行的研究代码。

| 场景 | 文档章节 | 包 | 状态 |
|---|---|---|---|
| 一：流式生成 · 攒包 + DirectBuffer | §场景一 | `com.lind.depth.stream` | ✅ |
| 二：mmap + 量化检索思想 | §场景二 | `com.lind.depth.mmap` | ✅ |
| 三：WAL + 断点续传 | §场景三 | `com.lind.depth.resume` | ✅ |

```bash
mvn -pl lind-depth-research -am test
```

---

## 场景一：流式不是「有 Channel 就叫流式」

| 类 | 行为 |
|---|---|
| `PerTokenStreamWriter` | 每 token 立刻 flush（玩具） |
| `CoalescingStreamWriter` | `allocateDirect` 攒包，阈值 / 时间窗再 flush（工程） |

详见 [`stream/readme.md`](src/main/java/com/lind/depth/stream/readme.md)。

---

## 场景二：瓶颈往往是堆，不是 QPS

| 类 | 行为 |
|---|---|
| `HeapVectorIndex` | 全量 `float[]` 进堆暴力扫（玩具） |
| `MmapFloatMatrix` / `MmapCodeMatrix` | 矩阵 / PQ 编码落盘 + mmap |
| `ProductQuantizer` | 乘积量化：高维 → 每段 1 字节 |
| `IvfIndex` | 粗聚类分桶，查询只扫近邻桶 |
| `QuantizedMmapIndex` | IVF + PQ + mmap 组合（工程） |

详见 [`mmap/readme.md`](src/main/java/com/lind/depth/mmap/readme.md)。

---

## 场景三：兜底是无感恢复，不是打日志

| 类 | 行为 |
|---|---|
| `FileWal` | 预写进度（先记状态再承诺） |
| `InMemoryEventStream` | Redis Stream 内存替身 |
| `StreamingGenerator` | 生成 → WAL → Stream → 推送 |
| `ResumeService` | 按 `Last-Event-ID` 续推 |
| `ClientEventDeduper` | 客户端按 id 幂等去重 |
| `ToyRegenerateFromScratch` | 断线后整段重生成（玩具） |

详见 [`resume/readme.md`](src/main/java/com/lind/depth/resume/readme.md)。

---

## 设计原则

- **可对照**：坏做法与好做法并排，用测试里的数字说话  
- **纯 JDK**：不绑 Faiss / Redis；思想可直接迁到生产组件  
- **跟文档走**：包内 `readme.md` 与类注释指回 `工程化分析.md`  

# 场景一：流式攒包写出（`com.lind.depth.stream`）

对应 [`docs/工程化分析.md`](../../../../../../docs/工程化分析.md) **场景一**。

## 问题

框架里每来一个 token 就 `writeAndFlush`：

- 数据在 JVM 堆 ↔ 堆外 ↔ 内核之间反复拷贝  
- 系统调用次数 ≈ token 数  
- TCP 小包风暴，CPU 被「搬运」吃满  

## 本包做法

1. **`ByteBuffer.allocateDirect`**：缓冲区在堆外，减少一次堆内拷贝（Java 版「靠近零拷贝」）。  
2. **攒包**：`position >= flushBytes`（默认接近 MTU 有效载荷 ~1400）再写出。  
3. **时间窗兜底**：即使没满包，超过 `maxDelay`（默认 10ms）也 flush，控制首包延迟。  
4. **对照基线**：`PerTokenStreamWriter` 刻意每 token 一 flush，方便在测试里对比 `flushCount`。

## 阅读顺序

`TokenSource` → `FlushPolicy` → `ByteSink` → `CoalescingStreamWriter` / `PerTokenStreamWriter` → 测试。

心智模型（Heap vs Direct、flip/clear、transferTo、为何要攒包）见
[`lind-architecture` 的 `nio/readme.md`](../../../../../lind-architecture/src/main/java/com/lind/architecture/nio/readme.md)。

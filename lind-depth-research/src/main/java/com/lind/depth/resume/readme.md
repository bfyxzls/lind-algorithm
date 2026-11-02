# 场景三：WAL + 断点续传（`com.lind.depth.resume`）

对应 [`docs/工程化分析.md`](../../../../../../docs/工程化分析.md) **场景三**。

## 问题

断线后「整段重生成」→ 用户看见失败/重复。工程目标是**无感恢复**：带 `Last-Event-ID` 从断点续推，客户端按 ID 去重。

## 本包做法（纯 JDK，不绑 Redis）

| 组件 | 对应文档 |
|---|---|
| `FileWal` | WAL：先记 offset，再对外承诺 |
| `InMemoryEventStream` | Redis Stream 的内存替身（稳定 message id + 内容） |
| `StreamingGenerator` | 生成 → 写 WAL → 写 Stream → 可推送给客户端 |
| `ResumeService` | 根据 Last-Event-ID 从断点续读 |
| `ClientEventDeduper` | 客户端按 id 幂等去重 |
| `ToyRegenerateFromScratch` | 玩具：断线后从头再生成 |

真实生产把 `InMemoryEventStream` 换成 Redis Stream / Kafka 即可，协议头仍是 SSE 的 `Last-Event-ID`。

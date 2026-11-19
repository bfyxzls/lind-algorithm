# lind-java-foundation

Java **底座补强**独立模块：把日常业务里用得少、却又关键的三大块收在一起——

1. **IO 流**（Reader / Writer / Stream、缓冲、文件与 Socket）  
2. **NIO**（ByteBuffer、DirectBuffer、零拷贝思想）  
3. **锁与并发原语**（丢失更新、ReentrantLock + Condition）

> **学习请从本文件进入**，再点进各包文档与 demo。  
> 架构模式（ToyDoc SDK / elasticsearch-java 对照）仍在 [`lind-architecture`](../lind-architecture/README.md)。

```bash
mvn -pl lind-java-foundation -am test
```

---

## 三大块导航

| # | 主题 | 入口文档 | 包 | 关键测试 |
|---|---|---|---|---|
| **一** | IO 流 | [`io/mental-model.md`](src/main/java/com/lind/foundation/io/mental-model.md) | `com.lind.foundation.io` | `StreamFamiliesDemoTest`、`WriterDemoTest`、`BufferedFileCopyDemoTest`、`CsvExportBenchmarkTest` |
| **二** | NIO | [`nio/readme.md`](src/main/java/com/lind/foundation/nio/readme.md) | `com.lind.foundation.nio` | `DirectBufferDemoTest` |
| **三** | 锁 | [`concurrent/locks.md`](src/main/java/com/lind/foundation/concurrent/locks.md) | `com.lind.foundation.concurrent` | `LostUpdateDemoTest`、`BoundedBufferTest` |

### 推荐阅读顺序

```text
本 README
  │
  ├─① io/mental-model.md      ← 为何难懂、四大家族
  │    └─ io/readme.md        ← Writer / flush / Socket
  │    └─ io/buffered-file.md ← 物理文件 + CSV 导出对比
  │
  ├─② nio/readme.md           ← DirectBuffer / flip / 攒包思想
  │    └─（可选）lind-depth-research / lind-stream-web 工程攒包
  │
  └─③ concurrent/locks.md     ← 丢失更新 → 有界队列
       └─ concurrent/readme.md
```

---

## 一、IO 流

| demo | 做什么 |
|---|---|
| `StreamFamiliesDemo` | Input/OutputStream × Reader/Writer 最小对照 |
| `WriterDemos` | StringWriter、BufferedWriter 攒包、Socket+flush |
| `BufferedFileCopyDemos` | 读写/拷贝物理文件 |
| `CsvExportBenchmark` | 大 List 导出 CSV：有无缓冲性能报告 |

```bash
mvn -pl lind-java-foundation -Dtest=StreamFamiliesDemoTest,WriterDemoTest,BufferedFileCopyDemoTest,CsvExportBenchmarkTest test
```

---

## 二、NIO

| demo | 做什么 |
|---|---|
| `DirectBufferDemos` | Heap vs Direct、flip/clear、攒包、transferTo |

```bash
mvn -pl lind-java-foundation -Dtest=DirectBufferDemoTest test
```

工程向加深：[`lind-depth-research`](../lind-depth-research/README.md)（Direct 攒包写出）、[`lind-stream-web`](../lind-stream-web/README.md)（SSE 字符层攒包）。

---

## 三、锁

| demo | 做什么 |
|---|---|
| `LostUpdateDemos` | 无锁丢数 vs synchronized / ReentrantLock |
| `BoundedBuffer` | ReentrantLock + 双 Condition 有界队列 |

```bash
mvn -pl lind-java-foundation -Dtest=LostUpdateDemoTest,BoundedBufferTest test

mvn -pl lind-java-foundation -q org.codehaus.mojo:exec-maven-plugin:3.5.0:java \
  "-Dexec.mainClass=com.lind.foundation.concurrent.LostUpdateDemos" "-Dexec.classpathScope=compile"
```

待续：ReadWriteLock、CAS/Atomic、线程池拒绝策略（见 `concurrent/readme.md`）。

---

## 跟练方式（资深业务向）

1. 先问三问再看 API（流：方向/单位/角色；NIO：堆内外谁拷贝；锁：保护什么状态）  
2. 跑测试 → 改一个参数看现象  
3. 对照「坏做法 / 好做法」（CSV 每行 flush、无锁 count++）  
4. 每周一块，不要三块齐头并进  

更细的周计划口吻说明见下文「阶段建议」。

### 阶段建议（约 4～6 周）

| 周 | 焦点 |
|---|---|
| 1～1.5 | 模块一 IO（mental-model → 文件 → CSV → Socket） |
| 1.5～2 | 模块二 NIO + 对照 depth/stream-web |
| 2 | 模块三锁 3.1 / 3.2，再按需 3.3+ |

---

## 与其它模块的关系

| 模块 | 职责 |
|---|---|
| **lind-java-foundation**（本模块） | 底座：流 / NIO / 锁 |
| `lind-architecture` | 架构模式：Builder、Transport、Variant（elasticsearch-java 对照） |
| `lind-depth-research` | 工程场景：攒包写出、mmap、断点续传 |
| `lind-stream-web` | SSE 演示 + 字符层攒包 |

---

## 一键跑三大块测试

```bash
mvn -pl lind-java-foundation -Dtest=StreamFamiliesDemoTest,WriterDemoTest,BufferedFileCopyDemoTest,CsvExportBenchmarkTest,DirectBufferDemoTest,LostUpdateDemoTest,BoundedBufferTest test
```

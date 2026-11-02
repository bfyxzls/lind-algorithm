# 零拷贝 / DirectBuffer 详解（接上 `Writer`）

上一篇 [`../io/readme.md`](../io/readme.md) 讲的是：**字符怎么变成可替换的写出抽象**。  
这篇讲下一步：**字节在进内核 / 网卡之前，还要被搬几次？能不能少搬？**

工程落地对照：[`lind-depth-research` 的 `CoalescingStreamWriter`](../../../../../lind-depth-research/src/main/java/com/lind/depth/stream/CoalescingStreamWriter.java)（`allocateDirect` + 攒包）。

---

## 1. 先把名词说清楚（避免神话）

「零拷贝」不是 Java API 名字，而是一类 **目标**：

> 数据从产生点到消费点（磁盘 → Socket、GPU → 网卡…），**尽量少经 CPU 在用户态缓冲区之间搬来搬去**。

| 层次 | 典型手段 | Java 里你能碰到的 |
|---|---|---|
| 操作系统真·零拷贝 | `sendfile` / `splice` / `DMA` | `FileChannel.transferTo` / `transferFrom`（底层常走 sendfile） |
| 减少 JVM 堆内搬运 | 堆外内存，Channel 直接吃 DirectBuffer | `ByteBuffer.allocateDirect` |
| 减少系统调用 / 小包 | 攒到接近 MTU 再写 | 阈值 + 时间窗 flush（深度模块已做） |

**诚实结论：**

- Java **很少**能做到「从业务对象到网卡 0 次拷贝」。  
- 但你能做到：**少一次堆拷贝、少 N 次 write 系统调用、少一堆 TCP 小包**——这已经能把 CPU 从「搬运工」里解放出来。  
- 说「DirectBuffer = 零拷贝」不严谨；说「DirectBuffer 是 Java 侧靠近零拷贝思想的常用砖块」才准确。

---

## 2. 一次「普通写出」到底拷了几次？

以「框架里每个 token `writeAndFlush`」为例（文档场景一）：

```
String token
    → getBytes()           【堆内】新建 byte[]
    → Netty/堆 ByteBuf     【堆内】再拷一次（视实现）
    → 堆外 / native        【堆→堆外】JNI 或 Direct 拷贝
    → write() 进内核       【用户态→内核】又一次
    → 网卡 DMA             【内核→设备】
```

token 又短又密时：

- **拷贝次数 × token 数**  
- **系统调用次数 ≈ token 数**  
- CPU 上看起来像「业务很重」，其实在搬字节。

目标形态（思想，不要求一步到位）：

```
尽量少的缓冲切换 → 堆外 DirectBuffer 攒够 ~MTU → 一次 Channel.write
（文件静态资源则：FileChannel.transferTo(socket) 走 sendfile）
```

---

## 3. Heap Buffer vs Direct Buffer

```java
ByteBuffer heap   = ByteBuffer.allocate(4096);       // 底层是 byte[]，在 JVM 堆
ByteBuffer direct = ByteBuffer.allocateDirect(4096); // 底层在堆外（native）
```

| | Heap (`allocate`) | Direct (`allocateDirect`) |
|---|---|---|
| 内存位置 | JVM 堆，可被 GC | 堆外，生命周期由 Buffer / Cleaner 管 |
| `array()` | 通常可以 | **不行**（没有公开 heap 数组） |
| 给 `SocketChannel` / `FileChannel` | 常常还要先拷到临时 Direct | 更可能被内核/JNI **直接**使用 |
| 分配成本 | 低 | **高**（一次 syscall/预留），应复用 |
| 适合 | 小对象、编解码中间态、短命缓冲 | 网络/文件热路径上的读写缓冲 |

**经验法则（老鸟版）：**

1. 热路径上的 **读写缓冲池** 用 Direct，并且 **clear 复用**，不要每次 `allocateDirect`。  
2. 业务里随手 `new byte[n]`、短生命周期临时缓冲，继续用堆即可。  
3. Direct 泄漏难查：池化或限定生命周期（连接关闭就释放）。

---

## 4. ByteBuffer 三个指针：不会 flip 等于不会用

```
capacity = 总容量（不变）
limit    = 可读/可写上界
position = 下一个读或写的位置
```

常见状态机（和 `CoalescingStreamWriter` 完全一致）：

```
allocateDirect → 写模式：position 前进，limit=capacity
     put / put ...
flip()  → 读模式：limit=旧 position，position=0
     channel.write(buf) 读走数据
clear() → 回到写模式：position=0，limit=capacity（复用同一块！）
```

还有：

- `compact()`：把未读完的数据挪到开头，继续写（半包场景）  
- `rewind()` / `mark()` / `reset()`：重读、书签  

**demo 里会断言 flip/clear 的 position/limit**，建议动手改坏一次再看异常。

---

## 5. DirectBuffer 和「真零拷贝」差在哪？

```
┌─────────────────────────────────────────────────────────┐
│  A. DirectBuffer + Channel.write                        │
│     用户态堆外缓冲 ──write──► 内核 socket 缓冲 ──► 网卡   │
│     仍有一次「用户态 → 内核」拷贝（除非驱动/协议栈另有优化） │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  B. FileChannel.transferTo(socket) ≈ sendfile           │
│     文件页缓存 ──内核内──► socket（CPU 几乎不碰 payload） │
│     这才是教科书里的「零拷贝发文件」                        │
└─────────────────────────────────────────────────────────┘
```

流式 AI token **不是磁盘文件**，一般走不了 sendfile；更现实的是 **A + 攒包**。  
静态资源、大文件下载，优先想 **B**。

本包 `ZeroCopyTransferDemo` 用「文件 → 文件」的 `transferTo` 演示 API 形态（同机复制也能走内核优化路径，视 OS 而定）。

---

## 6. 攒包：零拷贝思想里被低估的一半

即使全用 DirectBuffer，若 **每 5 字节 flush 一次**：

- 系统调用仍然爆炸  
- TCP 头开销、中断、上下文切换照样打满 CPU  

所以深度模块的公式是：

```text
DirectBuffer（少堆拷贝） + 字节阈值≈MTU（少 syscall） + 时间窗（控延迟）
```

本包 `CoalesceDirectDemo` 是缩微版：只对比「每段一刷」vs「满阈值再刷」的 `flushCount`。  
完整带时间窗 / Sink 抽象的版本在 `lind-depth-research`。

---

## 7. 和 `Writer` 怎么接上？

完整链路可以是：

```text
业务对象
  → JsonMapper / 手工拼装   （字符，Writer）
  → CharsetEncoder          （字符 → 字节）
  → Direct ByteBuffer       （堆外攒包）
  → SocketChannel.write     （进内核）
```

注意：

- `Writer` 解决的是 **字符抽象与编码边界**  
- `DirectBuffer` 解决的是 **字节热路径放哪、怎么少搬**  
- 两者叠在一起，才接近「工程级流式写出」

`String.getBytes(UTF_8)` 每次仍会在堆上出 `byte[]`——这是 Java 文本路径的常见残留拷贝；极致优化会走 `CharsetEncoder` 直接 encode 进 DirectBuffer（Netty 等框架已做）。教学上先接受「先堆内 byte[] 再 put 进 Direct」这一档，已经比「每 token 全链路 flush」强一个数量级。

---

## 8. 什么时候不要上 DirectBuffer？

| 场景 | 建议 |
|---|---|
| 缓冲 &lt; 一两 KB、偶发 IO | 堆 Buffer 更简单 |
| 分配极频繁且不复用 | Direct 分配反而更慢、更易碎 |
| 只在堆内算一下就扔 | 根本不需要 Direct |
| 需要 `array()` 给老 API | 只能用 Heap，或显式拷一次 |

**原则：先量 syscall / 拷贝 / 小包，再决定 Direct；不要为了「听起来高性能」全局 Direct。**

---

## 9. 速查清单

1. 零拷贝是目标，不是某个类名；Java 常见两招：`transferTo` + `allocateDirect`  
2. Direct = 堆外，利 Channel，弊分配贵 → **池化 / clear 复用**  
3. 会用 `flip` / `clear`，才算会用 Buffer  
4. Direct 不替代攒包；小包风暴照样打满 CPU  
5. 流式业务看 `lind-depth-research`；本包只建立心智模型  

可运行：

```bash
mvn -pl lind-architecture -Dtest=DirectBufferDemoTest test
mvn -pl lind-architecture -q org.codehaus.mojo:exec-maven-plugin:3.5.0:java \
  "-Dexec.mainClass=com.lind.architecture.nio.DirectBufferDemos" "-Dexec.classpathScope=compile"
```

# Java 流心智模型：为何难懂 & 怎么想

> 先读本文，再读 [`readme.md`](readme.md)（Writer 细讲）和 [`../nio/readme.md`](../nio/readme.md)（DirectBuffer）。  
> 可运行对照：`StreamFamiliesDemo` / `StreamFamiliesDemoTest`。

---

## 1. 为什么特别难懂（不是你的问题）

业务十年写的是「**结果**」：`String`、`byte[]`、`Files.writeString`、`ObjectMapper.writeValueAsString`。  
`Writer` / `OutputStream` 描述的是「**管道**」——数据还在路上、可能没落地、还有缓冲和编码。大脑从「值」切到「过程」，天然别扭。

难在下面三层叠在一起，教材却爱平铺类名：

| 维度 | 你要先问 | 对应类型感 |
|---|---|---|
| 方向 | 往外推还是往里拉？ | `Output*` / `Writer` vs `Input*` / `Reader` |
| 单位 | 管子里是字节还是字符？ | `*Stream` vs `Reader` / `Writer` |
| 角色 | 这一节是源头/汇，还是装饰？ | `File*` / `Socket*` / `ByteArray*` vs `Buffered*` |

具体放大认知负担的五点：

1. **单位双轨**：机器只有字节；人写文本想字符 → Java 做两套 API，中间用 Charset 桥连接。  
2. **方向对称、类名爆炸**：入/出各一套；再叠缓冲、文件、数组…继承树劝退。  
3. **装饰器可无限套**：`BufferedWriter(OutputStreamWriter(socket.getOutputStream()))` 是**管道组装**，不是一个类干完。  
4. **缓冲破坏直觉**：`write` ≠ 对端立刻看见；`close` ≠ 一定调下游 `flush`（见 `BufferedWriter`）。  
5. **框架把管道藏起来了**：直到流式 / Socket / 可替换出口，才被迫直面。

---

## 2. 只记一张「水管图」

把 IO 想成**单向水管**，不要先背继承树。

```
业务数据
   │  write / read = 往管子里推 / 从管子里拉
   ▼
┌──────────────┐
│ 装饰层（可选） │  Buffered*：攒一截再交给下游
└──────┬───────┘
       ▼
┌──────────────┐
│ 单位转换（桥） │  *StreamWriter / *StreamReader + Charset
└──────┬───────┘
       ▼
┌──────────────┐
│ 真正的口     │  文件 / Socket / 内存 byte[] / String 缓冲
└──────────────┘
```

**铁律（够用）：**

- 二进制（图片、protobuf、密文）→ `InputStream` / `OutputStream`  
- 文本（JSON、HTTP 行、日志）→ `Reader` / `Writer`；落网/落盘前经 `OutputStreamWriter(UTF_8)` 等变成字节  
- `write` / `read` 只表示「推进管道」；**对端要看见 → 看缓冲 + 是否 `flush`**  
- 组装顺序从外到内读：**业务 → 缓冲 → 编码 → Socket/文件字节口**

```
业务字符串
    → BufferedWriter
    → OutputStreamWriter(UTF_8)
    → Socket.getOutputStream()
    → TCP 字节
```

---

## 3. 四大家族（一张表）

| | **字节** | **字符** |
|---|---|---|
| **出（推）** | `OutputStream` | `Writer` |
| **入（拉）** | `InputStream` | `Reader` |

| 家族 | 最小操作 | 典型节点 | 典型装饰 |
|---|---|---|---|
| `OutputStream` | `write(byte[])` | `FileOutputStream`、`ByteArrayOutputStream`、`Socket#getOutputStream` | `BufferedOutputStream` |
| `InputStream` | `read()` / `read(byte[])`，EOF=`-1` | `FileInputStream`、`ByteArrayInputStream` | `BufferedInputStream` |
| `Writer` | `write(String/char[])` | `StringWriter`、`FileWriter`（少用） | `BufferedWriter` |
| `Reader` | `read(char[])`，EOF=`-1` | `StringReader`、`FileReader`（少用） | `BufferedReader` |

同一段 `"Hello"` 四条最小路径见 `StreamFamiliesDemo`。

---

## 4. 唯一必记的桥：Charset

字符世界 ↔ 字节世界，**必须**经过编码：

| 方向 | 类 | 含义 |
|---|---|---|
| 字符 → 字节 | `OutputStreamWriter` | `Writer` 包在 `OutputStream` 外 |
| 字节 → 字符 | `InputStreamReader` | `Reader` 包在 `InputStream` 外 |

```java
new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8));
new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8));
```

**一律显式 `UTF_8`。** 裸 `FileWriter` / 无 Charset 的构造会吃平台默认编码。

---

## 5. 节点 vs 装饰器

| 角色 | 干什么 | 例子 |
|---|---|---|
| **节点** | 真正的源头或汇 | `ByteArrayOutputStream`、`FileInputStream`、`Socket` 流 |
| **装饰器** | 包一层，改行为（缓冲、计数、加密） | `BufferedWriter`、`ProbeWriter`、`CountingWriter` |

读代码时从**最外层**看业务怎么 `write`，一层层剥到**最内层**才是磁盘/网卡。

---

## 6. `flush` / `close` /「对端看不见」决策树

```
我刚 write 完，对端读得到吗？
        │
        ├─ 中间有 Buffered* / 编码器自带缓冲？
        │         │
        │         ├─ 是 → 默认「还在管子里」
        │         │         │
        │         │         ├─ 协议边界要交付？ → 显式 flush()
        │         │         └─ 整段写完且可关流？ → close()（通常会带出残余，
        │         │                                  但 BufferedWriter.close()
        │         │                                  不保证调下游 flush()）
        │         │
        │         └─ 否 → 仍可能停在 OS/TCP 缓冲；交互协议仍常要 flush
        │
        └─ 调试：断点打在「你关心的那一层」
                  攒包 → write；刷出 → 显式 flush
```

| 调用 | 典型效果 |
|---|---|
| `write` | 推进本层管道，未必到对端 |
| `flush` | 本层缓冲推出，并尽量要求下游也 flush |
| `close` | 结束生命周期；常先刷残余，**不保证** ≡ 下游 `flush()` |

细节与 Socket 示例：[`readme.md` §5](readme.md)。

---

## 7. 选型口诀

1. 先问三问：**方向？单位？这一层角色？**  
2. 先画管道再写代码，禁止一上来 `new FileWriter`。  
3. 文本协议默认：`BufferedWriter → OutputStreamWriter(UTF_8) → 出口`，边界 `flush`。  
4. API 要可测、可换落点：收 `Writer` / `OutputStream`，不要只返回 `String`（见 `JsonMapper`）。  
5. 性能热路径再下探字节侧 DirectBuffer / 攒包——先分清字符/字节边界。

---

## 8. 本仓库对照

| 材料 | 解决什么 |
|---|---|
| 本文 + `StreamFamiliesDemo` | 四大家族 + 桥 + 心智模型 |
| [`readme.md`](readme.md) + `WriterDemos` | Writer / BufferedWriter / Socket |
| [`buffered-file.md`](buffered-file.md) + `BufferedFileCopyDemos` | 物理文件：BufferedReader ↔ BufferedWriter |
| [`../nio/readme.md`](../nio/readme.md) | DirectBuffer / 零拷贝思想 |
| [`lind-depth-research` stream](../../../../../lind-depth-research/src/main/java/com/lind/depth/stream/readme.md) | 工程级字节攒包 + 时间窗 |

```bash
mvn -pl lind-java-foundation -Dtest=StreamFamiliesDemoTest,WriterDemoTest test
```

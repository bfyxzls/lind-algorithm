# java.io.Writer 详解（给用了十年但少碰字符流的人）

> **先读心智模型：** [`mental-model.md`](mental-model.md)（为何难懂、四大家族、Charset 桥、flush 决策树）。  
> **读写物理文件：** [`buffered-file.md`](buffered-file.md)（`BufferedReader` / `BufferedWriter` + `BufferedFileCopyDemos`）。

你们在 `JsonMapper.write(Object, Writer)` 里已经碰到它了：

```java
void write(Object value, Writer writer) throws IOException;
```

为什么不直接返回 `String`？为什么不写 `OutputStream`？下面把这件事说透。

---

## 1. 一句话定位

| 概念 | 本质 |
|---|---|
| `OutputStream` | **字节**出口：`write(byte)` / `write(byte[])` |
| `Writer` | **字符**出口：`write(char)` / `write(String)` / `write(char[])` |

Java 的 `String` / `char` 是 Unicode（UTF-16）。落盘、进 Socket、进 HTTP body 时最终都是 **bytes**。  
`Writer` 的价值是：**在「字符世界」里写代码，把「字符 → 字节 + 编码」推迟到适配器（如 `OutputStreamWriter`）里完成。**

对业务/框架作者：API 接收 `Writer`，调用方可以塞：

- 内存里攒字符串 → `StringWriter`
- 文件 → `Files.newBufferedWriter(...)`
- HTTP 响应体 → Servlet/容器给的 `Writer`
- 自己的缓冲 / 加密 / 计量包装器 → 自定义 `Writer`

**生产者只认识「字符出口」，不认识具体落点。** 这和你们 Transport 不绑死 HTTP 是同一类抽象。

---

## 2. 和你日常经验的对照

写了十年业务代码，更常见的是：

```java
Files.writeString(path, json);           // 一次写完
objectMapper.writeValueAsString(obj);  // 直接拿 String
```

这些底层往往仍会走到 `Writer` / `OutputStream`，只是被工具封住了。

当你要做下面这些事时，`Writer` 就会从「底层细节」变成「正确抽象」：

1. **流式写出**：JSON 很大，不能先 `toString()` 再整包发送  
2. **同一套序列化逻辑**写到文件 / 内存 / Socket  
3. **可测试**：单测塞 `StringWriter`，断言内容，不碰磁盘  
4. **框架 SPI**：像 Jackson / elasticsearch-java 的 JsonpMapper，协议层只依赖字符流抽象

所以 `JsonMapper` 的签名是 `write(..., Writer)`，`writeToString` 只是用 `StringWriter` 包了一层便利方法——**String 是特例，Writer 才是通用出口。**

---

## 3. 类型体系（记这张图就够）

```
                    Appendable          Flushable          Closeable
                        △                   △                  △
                        └─────────┬─────────┴──────────────────┘
                                  │
                               Writer  (abstract)
                                  │
          ┌───────────┬───────────┼───────────┬──────────────┐
          │           │           │           │              │
   StringWriter  CharArrayWriter  BufferedWriter  OutputStreamWriter  PrintWriter
                                                      │
                                                 FileWriter
                                              (历史 API，编码不清晰，少用)
```

| 类 | 干什么 | 老鸟怎么用 |
|---|---|---|
| `StringWriter` | 写到内存 `StringBuilder` | 单测、小结果、`writeToString` |
| `CharArrayWriter` | 写到可扩容 `char[]` | 偶发；多数情况 `StringWriter` 更省事 |
| `BufferedWriter` | 装饰器：攒字符再刷到底层 | 写文件几乎总要包一层 |
| `OutputStreamWriter` | **字符 → 字节**，必须指定 `Charset` | 真正对接文件/Socket 的正规入口 |
| `FileWriter` | 写文件的快捷方式 | **尽量别用**（默认平台编码，坑） |
| `PrintWriter` | 像 `print`/`println`，异常可被吞掉 | 日志/文本协议；注意 `checkError()` |

现代写法优先：

```java
try (Writer w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
    w.write("...");
}
```

等价于：`BufferedWriter` → `OutputStreamWriter(UTF-8)` → `FileOutputStream`。

---

## 4. Writer 合同：你真正需要记住的方法

抽象基类核心（简化）：

```java
public abstract class Writer implements Appendable, Closeable, Flushable {
    abstract void write(char[] cbuf, int off, int len) throws IOException;
    abstract void flush() throws IOException;
    abstract void close() throws IOException;

    // 下面多半有默认实现，最终会调到上面的 write(char[],...)
    void write(int c);
    void write(char[] cbuf);
    void write(String str);
    void write(String str, int off, int len);
    Writer append(CharSequence csq);   // 来自 Appendable
}
```

### 4.1 `write`：往「字符管道」推数据

- 不保证立刻到磁盘/网络；中间可能有缓冲  
- 可以多次调用，拼出完整文档  

### 4.2 `flush`：强迫缓冲落地

- 「我写完重要一段了，下游现在就要看到」  
- 网络协议、交互式输出常要主动 `flush`  
- **只 flush 不 close** 时，流仍可继续写  

### 4.3 `close`：结束生命周期

- 通常会先 `flush`，再释放底层资源  
- close 之后再 write → 多数实现抛 `IOException`  
- **务必** `try-with-resources`，否则文件句柄/连接泄漏  

### 4.4 `Appendable` 意味着什么

`Writer` 实现了 `Appendable`，所以能接：

```java
Formatter formatter = new Formatter(writer);
formatter.format("id=%d%n", 42);
```

`StringBuilder` 也是 `Appendable`，但**不是** `Writer`（没有 IO 异常模型 / flush / close）。别混。

---

## 5. 缓冲：为什么「光会 write」不够

```
你的代码 write("a") write("b") write("c")
        │
        ▼
  BufferedWriter   ← 字符攒在堆里
        │  buffer 满或 flush/close
        ▼
  OutputStreamWriter  ← 按 Charset 转成 bytes
        │
        ▼
  FileOutputStream / Socket  ← 系统调用
```

若不缓冲，每个小 `write` 都可能触发编码甚至系统调用 → 极慢。  
这和「SSE 每个 token flush 一次」把 CPU 打满是同一类问题（见 `lind-depth-research` 攒包场景）。

经验法则：

- **小对象、内存**：`StringWriter` 即可  
- **文件/网络**：`BufferedWriter` 或 `Files.newBufferedWriter`  
- **要低延迟推送**：有意减小缓冲、或按策略 `flush`（产品决策，不是默认）

### BufferedWriter 和直接写：本质差在「触达下游的次数」

- `BufferedWriter`：`write` 先进入内部 `char[]`（默认 **8192 字符**）。**缓冲区满**、或你调用 **`flush()` / `close()`** 时，才把攒好的字符交给下游 `Writer`。
- 直接 `OutputStreamWriter`：每次 `write` 更可能立刻编码并调用底层 `OutputStream.write`（短字符串 ≈ 一次下游写；逐字符写更糟）。

结论：写少量数据时差别可忽略；**循环里大量小片段**时，BufferedWriter 显著减少下游写次数（文件上≈少系统调用，Socket 上≈少 TCP 小包）。

demo：`WriterDemos.demoBufferedWriterCoalesce()` 用 **`ProbeWriter`** 挂在 `BufferedWriter` 与下游之间计数。  
（不要只探 `OutputStream`：`OutputStreamWriter` 自带编码缓冲，两边都会变成「close 时 1 次」，对比失真。）

#### 调试陷阱：`BufferedWriter.close()` ≠ 下游 `flush()`

| 调用 | 对下游 `ProbeWriter` 的效果 |
|---|---|
| `bufferedWriter.write(...)` 且缓冲未满 | 不触达下游 |
| 缓冲满 / `flushBuffer` | 只调下游 **`write(char[])`** |
| `bufferedWriter.flush()` | `write` 残余（若有）+ 下游 **`flush()`** |
| `bufferedWriter.close()` | `write` 残余（若有）+ 下游 **`close()`**，**不调** `flush()` |

所以断点打在 `ProbeWriter.flush()`、只跑 demo6 且不显式 `w.flush()` 时，会以为「flush 没执行」——其实这条路径本来就不会进 `flush()`；请把断点改到 `write`，或在关闭前加 `w.flush()`。

### Socket 上的标准栈与「必须 flush」

```
业务多次 write("GET ") / write("/toy") / ...
        │
        ▼
  BufferedWriter          ← 字符攒包（默认 8K chars）
        │ 满或 flush/close
        ▼
  OutputStreamWriter(UTF-8) ← 字符 → 字节
        │
        ▼
  Socket.getOutputStream()  ← TCP
```

| 点 | 说明 |
|---|---|
| 为何包 BufferedWriter | 避免每个小字符串都打成独立 TCP 段 |
| 为何还要 OutputStreamWriter | Socket 只有字节流；必须显式 UTF-8 |
| **协议边界必须 flush** | 请求行/一帧写完后，若不清空缓冲，对端 `readLine` 会一直等 |
| close 也会 flush | 但连接还要继续读响应时，应先 `flush` 再读，而不是靠 close |

demo：

- `demoSocketRequestWithBufferedWriter()`：本机 ServerSocket 回显，客户端 `flush` 后收到 `ACK ...`
- `demoSocketWithoutFlushBlocksPeer()`：故意不 flush，对端短超时内读不到行

和 `lind-depth-research` 的 DirectBuffer 攒包是同一思想：**字符侧 BufferedWriter，字节热路径可再上 DirectBuffer**；本 demo 只把字符侧攒包说透。

---


## 6. 编码：Writer 最容易踩的坑

`Writer` 写的是 **char**。变成 bytes 时必须有 Charset。

| 写法 | 风险 |
|---|---|
| `new FileWriter(file)` | 用**默认平台编码**，Windows 常是 GBK，Linux 常是 UTF-8 → 互拷乱码 |
| `new OutputStreamWriter(out)` | 同上，缺 charset |
| `new OutputStreamWriter(out, UTF_8)` | ✅ |
| `Files.newBufferedWriter(path, UTF_8)` | ✅ |

**铁律：跨机器、跨语言、HTTP/JSON，字符出口一律显式 UTF-8。**

---

## 7. 自定义 Writer：理解「抽象」的最快方式

子类最少要实现：`write(char[],int,int)`、`flush`、`close`。  
demo 里有一个 `CountingWriter`：不落盘，只统计写了多少字符——单测和限流场景很常见。

装饰器模式在 IO 里是标配：

```text
YourApp → CountingWriter → BufferedWriter → OutputStreamWriter → FileOutputStream
```

每层只做一件事：计数 / 缓冲 / 编码 / 字节输出。

---

## 8. 和本模块 `JsonMapper` 的关系

```text
JsonMapper.write(obj, Writer)
        │
        ├─ 单测 / 便利方法：StringWriter → String
        ├─ 落盘：Files.newBufferedWriter(...)
        └─ 将来接 HTTP：response.getWriter() 或 OutputStreamWriter(socket)
```

换 `Writer` 实现，**序列化代码一行都不用改**。  
这就是为什么 API 要收 `Writer` 而不是只返回 `String`。

对称侧：读用 `Reader`（`StringReader` / `InputStreamReader`），同一套「字符抽象」。

---

## 9. 和 NIO 的关系（知道边界即可）

| 年代 | API | 备注 |
|---|---|---|
| 经典 IO | `java.io.Writer` | 阻塞、面向流、API 简单 |
| NIO | `CharBuffer` + `CharsetEncoder` | 更底层；多数业务仍用 Writer 包一层 |
| NIO.2 | `Files.newBufferedWriter` | **推荐**的文件写法，返回的仍是 `BufferedWriter` |

即便上 Netty / 反应式，边界处仍常把「字符序列化」写成对 `Writer` 或 `OutputStream` 的适配。  
先吃透 `Writer`，再看零拷贝/DirectBuffer，不会断层。

---

## 10. 速查清单

1. 字符用 `Writer`，字节用 `OutputStream`；JSON/文本优先想字符流  
2. API 设计：通用出口用 `Writer`，需要时再 `StringWriter` 转 String  
3. 文件：`Files.newBufferedWriter(path, UTF_8)`，别用裸 `FileWriter`  
4. `flush` ≠ `close`；close 前通常会 flush  
5. 永远 try-with-resources  
6. 需要观测/限流时，写装饰器 `Writer`，不要改业务序列化代码  

可运行示例见同包：

- `WriterDemos.java` — 说明性 demo（含 Socket + BufferedWriter 攒包）  
- `ProbeOutputStream.java` — 统计触达下游的 write 次数  
- `WriterDemoTest.java` — 断言版：

```bash
mvn -pl lind-java-foundation -Dtest=WriterDemoTest test
```

# BufferedReader / BufferedWriter：读写物理文件

接 [`mental-model.md`](mental-model.md) 与 [`readme.md`](readme.md)。  
场景：**从本机读文本 → 写出到物理文件**（日志导出、配置拷贝、行协议落盘）。

可运行：`BufferedFileCopyDemos` / `BufferedFileCopyDemoTest`。

```bash
mvn -pl lind-java-foundation -Dtest=BufferedFileCopyDemoTest test
```

---

## 1. 为什么文件场景几乎总要 Buffered*

磁盘 / SSD 的一次系统调用成本远高于往内存 `char[]` 里拷几个字符。

| 类 | 方向 | 默认缓冲 | 作用 |
|---|---|---|---|
| `BufferedWriter` | 出 | 8192 **字符** | 多次 `write`/`newLine` 先攒内存，满或 `flush`/`close` 再交给下游 |
| `BufferedReader` | 入 | 8192 **字符** | 一次从下游多读一截，你的 `readLine` 多半打在内存上 |

不缓冲时：循环里「一行一写 / 一字一读」≈ 大量进内核，文件拷贝会明显变慢。

---

## 2. 读写物理文件的标准栈（记住这一条）

```text
读：
  磁盘字节
    → FileInputStream / Files.newInputStream
    → InputStreamReader(UTF_8)     // 字节 → 字符
    → BufferedReader               // 字符预读

写：
  业务字符
    → BufferedWriter               // 字符攒包
    → OutputStreamWriter(UTF_8)    // 字符 → 字节
    → FileOutputStream / Files.newOutputStream
    → 磁盘
```

现代写法（推荐，等价于上面）：

```java
try (BufferedReader in = Files.newBufferedReader(src, UTF_8);
     BufferedWriter out = Files.newBufferedWriter(dst, UTF_8)) {
  String line;
  while ((line = in.readLine()) != null) {
    out.write(line);
    out.newLine();
  }
} // close → 写出残余缓冲
```

**铁律：显式 `UTF_8`。** 不要用裸 `FileReader` / `FileWriter`（平台默认编码）。

---

## 3. 和「直接 Files.readString / writeString」怎么选

| 方式 | 适合 |
|---|---|
| `Files.readString` / `writeString` | 小文件、一次性读写 |
| `BufferedReader` + `BufferedWriter` | **大文件、按行处理、流式拷贝**、边读边转换 |

业务十年常用前者；做导出、日志切片、百万行配置时必须会后者。

---

## 4. 拷贝时谁负责 flush？

- 循环中一般**不必**每行 `flush`（否则等于关掉攒包）。  
- `try-with-resources` 结束时 `close()` 会带出 `BufferedWriter` 残余字符。  
- 若拷贝后还要**立刻**用另一个进程读同一文件，可在 close 前显式 `out.flush()`（更稳妥）。

`BufferedWriter.close()` 会把残余 `write` 给下游，但不保证调下游 `flush()`——对 `OutputStreamWriter`→文件通常仍会在 close 链路里落到盘；协议/Socket 场景仍以显式 `flush` 为准（见 `readme.md`）。

---

## 5. demo 对照表

| 方法 | 知识点 |
|---|---|
| `writeLinesToFile` | `Files.newBufferedWriter` 落盘 |
| `readLinesFromFile` | `Files.newBufferedReader` + `readLine` |
| `copyFileLineByLine` | 读→写完整管道 |
| `compareWriteCallsToFile` | 有/无 `BufferedWriter` 时触达下游的 `write` 次数（`ProbeWriter`） |

---

## 6. 和 Socket / SSE 攒包的关系

同一思想，不同层：

| 层 | 组件 |
|---|---|
| 文件字符侧 | `BufferedWriter` / `BufferedReader` |
| Socket 字符侧 | `BufferedWriter` → `OutputStreamWriter` → Socket |
| SSE 业务侧 | `CoalescingStringFlux`（`lind-stream-web`） |
| 字节热路径 | `DirectBuffer`（`nio/readme.md`） |

---

## 7. 互联网业务里，平时哪儿会用到 BufferedWriter？

一句话：**凡是「循环往外吐很多小段文本」的出口，外面最好套一层缓冲**——文件、Socket、部分 Servlet 响应都是。

业务里你不一定亲手 `new BufferedWriter`：`Files.newBufferedWriter`、Tomcat 的 `response.getWriter()`、日志框架内部，往往**已经带了缓冲**。但作为后端，你要知道「谁在攒包、边界要不要 flush」。

### 7.1 高频真实场景

| 场景 | 典型写法 | 为啥需要缓冲 |
|---|---|---|
| **运营导出 CSV/明细** | 查库游标/分页 → 逐行 `write` 到文件或下载流 | 一行一刷磁盘/响应会极慢 |
| **对账 / 批处理落盘** | 扫订单写 `.txt` / `.csv` 给财务、渠道 | 十万行级，靠攒包减系统调用 |
| **生成临时报表再上传 OSS** | 先写本地临时文件，再 `putObject` | 边算边写，不能整表 `String` |
| **简易文本协议客户端** | 调内部 TCP/短信网关：写行 + `flush` | 小包合并；协议边界必须 flush |
| **手写 HTTP/健康检查探针** | `BufferedWriter` 写请求行到 `Socket` | 同你们 `WriterDemos` Socket 示例 |
| **模板/代码生成、配置渲染** | 循环拼多段文本写 `.sql` / `.yml` | 多次小 `write` 合并 |

### 7.2 例 1：导出 CSV（最常见）

```java
try (BufferedWriter out = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
    out.write("orderId,amount,status");
    out.newLine();
    for (Order o : orderRepository.scan(day)) { // 流式/游标，别 List 全装内存
        out.write(o.id() + "," + o.amount() + "," + o.status());
        out.newLine();
        // 不要每行 flush
    }
} // close 带出尾包
```

### 7.3 例 2：Socket 发一行命令（协议边界要 flush）

```java
try (Socket socket = new Socket(host, port);
     BufferedWriter out = new BufferedWriter(
         new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
     BufferedReader in = new BufferedReader(
         new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
    out.write("PING");
    out.newLine();
    out.flush();          // 不 flush，对端 readLine 可能一直等
    String pong = in.readLine();
}
```

### 7.4 例 3：你「没写 BufferedWriter」但其实在用

| 你写的 API | 底层常见情况 |
|---|---|
| `Files.newBufferedWriter(...)` | 就是 `BufferedWriter` |
| `PrintWriter` 包文件/响应 | 内部常带 autoFlush/缓冲，别误以为无缓冲 |
| Logback/Log4j 写日志文件 | Appender 内部缓冲 + 策略刷盘 |
| `HttpServletResponse.getWriter()` | 容器 Writer，通常有缓冲；`flushBuffer()` 才推给客户端 |

### 7.5 什么时候可以不管 / 不该乱 flush

- **小配置一次写完**：`Files.writeString` 足够，不必为 Buffered 而 Buffered。  
- **循环导出**：用 `BufferedWriter`，**不要**每行 `flush`。  
- **请求-响应式文本协议**：写完一帧/一行后 **必须** `flush`。  
- **二进制**（图片、protobuf）：走 `OutputStream` / `BufferedOutputStream`，不是 `BufferedWriter`。

### 7.6 和本仓库对照

| 材料 | 对应日常点 |
|---|---|
| `BufferedFileCopyDemos` | 导出/拷贝类落盘 |
| `CsvExportBenchmark` | 大 List 导出 CSV 性能对比 + Markdown 报告 |
| `WriterDemos` Socket + flush | 文本协议客户端 |
| `lind-stream-web` `CoalescingStringFlux` | 「业务层攒包」同一思想，对象是 SSE 事件不是文件 |

---

## 8. 大 List 导出 CSV 性能对比

类：`CsvExportBenchmark`。模拟订单 `List`，分别用三种方式写物理 CSV，并生成报告。

| 方式 | 实现 |
|---|---|
| A 无 BufferedWriter | `OutputStreamWriter` → 文件 |
| B 每行 flush | A + 每行 `flush()`（反例） |
| C BufferedWriter | `BufferedWriter` → `OutputStreamWriter` → 文件 |

```bash
# 默认 10 万行；可传行数：500000
mvn -pl lind-java-foundation -q org.codehaus.mojo:exec-maven-plugin:3.5.0:java \
  "-Dexec.mainClass=com.lind.foundation.io.CsvExportBenchmark" \
  "-Dexec.classpathScope=compile" "-Dexec.args=100000"

mvn -pl lind-java-foundation -Dtest=CsvExportBenchmarkTest test
```

报告含：造数耗时、三种写出耗时、文件字节、ProbeWriter 下游 write 次数、相对加速比。工作目录下会生成 `csv-export-benchmark-report.md`。

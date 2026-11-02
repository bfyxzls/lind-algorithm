# lind-architecture

对照 [`docs/elasticsearch-java的学习.md`](../docs/elasticsearch-java的学习.md) **§3 最值得学的几个「架构级」知识点**，用一个迷你 **ToyDoc SDK** 把模式落地（不依赖 Elasticsearch）。

| # | 知识点 | 包 / 类 | 状态 |
|---|---|---|---|
| 1 | Fluent / Functional Builder → 类型安全内嵌 DSL | `util` + `api.search.SearchRequest` | ✅ |
| 2 | Transport 作为稳定内核 | `transport.Transport` / `InMemoryTransport` | ✅ |
| 3 | JsonMapper 可替换 | `json.JsonMapper` / `JacksonJsonMapper` / `SimpleJsonMapper` | ✅ |
| 4 | Variant 表达多态 JSON | `api.search.query.Query` | ✅ |
| 5 | 规格驱动大 API 面（玩具 codegen） | `codegen.SpecDrivenEndpoints` | ✅ |
| 6 | 同步外观 + 异步内核 | `ToyClient` / `ToyAsyncClient` | ✅ |
| 7 | 命名空间拆分上帝 Client | `documents()` / `search()` | ✅ |

```bash
mvn -pl lind-architecture -am test
```

## 调用链（对照 elasticsearch-java）

```
ToyClient.search(s -> s.index("docs").query(q -> q.term(...)))
        │
        ▼
SearchNamespace → Endpoint + Request
        │
        ▼
Transport.performRequest(endpoint, request, jsonMapper)
        │
        ▼
InMemoryTransport（教学）/ 真实 HTTP（可自行扩展）
```

换 `JsonMapper` 或 `Transport` 实现时，业务侧 lambda DSL **不用改**。

## 设计原则

- **可对照**：每个知识点有独立包与测试断言  
- **薄而全**：不追求功能完备，追求「模式可迁到自研中间件 Client」  
- **纯教学依赖**：仅 Jackson（默认 Mapper）+ JUnit

## 补充：IO / NIO 心智模型

| 主题 | 文档 | 测试 |
|---|---|---|
| 流概念总览（为何难懂 / 四大家族） | [`io/mental-model.md`](src/main/java/com/lind/architecture/io/mental-model.md) | `StreamFamiliesDemoTest` |
| `java.io.Writer`（字符出口） | [`io/readme.md`](src/main/java/com/lind/architecture/io/readme.md) | `WriterDemoTest` |
| 物理文件 BufferedReader/Writer | [`io/buffered-file.md`](src/main/java/com/lind/architecture/io/buffered-file.md) | `BufferedFileCopyDemoTest` |
| 零拷贝 / `DirectBuffer` | [`nio/readme.md`](src/main/java/com/lind/architecture/nio/readme.md) | `DirectBufferDemoTest` |

流式攒包完整工程版见 [`lind-depth-research`](../lind-depth-research/README.md) 的 `CoalescingStreamWriter`。

```bash
mvn -pl lind-architecture -Dtest=StreamFamiliesDemoTest,WriterDemoTest,BufferedFileCopyDemoTest,DirectBufferDemoTest test
```

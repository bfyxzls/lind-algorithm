# lind-architecture

对照 [`docs/elasticsearch-java的学习.md`](../docs/elasticsearch-java的学习.md) **§3 最值得学的几个「架构级」知识点**，用迷你 **ToyDoc SDK** 落地（不依赖 Elasticsearch）。

**底座（IO / NIO / 锁）已拆到独立模块：** 请从 [`lind-java-foundation/README.md`](../lind-java-foundation/README.md) 统一进入学习。

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

# 知识点 (2)：Transport 稳定内核

- `Endpoint`：URL / Method / body / responseType
- `Transport.performRequest` / `performRequestAsync`：上层 API 只走这里
- `InMemoryTransport`：教学替身，证明换传输实现不改 DSL

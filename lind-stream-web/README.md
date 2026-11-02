# lind-stream-web

基于 **Server-Sent Events**（`Content-Type: text/event-stream`）的流式 Web 服务，模拟大模型接口「边生成边返回」。

## 启动

在仓库根目录：

```bash
mvn -pl lind-stream-web -am spring-boot:run
```

浏览器打开：http://localhost:8088/

## 接口

### 1. OpenAI 风格（推荐）

`POST /v1/chat/completions`

```bash
curl -N http://localhost:8088/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d "{\"model\":\"lind-demo\",\"stream\":true,\"messages\":[{\"role\":\"user\",\"content\":\"介绍一下 SSE\"}]}"
```

流式事件示例：

```text
data: {"id":"chatcmpl-...","object":"chat.completion.chunk","choices":[{"delta":{"role":"assistant"}}]}

data: {"id":"chatcmpl-...","object":"chat.completion.chunk","choices":[{"delta":{"content":"你好"}}]}

data: [DONE]
```

`stream: false` 时返回普通 JSON 完整响应。

### 2. 简易 SSE

```bash
curl -N "http://localhost:8088/api/chat/stream?q=你好"
```

事件名：`token` / `done`。

## 前端要点

不要用会缓冲整包的普通 `JSON` 解析；应使用：

- `fetch` + `ReadableStream`（本模块首页演示）
- 或浏览器 `EventSource`（仅适合 GET）

反向代理（Nginx）需关闭缓冲，例如：

```nginx
proxy_buffering off;
proxy_http_version 1.1;
chunked_transfer_encoding on;
```

响应头已包含 `Cache-Control: no-cache` 与 `X-Accel-Buffering: no`。

## SSE 攒包（优化）

默认在字符层做 **阈值 + 时间窗** 攒包，再写成 SSE 事件，减少「每 token 一包」：

| 配置 | 含义 |
|---|---|
| `lind.stream.coalesce.enabled` | 总开关 |
| `lind.stream.coalesce.flush-chars` | 累计字符数达标即发出 |
| `lind.stream.coalesce.max-delay` | 未满阈值也按时发出（时间窗兜底） |

**改配置改 `application.yml`，不要只改 Java 字段默认值**（YAML 会覆盖）。

模拟生成约 **40ms / token、约 2 字**。若 `max-delay` 只有 50ms，缓冲里往往 1～2 个 token 就被刷出，这时把 `flush-chars` 从 48 改成 480 **几乎无差别**（DevTools 里仍是很碎的 `token` 事件）。想看大包：同时加大 `max-delay`（如 `3s`），或观察启动日志里的 `SSE coalesce effective config`。

实现：`CoalescingStringFlux`（思想对齐 `lind-depth-research` 的 FlushPolicy；此处在 WebFlux/SSE 字符层，而非 DirectBuffer）。

对照（关闭攒包）：

```bash
curl -N "http://localhost:8088/api/chat/stream?q=你好&coalesce=false"
```

`/v1/chat/completions` 流式同样走服务默认攒包配置。

攒包和未攒包截图对比

![](https://images.cnblogs.com/cnblogs_com/lori/2457803/o_261029064028_sse.png)

## 说明

当前为**演示用模拟生成**（按片断延迟推送），便于联调前端与网关；接入真实大模型时，把 `ChatCompletionService` 换成上游 SSE/流式客户端即可。

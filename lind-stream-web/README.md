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

## 说明

当前为**演示用模拟生成**（按片断延迟推送），便于联调前端与网关；接入真实大模型时，把 `ChatCompletionService` 换成上游 SSE/流式客户端即可。

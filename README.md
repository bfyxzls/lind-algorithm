# lind-algorithm

通用算法与数据结构库（多模块）。核心算法纯 JDK（17+）；另含 SSE 流式 Web 演示子项目。

原隶属 [lind-framework](https://github.com/bfyxzls/lind-framework)，现已独立维护。

## 工程结构

| 模块 | 说明 |
|---|---|
| `lind-algorithm` | 算法与数据结构核心库（无 Spring 运行时依赖） |
| `lind-stream-web` | 流式 Web 服务：`text/event-stream`，类大模型逐段返回 |

```bash
mvn clean test
mvn -pl lind-stream-web -am spring-boot:run
# 打开 http://localhost:8088/
```

## 核心库模块一览（`lind-algorithm`）

| 包 | 内容 |
|---|---|
| `tree` | Trie / 二叉树 / 红黑树 / B+ / R 树 / AST / CST / 决策树 / 流程树 / 多叉树 / LSM / 菜单树 |
| `fsm` | 有限状态机（Guard / Action / Entry-Exit） |
| `wheel` | 层级哈希时间轮（延时任务） |
| `loadbalance` | 随机 / 轮询 / LRU / LFU / 一致性哈希 / 平滑加权轮询 |
| `ratelimit` | 令牌桶 / 漏桶 / 滑动窗口 |
| `circuit` | 熔断器（Closed / Open / Half-Open） |
| `retry` | 指数退避重试（可选抖动） |
| `bloom` | Bloom 过滤器 |
| `skiplist` | 跳表有序 KV |
| `cache` | LRU / LFU 本地缓存 |
| `id` | 雪花算法 ID |
| `bitmap` | 位图与集合运算 |
| `topk` | Top-K 计数 / Count-Min Sketch |
| `schedule` | 简化 Cron 下次触发时间 |
| `graph` | Dijkstra / 拓扑排序 / 环检测 |
| `unionfind` | 并查集 |
| `stringmatch` | KMP / AC 自动机 |

## 依赖

```xml
<dependency>
    <groupId>com.lind</groupId>
    <artifactId>lind-algorithm</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 流式接口速览（`lind-stream-web`）

```bash
curl -N http://localhost:8088/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d "{\"model\":\"lind-demo\",\"stream\":true,\"messages\":[{\"role\":\"user\",\"content\":\"你好\"}]}"
```

详见 [lind-stream-web/README.md](lind-stream-web/README.md)。

## License

Apache License 2.0

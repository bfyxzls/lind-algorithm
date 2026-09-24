# lind-qps-checker

控制台压测工具：在一个测试周期内评估 REST / HTTP 服务的并发处理能力，并输出测试报告。

## 报告指标

- 测试并发数 / 最大可用并发数（`--find-max`）
- 总请求数、成功数、失败数、**成功率**
- **QPS**
- 最小 / 平均 / **最大** / P95 / P99 响应时间

## 运行

```bash
# 打包
mvn -pl lind-qps-checker -am package -DskipTests

# GET + Query 参数
java -jar lind-qps-checker/target/lind-qps-checker-1.0.0.jar \
  --url http://localhost:8080/api/list \
  --param page=1 --param size=20 \
  --concurrency 50 --duration 30

# POST JSON + 多个 Header
java -jar lind-qps-checker/target/lind-qps-checker-1.0.0.jar \
  --url http://localhost:8080/api/login \
  --method POST \
  --header "Content-Type:application/json" \
  --header "Authorization:Bearer token" \
  --body "{\"username\":\"test\",\"password\":\"123456\"}" \
  --concurrency 20 --duration 10

# POST 表单 + Query
java -jar lind-qps-checker/target/lind-qps-checker-1.0.0.jar \
  --url http://localhost:8080/login \
  --method POST \
  --param from=app \
  --form username=admin --form password=123 \
  --concurrency 20 --duration 10

# 阶梯加压，探测最大可用并发
java -jar lind-qps-checker/target/lind-qps-checker-1.0.0.jar \
  --find-max \
  --url http://localhost:8080/api/ping \
  --start 10 --step 10 --max-concurrency 200 \
  --duration 5 \
  --min-success-rate 99 \
  --max-rt-ms 3000
```

## 主要参数

| 参数 | 说明 | 默认 |
|---|---|---|
| `--url` | 目标 URL | 必填 |
| `--method` | HTTP 方法（GET/POST/PUT/PATCH/DELETE…） | `GET` |
| `--body` | 原始请求体（JSON 等）；不可与 `--form` 同用 | 空 |
| `--header` | 请求头，**可重复**；`Name:Value` 或 `Name=Value` | 无 |
| `--param` / `--query` | Query 参数，**可重复**；`key=value` | 无 |
| `--form` | 表单参数（`x-www-form-urlencoded`），**可重复** | 无 |
| `--concurrency` | 固定并发 | `10` |
| `--duration` | 周期秒数；find-max 时为每阶梯秒数 | `10` / `5` |
| `--timeout-ms` | 单次请求超时 | `5000` |
| `--find-max` | 启用最大并发探测 | 关 |
| `--start` / `--step` / `--max-concurrency` | 阶梯范围 | `10` / `10` / `200` |
| `--min-success-rate` | SLA 成功率 % | `99` |
| `--max-rt-ms` | SLA 最大响应时间 ms | `3000` |

说明：

- 多个 Header / 参数可写多次：`--header A:1 --header B:2`
- 也可一次写多个 Query：`--param a=1&b=2`
- Header 也可：`--header "A:1;B:2"`
- 使用 `--form` 时自动设置 `Content-Type: application/x-www-form-urlencoded`
- 使用 `--body` 且未指定 Content-Type 时，默认 `application/json`

## 模块结构

| 类 | 职责 |
|---|---|
| `QpsCheckerMain` | 控制台入口 |
| `CliArgs` | 参数解析 |
| `LoadConfig` | 压测配置（含 Header / Query / Form / Body） |
| `HttpRequester` | JDK HttpClient 单次请求 |
| `LoadRunner` | 固定并发单周期压测 |
| `CapacityFinder` | 阶梯加压找最大可用并发 |
| `LoadReport` | 指标汇总与文本报告 |

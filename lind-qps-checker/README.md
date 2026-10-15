# lind-qps-checker

HTTP 压测工具：**Web 界面（FreeMarker）**。评估 REST / HTTP 服务的并发与 QPS，任务与用例持久化到 H2。

## 报告指标

- 测试并发数 / 最大可用并发数（阶梯加压）
- 总请求数、成功数、失败数、**成功率**
- **QPS**
- 最小 / 平均 / **最大** / P95 / P99 响应时间
- 失败样本：HTTP 状态码 + 响应体

## 启动

```bash
mvn -pl lind-qps-checker -am spring-boot:run
# 或
mvn -pl lind-qps-checker -am package -DskipTests
java -jar lind-qps-checker/target/lind-qps-checker-1.0.0.jar
```

浏览器打开 [http://localhost:8091](http://localhost:8091)：

| 页面 | 说明 |
|---|---|
| `/` | 新建压测；可「保存为用例」 |
| `/cases` | 左文件夹 / 右用例列表；支持「导入 curl」 |
| `/cases/new`、`/cases/{id}/edit` | 新建 / 编辑用例 |
| `/jobs/{id}` | 任务详情（自动轮询报告） |
| `/jobs` | 历史任务 |
| `/api/jobs/{id}` | JSON 状态 |
| `/h2-console` | H2 控制台 |

默认端口：`8091`。库文件：`src/main/resources/h2/qps.*`。

## 表单字段

| 字段 | 说明 | 默认 |
|---|---|---|
| URL | 目标地址 | 必填 |
| method | HTTP 方法 | `GET` |
| body | 原始请求体；不可与 form 同用 | 空 |
| headers | 每行 `Name:Value` / `Name=Value` | 无 |
| query | 每行 `key=value` | 无 |
| form | 每行 `key=value` | 无 |
| concurrency | 固定并发 | `10` |
| duration | 周期秒数；FIND_MAX 时为每阶梯秒数 | `10` |
| timeoutMs | 单次请求超时 | `5000` |
| mode | `FIXED` / `FIND_MAX` | `FIXED` |

## 模块结构

```
com.lind.qps
├── QpsCheckerApplication     # Spring Boot 入口
├── engine/                   # 压测引擎（与 Web 无关）
│   LoadConfig, LoadRunner, CapacityFinder, HttpRequester,
│   LoadReport, FailureSample, RequestResult
└── web/
    ├── WebExceptionHandler
    ├── catalog/              # 文件夹 / 用例
    │   CaseCatalogController, CaseCatalogService, CurlImporter, …
    └── job/                  # 压测任务与表单
        LoadJobController, LoadJobService, LoadForm, …
```

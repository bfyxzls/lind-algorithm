# lind-qps-checker

HTTP 压测工具：**Web 界面（FreeMarker）**。评估 REST / HTTP 服务的并发与 QPS，任务与用例持久化到 **MySQL**，表结构由 **Liquibase** 管理。

## 报告指标

- 测试并发数 / 最大可用并发数（阶梯加压）
- 总请求数、成功数、失败数、**成功率**
- **QPS**
- 最小 / 平均 / **最大** / P95 / P99 响应时间
- 失败样本：HTTP 状态码 + 响应体

## 数据库准备

1. 本地启动 MySQL，创建库（仅需一次）：

```sql
CREATE DATABASE lind_qps DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 按需修改 [`src/main/resources/application.yml`](src/main/resources/application.yml) 中的账号密码（默认 `root` / `123456`）。

3. 应用启动时 Liquibase 会自动执行 changelog，创建 `qps_job` / `qps_folder` / `qps_case` 等表。

> 时间字段使用 `datetime`（避免 MySQL 对多个 `TIMESTAMP NOT NULL` 的默认值限制）。  
> 若曾启动失败留下半成品表，可清空后重跑：
>
> ```sql
> USE lind_qps;
> DROP TABLE IF EXISTS qps_case, qps_folder, qps_job, DATABASECHANGELOG, DATABASECHANGELOGLOCK;
> ```

变更脚本目录：

```
src/main/resources/db/changelog/
├── db.changelog-master.xml           # 总入口
└── changes/
    └── 001-init-qps-tables.xml       # 初始表结构（XML，可读性更好）
```

后续改表：在 `changes/` 新增 `002-xxx.xml`，并在 master 中 `<include/>`，**不要改已发布过的 changeset id**。

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

默认端口：`8091`。

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

## 测试说明

单元 / Web 测试使用 **H2 内存库**（`MODE=MySQL`），并执行同一套 Liquibase changelog，无需本机 MySQL。

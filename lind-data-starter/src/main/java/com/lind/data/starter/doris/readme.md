# doris 包（com.lind.data.starter.doris）

基于 **Apache Doris** 的场景封装：MySQL 协议 JDBC 查询（FE Query Port）+ FE HTTP **Stream Load** 批量导入。Doris 无官方 Spring Boot Starter，本包按 `lind.data.doris.*` 自动装配。

## 使用场景

| 组件 | 能力 | 典型场景 |
|---|---|---|
| `DorisJdbcQuery` | `queryForList` / `query` / `update` / `batchUpdate` | OLAP 查询、维表点查、轻量 DML |
| `DorisStreamLoad` | CSV / JSON HTTP Stream Load | 明细事实表批量灌数、近实时入库 |
| `LindSpringDoris` | 门面：`jdbc()` / `streamLoad()` / `jdbcTemplate()` | 业务统一入口 |

## 启用条件

1. classpath 存在 `spring-boot-starter-jdbc`、`mysql-connector-j`（Doris 兼容 MySQL 协议）
2. **显式**开启：`lind.data.doris.enabled=true`（默认关闭，避免误连）
3. 配置 JDBC 与 FE HTTP

```yaml
lind:
  data:
    doris:
      enabled: true
      jdbc-url: jdbc:mysql://127.0.0.1:9030/demo
      username: root
      password: ""
      driver-class-name: com.mysql.cj.jdbc.Driver
      fe-http-host: 127.0.0.1
      fe-http-port: 8030
      database: demo
      max-filter-ratio: 0.1
      timeout-seconds: 600
      label-prefix: lind
```

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
<dependency>
  <groupId>com.mysql</groupId>
  <artifactId>mysql-connector-j</artifactId>
</dependency>
```

自动配置会按需创建：

- `DataSource`（仅当容器中尚无任何 `DataSource` 时，按 `lind.data.doris.*` 建 Hikari 池）
- `JdbcTemplate`（缺失时）
- `DorisStreamLoad`（`HttpURLConnection`，支持 `Expect: 100-continue`）
- `LindSpringDoris`

若业务已提供 `DataSource` / `JdbcTemplate`（例如 `spring.datasource.*` 已指向 Doris FE），则复用现有 Bean，仅补 Stream Load 与门面。

## 调用方法

```java
@Autowired
LindSpringDoris doris;

// JDBC 查询
List<Map<String, Object>> rows = doris.jdbc().queryForList(
    "select dt, sum(amount) as amt from dws_order where dt = ?", "2026-10-14");

int n = doris.jdbc().update("insert into dim_user(id, name) values(?, ?)", 1, "lind");

// Stream Load：JSON 数组
DorisStreamLoadResult jsonResult = doris.streamLoad().loadJson("ods_event",
    "[{\"id\":1,\"name\":\"a\"},{\"id\":2,\"name\":\"b\"}]");
if (!jsonResult.success()) {
    throw new IllegalStateException(jsonResult.message());
}

// Stream Load：CSV + 自定义 label
DorisStreamLoadResult csvResult = doris.streamLoad().load(
    "ods_event",
    "1,a\n2,b".getBytes(StandardCharsets.UTF_8),
    DorisStreamLoad.FORMAT_CSV,
    "manual-label-001");
```

表需事先在 Doris 中创建（本封装不负责 DDL）。Stream Load 路径为：

`PUT http://{feHttpHost}:{feHttpPort}/api/{database}/{table}/_stream_load`

## 注意

- Query Port（默认 9030）走 JDBC；HTTP Port（默认 8030）走 Stream Load，两者都要通。
- Stream Load 成功状态含 `Success` 与 `Publish Timeout`（后者表示已提交、发布超时，一般可按成功处理）。
- 同一应用既有业务 MySQL 又有 Doris 时，请自行声明 Doris 专用 `DataSource`/`JdbcTemplate` Bean，或只用 Stream Load、JDBC 走已有数据源。
- 单测 mock `JdbcTemplate` / `HttpClient`，无需本机 Doris。

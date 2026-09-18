# hbase 包（com.lind.data.starter.hbase）

基于 **Apache HBase Client**（`org.apache.hadoop.hbase.client.Connection`）的轻量模板。HBase 无官方 Spring Boot Starter，本包提供连接自动配置与 `LindHBaseTemplate`。

## 使用场景

| 方法 | 能力 | 典型场景 |
|---|---|---|
| `put` / `putString` | 写入 Cell | 用户画像、宽表属性 |
| `get` / `getString` | 按 RowKey + CF + Qualifier 读取 | 精确点查 |
| `scan` | 起止 RowKey 扫描 | 范围拉取、离线导出雏形 |
| `delete` | 按 RowKey 删除 | 清理过期行 |

## 启用条件

1. classpath 存在 `hbase-client`
2. **显式**开启：`lind.data.hbase.enabled=true`（默认关闭，避免误连）
3. 配置 ZooKeeper 地址

```yaml
lind:
  data:
    hbase:
      enabled: true
      zookeeper-quorum: 127.0.0.1
      zookeeper-client-port: "2181"
      zookeeper-znode-parent: /hbase
```

```xml
<dependency>
  <groupId>org.apache.hbase</groupId>
  <artifactId>hbase-client</artifactId>
</dependency>
```

自动配置会创建：

- `Connection`（`destroyMethod = close`）
- `LindHBaseTemplate`

若业务已自行提供 `Connection` Bean，则不会重复创建，仅在缺失时装配模板。

## 调用方法

```java
@Autowired
LindHBaseTemplate hbase;

// 写入字符串
hbase.putString("user_profile", "u1001", "cf", "name", "lind");

// 读取
Optional<String> name = hbase.getString("user_profile", "u1001", "cf", "name");

// 字节写入
hbase.put("user_profile", "u1001", "cf", "avatar", bytes);

// 扫描 [start, stop)
List<Result> rows = hbase.scan("user_profile", "u1000", "u2000");

// 删除整行
hbase.delete("user_profile", "u1001");
```

表与列族需事先在 HBase 中创建（本模板不负责 DDL）。

## 注意

- `Connection` 线程安全，应用内单例即可；`Table` 由模板按次开关。
- 大 Scan 注意限流与超时，生产可改用 `ResultScanner` 流式消费（可扩展 `connection()`）。
- 单测 mock `Connection` / `Table`，无需本机 HBase / ZooKeeper。

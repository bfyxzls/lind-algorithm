# mongodb 包（com.lind.data.mongodb）

基于官方 **[mongodb-driver-sync](https://www.mongodb.com/docs/drivers/java/sync/current/)** 的 MongoDB 场景封装（无 Spring 依赖）。

## 入口

```java
try (LindMongo mongo = LindMongo.connect("mongodb://127.0.0.1:27017", "demo")) {
    mongo.documents("users").insert(new Document("name", "lind"));
}
```

也可注入自定义 `MongoCommands`（便于单测）。

## 场景一览

| 组件 | 能力 | 场景 |
|---|---|---|
| `MongoDocuments` | insert/find/update/delete | 文档 CRUD |
| `MongoQuery` | filter + 分页排序 | 条件查询 |
| `MongoAggregation` | pipeline / groupCount | 统计分析 |
| `MongoGeo` | 2dsphere + $near | 附近的人 / 门店 |
| `MongoTtlStore` | TTL 索引 | 验证码 / 会话过期 |
| `MongoTextSearch` | text 索引 | 站内全文搜索 |
| `MongoSequence` | findAndModify + $inc | 分布式序列 / 全局 ID |

## 示例

```java
// 分页查询
mongo.query("orders").find(Filters.eq("status", "PAID"), 1, 20);

// 附近 5km
mongo.geo("shops").ensureGeoIndex();
mongo.geo("shops").near(116.4, 39.9, 5000, 20);

// 全局序列
long id = mongo.sequence("counters").next("order");
```

## 注意

- 单测 mock `MongoCommands`，不强制本机 MongoDB。
- Geo / Text 使用前需 `ensure*Index()`。
- TTL 依赖 `expireAt`（Date）字段与后台清理线程，非即时删除。

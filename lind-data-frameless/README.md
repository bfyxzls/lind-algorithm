# lind-data-frameless

无 Spring 的数据中间件场景封装（frameless = 不依赖 Spring 框架）。

| 包 | 客户端 | 场景 |
|---|---|---|
| `com.lind.data.redis` | Jedis | 缓存 / 锁 / 限流 / Bloom / GEO / 排行 / 队列 / 延迟队列 / PubSub / 位图 / HLL / 社交 / 分布式 ID |
| `com.lind.data.mongodb` | mongodb-driver-sync | CRUD / 查询 / 聚合 / GEO / TTL / 全文 / 序列 |
| `com.lind.data.elasticsearch` | elasticsearch-java | 文档 / 全文检索 / 聚合 / 地理距离 |

## 与 Spring Boot / lind-data-starter 怎么选

| 场景 | 选择 |
|---|---|
| Spring Boot 业务项目 | **[`lind-data-starter`](../lind-data-starter/README.md)**（基于 Spring Data Redis / MongoDB / Elasticsearch + HBase 自动配置） |
| 无 Spring / CLI / 教学演示原生命令拼装 | **本模块 `lind-data-frameless`** |

**一句话**：生产 Spring Boot → `lind-data-starter`；无框架或要看清 Jedis/Driver 命令 → `lind-data-frameless`。

同一应用对同一中间件不要同时混用两套客户端。

## 依赖

```xml
<dependency>
    <groupId>com.lind</groupId>
    <artifactId>lind-data-frameless</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 快速开始

```java
try (LindRedis redis = LindRedis.connect("127.0.0.1", 6379)) {
    redis.cache().set("k", "v", Duration.ofMinutes(5));
}

try (LindMongo mongo = LindMongo.connect("mongodb://127.0.0.1:27017", "demo")) {
    mongo.documents("users").insert(new Document("name", "lind"));
}

try (LindElasticsearch es = LindElasticsearch.connect("127.0.0.1", 9200)) {
    es.documents("products").save("1", Map.of("title", "phone"));
}
```

各包说明见：

- [redis/readme.md](src/main/java/com/lind/data/redis/readme.md)
- [mongodb/readme.md](src/main/java/com/lind/data/mongodb/readme.md)
- [elasticsearch/readme.md](src/main/java/com/lind/data/elasticsearch/readme.md)

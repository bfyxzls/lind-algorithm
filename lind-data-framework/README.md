# lind-data-framework

数据中间件场景封装库（无 Spring 运行时依赖），从算法模块拆出，职责更清晰。

| 包 | 客户端 | 场景 |
|---|---|---|
| `com.lind.data.redis` | Jedis | 缓存 / 锁 / 限流 / Bloom / GEO / 排行 / 队列 / 延迟队列 / PubSub / 位图 / HLL / 社交 / 分布式 ID |
| `com.lind.data.mongodb` | mongodb-driver-sync | CRUD / 查询 / 聚合 / GEO / TTL / 全文 / 序列 |
| `com.lind.data.elasticsearch` | elasticsearch-java | 文档 / 全文检索 / 聚合 / 地理距离 |

## 依赖

```xml
<dependency>
    <groupId>com.lind</groupId>
    <artifactId>lind-data-framework</artifactId>
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

# lind-data-starter

Spring Boot 3 Starter：在官方 Spring Data Redis / MongoDB / Elasticsearch 与 Apache HBase Client 之上提供场景封装与自动配置。

与 [`lind-data-frameless`](../lind-data-frameless/README.md)（无 Spring、直连客户端）互补：Boot 应用优先本模块。

## 包文档

| 包 | 说明文档 |
|---|---|
| Redis | [redis/readme.md](src/main/java/com/lind/data/starter/redis/readme.md) |
| MongoDB | [mongodb/readme.md](src/main/java/com/lind/data/starter/mongodb/readme.md) |
| Elasticsearch | [elasticsearch/readme.md](src/main/java/com/lind/data/starter/elasticsearch/readme.md) |
| HBase | [hbase/readme.md](src/main/java/com/lind/data/starter/hbase/readme.md) |

## 依赖

```xml
<dependency>
  <groupId>com.lind</groupId>
  <artifactId>lind-data-starter</artifactId>
  <version>${project.version}</version>
</dependency>

<!-- 按需引入官方 Starter（本模块对它们为 optional） -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
<!-- HBase 无官方 Boot Starter，需显式引入 -->
<dependency>
  <groupId>org.apache.hbase</groupId>
  <artifactId>hbase-client</artifactId>
</dependency>
```

## 自动配置

| 开关 | 默认 | Bean |
|------|------|------|
| `lind.data.redis.enabled` | `true` | `LindSpringRedis`（需已有 `StringRedisTemplate`） |
| `lind.data.mongo.enabled` | `true` | `LindSpringMongo`（需已有 `MongoTemplate`） |
| `lind.data.elasticsearch.enabled` | `true` | `LindSpringElasticsearch`（需已有 `ElasticsearchOperations`） |
| `lind.data.hbase.enabled` | `false` | `Connection` + `LindHBaseTemplate` |

连接信息使用 Spring Boot 官方属性；HBase 使用 `lind.data.hbase.*`，详见 [hbase/readme.md](src/main/java/com/lind/data/starter/hbase/readme.md)。

## 快速用法

```java
@Autowired LindSpringRedis redis;
redis.cache().set("k", "v", Duration.ofMinutes(5));
redis.lock("order:1", Duration.ofSeconds(30)).tryLock();
redis.rateLimiter("api:ip", 100, Duration.ofSeconds(1)).tryAcquire();
redis.delayQueue("jobs").scheduleAfterMillis("payload", 5_000);
redis.idGenerator("order:seq").nextId();

@Autowired LindSpringMongo mongo;
mongo.documents().insert(entity);
mongo.documents().nextSequence("order");
mongo.query().findAnd(Order.class, 1, 20,
    Criteria.where("status").is("PAID"), Criteria.where("amount").gte(100));
mongo.aggregation().groupCount("orders", "status");

@Autowired LindSpringElasticsearch es;
es.documents().save(doc);
es.search().multiMatch("耳机", List.of("title", "desc"), 1, 20, Product.class);
es.aggregation().terms("brand.keyword", 10, Product.class);

@Autowired LindHBaseTemplate hbase;
hbase.putString("t", "rk", "cf", "q", "v");
```

## 规范说明

- 使用 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册自动配置（Boot 3）。
- `@ConditionalOnClass` / `@ConditionalOnBean` / `@ConditionalOnProperty` 保证按需装配。
- 场景类可被用户自定义 Bean 覆盖（`@ConditionalOnMissingBean`）。
- 场景单测 mock 底层 Template / Connection，不强制本机中间件。

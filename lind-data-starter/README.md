# lind-data-starter

Spring Boot 3 Starter：在官方 Spring Data Redis / MongoDB / Elasticsearch 与 Apache HBase Client 之上提供场景封装与自动配置。

与无link ../lind-data-frameless `lind-data-frameless`}（无 Spring、直连客户端）互补：Boot 应用优先本模块。

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

HBase 连接示例：

```yaml
lind:
  data:
    hbase:
      enabled: true
      zookeeper-quorum: 127.0.0.1
      zookeeper-client-port: "2181"
      zookeeper-znode-parent: /hbase
```

连接信息仍使用 Spring Boot 官方属性，例如：

```yaml
spring:
  data:
    redis:
      host: 127.0.0.1
    mongodb:
      uri: mongodb://127.0.0.1:27017/demo
    elasticsearch:
      repositories:
        enabled: true
```

## 用法示例

```java
@Autowired
LindSpringRedis redis;

redis.cache().set("k", "v", Duration.ofMinutes(5));
boolean locked = redis.lock("order:1", Duration.ofSeconds(30)).tryLock();
boolean ok = redis.rateLimiter("api:ip", 100, Duration.ofSeconds(1)).tryAcquire();
redis.delayQueue("jobs").scheduleAfterMillis("payload", 5_000);
long id = redis.idGenerator("order:seq").nextId();
```

```java
@Autowired
LindSpringMongo mongo;

mongo.documents().insert(entity);
long seq = mongo.documents().nextSequence("order");
```

```java
@Autowired
LindSpringElasticsearch es;

es.documents().save(doc);
es.documents().match("title", "keyword", MyDoc.class);
```

```java
@Autowired
LindHBaseTemplate hbase;

hbase.putString("t", "rk", "cf", "q", "v");
```

## 规范说明

- 使用 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册自动配置（Boot 3）。
- `@ConditionalOnClass` / `@ConditionalOnBean` / `@ConditionalOnProperty` 保证按需装配。
- 场景类可被用户自定义 Bean 覆盖（`@ConditionalOnMissingBean`）。

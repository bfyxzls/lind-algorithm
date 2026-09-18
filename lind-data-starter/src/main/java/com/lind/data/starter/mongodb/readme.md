# mongodb 包（com.lind.data.starter.mongodb）

基于 **Spring Data MongoDB**（`MongoTemplate`）的文档 CRUD、多条件检索与聚合，由 `MongoStarterAutoConfiguration` 自动装配 `LindSpringMongo`。

## 使用场景

| 组件 | 能力 | 典型场景 |
|---|---|---|
| `SpringMongoDocuments` | insert / findById / find / save / remove / nextSequence | 文档持久化、全局序列 |
| `SpringMongoQuery` | AND / OR / Map 等值 / 分页排序 / count | **多条件检索**（订单列表、筛选） |
| `SpringMongoAggregation` | groupCount / sum / avg / 自定义 Aggregation | **聚合统计**（状态分布、金额汇总） |

## 启用条件

1. classpath 存在 `MongoTemplate`（`spring-boot-starter-data-mongodb`）
2. 容器中已有 `MongoTemplate` Bean
3. `lind.data.mongo.enabled=true`（默认开启）

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://127.0.0.1:27017/demo
lind:
  data:
    mongo:
      enabled: true
```

## 调用方法

### 文档 CRUD

```java
@Autowired
LindSpringMongo mongo;

User saved = mongo.documents().insert(new User("lind"));
mongo.documents().findById(saved.getId(), User.class);
long orderId = mongo.documents().nextSequence("order");
```

### 多条件检索

```java
SpringMongoQuery query = mongo.query();

// AND：status=PAID 且 amount >= 100，按创建Time 降序分页
List<Order> orders = query.findAnd(
    Order.class, 1, 20,
    Sort.by(Sort.Direction.DESC, "createTime"),
    Criteria.where("status").is("PAID"),
    Criteria.where("amount").gte(100));

// OR
List<Order> orList = query.findOr(
    Order.class, 1, 10,
    Criteria.where("status").is("PAID"),
    Criteria.where("status").is("REFUND"));

// Map 等值 AND
List<Order> eq = query.findByEquals(
    Map.of("userId", "u1", "status", "PAID"), Order.class, 1, 20);

// 计数
long n = query.countAnd(Order.class,
    Criteria.where("status").is("PAID"),
    Criteria.where("amount").gte(100));
```

### 聚合计算

```java
SpringMongoAggregation agg = mongo.aggregation();

// 按 status 分组计数
List<Document> byStatus = agg.groupCount("orders", "status");
// 或按实体集合
List<Document> byStatus2 = agg.groupCount(Order.class, "status");

// 金额求和 / 平均
List<Document> total = agg.sum("orders", "amount");
List<Document> avg = agg.avg("orders", "amount");

// 自定义管道
Aggregation pipeline = Aggregation.newAggregation(
    Aggregation.match(Criteria.where("status").is("PAID")),
    Aggregation.group("userId").sum("amount").as("total"),
    Aggregation.sort(Sort.Direction.DESC, "total"));
List<Document> ranking = agg.aggregate(pipeline, "orders", Document.class);
```

## 注意

- `nextSequence` 使用 upsert，首次调用会自动创建 `counters` 文档。
- 复杂索引、事务请结合 `MongoTemplate` / `MongoTransactionManager`。
- 单测 mock `MongoTemplate`，无需本机 MongoDB。

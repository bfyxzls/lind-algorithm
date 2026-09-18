# elasticsearch 包（com.lind.data.starter.elasticsearch）

基于 **Spring Data Elasticsearch**（`ElasticsearchOperations`）的文档写入、分词检索与聚合，由 `ElasticsearchStarterAutoConfiguration` 自动装配 `LindSpringElasticsearch`。

## 使用场景

| 组件 | 能力 | 典型场景 |
|---|---|---|
| `SpringEsDocuments` | save / get / delete / Criteria 检索 | 索引维护、简单条件查询 |
| `SpringEsSearch` | match / multi_match / match_phrase | **分词全文检索**（商品、文章搜索） |
| `SpringEsAggregation` | terms / sum / avg / 自定义聚合 | **聚合统计**（品牌分布、销售额） |

## 启用条件

1. classpath 存在 `ElasticsearchOperations`（`spring-boot-starter-data-elasticsearch`）
2. 容器中已有 `ElasticsearchOperations` Bean
3. `lind.data.elasticsearch.enabled=true`（默认开启）

```yaml
spring:
  elasticsearch:
    uris: http://127.0.0.1:9200
lind:
  data:
    elasticsearch:
      enabled: true
```

## 调用方法

### 文档 CRUD

```java
@Autowired
LindSpringElasticsearch es;

Product p = es.documents().save(new Product("1", "无线耳机"));
es.documents().get("1", Product.class);
es.documents().delete("1", Product.class);
```

### 分词检索

```java
// 单字段 match（分析器分词）
SearchHits<Product> hits = es.search().match("title", "无线耳机", 1, 20, Product.class);

// 多字段 multi_match
SearchHits<Product> multi = es.search().multiMatch(
    "蓝牙降噪", List.of("title", "description"), 1, 20, Product.class);

// 短语 match_phrase（词序敏感）
SearchHits<Product> phrase = es.search().matchPhrase("title", "无线耳机", 1, 10, Product.class);
```

### 聚合计算

```java
// Terms：按品牌分桶（text 字段用 brand.keyword）
Map<String, Long> byBrand = es.aggregation().terms("brand.keyword", 10, Product.class);

// 指标
double total = es.aggregation().sum("price", Product.class);
double avgPrice = es.aggregation().avg("price", Product.class);

// 自定义聚合
Aggregation agg = Aggregation.of(a -> a.terms(t -> t.field("category.keyword").size(20)));
SearchHits<Product> raw = es.aggregation().aggregate("by_cat", agg, Product.class);
```

## 注意

- match / multi_match 依赖字段 mapping 为 `text`（带 analyzer）；terms 聚合对 text 字段请用 `.keyword`。
- 索引 mapping、分片副本请用官方 `@Setting` / `@Mapping` 或运维模板管理。
- 单测 mock `ElasticsearchOperations`，无需本机 ES。

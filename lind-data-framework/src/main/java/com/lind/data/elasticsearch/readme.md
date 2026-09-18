# elasticsearch 包（com.lind.data.elasticsearch）

基于官方 **[elasticsearch-java](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/index.html)** 的场景封装（无 Spring 依赖）。

## 入口

```java
try (LindElasticsearch es = LindElasticsearch.connect("127.0.0.1", 9200)) {
    es.documents("products").save("1", Map.of("title", "phone", "price", 99));
    es.search("products").match("title", "phone", 1, 10);
}
```

也可注入自定义 `ElasticsearchCommands`（便于单测）。

## 场景一览

| 组件 | 能力 | 场景 |
|---|---|---|
| `EsDocuments` | index/get/update/delete/bulk | 文档写入与维护 |
| `EsSearch` | match / multi_match | 全文检索 |
| `EsAggregation` | terms 聚合 | 分类统计 / 热词 |
| `EsGeoSearch` | geo_distance | 附近检索 |

## 示例

```java
es.documents("products").ensureIndex();
es.documents("products").bulkSave(List.of(
    Map.of("title", "a"),
    Map.of("title", "b")
));

EsSearchResult result = es.search("products")
    .multiMatch("无线耳机", List.of("title", "desc"), 1, 20);

Map<String, Long> byBrand = es.aggregation("products").terms("brand.keyword", 10);
```

## 注意

- 默认连接无认证 HTTP；生产请自行扩展 HTTPS / API Key。
- `geo_distance` 要求 mapping 中字段为 `geo_point`。
- 单测 mock `ElasticsearchCommands`，不强制本机 Elasticsearch。

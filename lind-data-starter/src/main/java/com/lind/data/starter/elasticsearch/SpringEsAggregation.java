package com.lind.data.starter.elasticsearch;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregation;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.AggregationsContainer;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;

/**
 * 聚合计算（terms / sum / avg），基于 {@link NativeQuery}。
 */
public class SpringEsAggregation {

	private final ElasticsearchOperations operations;

	public SpringEsAggregation(ElasticsearchOperations operations) {
		this.operations = Objects.requireNonNull(operations, "operations");
	}

	/**
	 * Terms 聚合：按字段分桶计数。text 字段请使用 {@code field.keyword}。
	 * @return key → doc_count（保持桶顺序）
	 */
	public <T> Map<String, Long> terms(String field, int size, Class<T> entityClass) {
		Objects.requireNonNull(field, "field");
		Objects.requireNonNull(entityClass, "entityClass");
		if (size < 1) {
			throw new IllegalArgumentException("size must be >= 1");
		}
		String aggName = "terms_agg";
		Query query = NativeQuery.builder().withMaxResults(0).withQuery(q -> q.matchAll(m -> m))
				.withAggregation(aggName, Aggregation.of(a -> a.terms(t -> t.field(field).size(size)))).build();
		SearchHits<T> hits = operations.search(query, entityClass);
		return parseTerms(hits.getAggregations(), aggName);
	}

	/**
	 * Sum 指标聚合。
	 */
	public <T> double sum(String field, Class<T> entityClass) {
		Objects.requireNonNull(field, "field");
		return metric(entityClass, "sum_agg", Aggregation.of(a -> a.sum(s -> s.field(field))), Aggregate::isSum,
				agg -> nullToZero(agg.sum().value()));
	}

	/**
	 * Avg 指标聚合。
	 */
	public <T> double avg(String field, Class<T> entityClass) {
		Objects.requireNonNull(field, "field");
		return metric(entityClass, "avg_agg", Aggregation.of(a -> a.avg(s -> s.field(field))), Aggregate::isAvg,
				agg -> nullToZero(agg.avg().value()));
	}

	/**
	 * 自定义聚合：返回原始 {@link SearchHits}，由调用方解析 buckets。
	 */
	public <T> SearchHits<T> aggregate(String aggName, Aggregation aggregation, Class<T> entityClass) {
		Objects.requireNonNull(aggName, "aggName");
		Objects.requireNonNull(aggregation, "aggregation");
		Objects.requireNonNull(entityClass, "entityClass");
		Query query = NativeQuery.builder().withMaxResults(0).withQuery(q -> q.matchAll(m -> m))
				.withAggregation(aggName, aggregation).build();
		return operations.search(query, entityClass);
	}

	private <T> double metric(Class<T> entityClass, String aggName, Aggregation aggregation,
			Predicate<Aggregate> typeCheck, Function<Aggregate, Double> extractor) {
		SearchHits<T> hits = aggregate(aggName, aggregation, entityClass);
		Aggregate aggregate = resolveAggregate(hits.getAggregations(), aggName);
		if (aggregate == null || !typeCheck.test(aggregate)) {
			return 0D;
		}
		return extractor.apply(aggregate);
	}

	private static double nullToZero(Double value) {
		return value == null ? 0D : value;
	}

	static Map<String, Long> parseTerms(AggregationsContainer<?> container, String aggName) {
		Aggregate aggregate = resolveAggregate(container, aggName);
		if (aggregate == null || !aggregate.isSterms()) {
			return Map.of();
		}
		Map<String, Long> result = new LinkedHashMap<>();
		for (StringTermsBucket bucket : aggregate.sterms().buckets().array()) {
			result.put(bucket.key().stringValue(), bucket.docCount());
		}
		return Collections.unmodifiableMap(result);
	}

	static Aggregate resolveAggregate(AggregationsContainer<?> container, String aggName) {
		if (!(container instanceof ElasticsearchAggregations esAggs)) {
			return null;
		}
		ElasticsearchAggregation esAgg = esAggs.get(aggName);
		if (esAgg == null) {
			return null;
		}
		return esAgg.aggregation().getAggregate();
	}

}

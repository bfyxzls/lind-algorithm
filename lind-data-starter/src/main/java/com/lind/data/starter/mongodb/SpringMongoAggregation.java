package com.lind.data.starter.mongodb;

import java.util.List;
import java.util.Objects;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

/**
 * 聚合计算（groupCount / 自定义 pipeline）。
 */
public class SpringMongoAggregation {

	private final MongoTemplate mongo;

	public SpringMongoAggregation(MongoTemplate mongo) {
		this.mongo = Objects.requireNonNull(mongo, "mongo");
	}

	/**
	 * 按字段分组计数，按 count 降序。
	 */
	public List<Document> groupCount(String collection, String groupField) {
		Objects.requireNonNull(collection, "collection");
		Objects.requireNonNull(groupField, "groupField");
		Aggregation aggregation = Aggregation.newAggregation(
				Aggregation.group(groupField).count().as("count"),
				Aggregation.sort(Sort.Direction.DESC, "count"));
		return aggregate(aggregation, collection, Document.class);
	}

	/**
	 * 按实体映射集合分组计数。
	 */
	public <T> List<Document> groupCount(Class<T> entityClass, String groupField) {
		Objects.requireNonNull(entityClass, "entityClass");
		Objects.requireNonNull(groupField, "groupField");
		Aggregation aggregation = Aggregation.newAggregation(
				Aggregation.group(groupField).count().as("count"),
				Aggregation.sort(Sort.Direction.DESC, "count"));
		return aggregate(aggregation, entityClass, Document.class);
	}

	/**
	 * 数值字段求和：{@code { _id: null, total: { $sum: "$field" } }}。
	 */
	public List<Document> sum(String collection, String field) {
		Objects.requireNonNull(collection, "collection");
		Objects.requireNonNull(field, "field");
		Aggregation aggregation = Aggregation.newAggregation(Aggregation.group().sum(field).as("total"));
		return aggregate(aggregation, collection, Document.class);
	}

	/**
	 * 数值字段求平均。
	 */
	public List<Document> avg(String collection, String field) {
		Objects.requireNonNull(collection, "collection");
		Objects.requireNonNull(field, "field");
		Aggregation aggregation = Aggregation.newAggregation(Aggregation.group().avg(field).as("avg"));
		return aggregate(aggregation, collection, Document.class);
	}

	public <O> List<O> aggregate(Aggregation aggregation, String collection, Class<O> outputType) {
		Objects.requireNonNull(aggregation, "aggregation");
		Objects.requireNonNull(collection, "collection");
		Objects.requireNonNull(outputType, "outputType");
		AggregationResults<O> results = mongo.aggregate(aggregation, collection, outputType);
		return results.getMappedResults();
	}

	public <T, O> List<O> aggregate(Aggregation aggregation, Class<T> entityClass, Class<O> outputType) {
		Objects.requireNonNull(aggregation, "aggregation");
		Objects.requireNonNull(entityClass, "entityClass");
		Objects.requireNonNull(outputType, "outputType");
		AggregationResults<O> results = mongo.aggregate(aggregation, entityClass, outputType);
		return results.getMappedResults();
	}

}

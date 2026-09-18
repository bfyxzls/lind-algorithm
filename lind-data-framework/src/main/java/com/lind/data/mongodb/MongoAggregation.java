package com.lind.data.mongodb;

import java.util.List;
import java.util.Objects;
import org.bson.Document;

/**
 * 聚合分析（group / count 等常用管道）。
 */
public final class MongoAggregation {

	private final MongoCommands mongo;

	private final String collection;

	public MongoAggregation(MongoCommands mongo, String collection) {
		this.mongo = Objects.requireNonNull(mongo, "mongo");
		this.collection = Objects.requireNonNull(collection, "collection");
	}

	public List<Document> groupCount(String groupField) {
		Objects.requireNonNull(groupField, "groupField");
		List<Document> pipeline = List.of(
				new Document("$group", new Document("_id", "$" + groupField).append("count", new Document("$sum", 1))),
				new Document("$sort", new Document("count", -1)));
		return mongo.aggregate(collection, pipeline);
	}

	public List<Document> pipeline(List<Document> stages) {
		return mongo.aggregate(collection, Objects.requireNonNull(stages, "stages"));
	}

}

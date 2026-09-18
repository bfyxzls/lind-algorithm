package com.lind.data.mongodb;

import java.util.List;
import java.util.Objects;
import org.bson.Document;
import org.bson.conversions.Bson;

/**
 * 条件查询 / 分页 / 排序。
 */
public final class MongoQuery {

	private final MongoCommands mongo;

	private final String collection;

	public MongoQuery(MongoCommands mongo, String collection) {
		this.mongo = Objects.requireNonNull(mongo, "mongo");
		this.collection = Objects.requireNonNull(collection, "collection");
	}

	public List<Document> find(Bson filter, int page, int size) {
		if (page < 1 || size < 1) {
			throw new IllegalArgumentException("page/size must be >= 1");
		}
		return mongo.find(collection, filter, (page - 1) * size, size, null);
	}

	public List<Document> find(Bson filter, int page, int size, Bson sort) {
		if (page < 1 || size < 1) {
			throw new IllegalArgumentException("page/size must be >= 1");
		}
		return mongo.find(collection, filter, (page - 1) * size, size, sort);
	}

	public long count(Bson filter) {
		return mongo.count(collection, filter);
	}

	public List<Document> eq(String field, Object value, int limit) {
		return mongo.find(collection, new Document(field, value), 0, limit, null);
	}

}

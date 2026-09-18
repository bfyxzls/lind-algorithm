package com.lind.data.mongodb;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.bson.Document;

/**
 * 文档 CRUD。
 */
public final class MongoDocuments {

	private final MongoCommands mongo;

	private final String collection;

	public MongoDocuments(MongoCommands mongo, String collection) {
		this.mongo = Objects.requireNonNull(mongo, "mongo");
		this.collection = Objects.requireNonNull(collection, "collection");
	}

	public String insert(Document document) {
		return mongo.insert(collection, document);
	}

	public Optional<Document> findById(String id) {
		return mongo.findById(collection, id);
	}

	public long updateById(String id, Document fields) {
		return mongo.updateById(collection, id, Objects.requireNonNull(fields, "fields"));
	}

	public long deleteById(String id) {
		return mongo.deleteById(collection, id);
	}

	public List<Document> findAll(int skip, int limit) {
		return mongo.find(collection, new Document(), skip, limit, null);
	}

	public long count() {
		return mongo.count(collection, new Document());
	}

}

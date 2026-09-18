package com.lind.data.mongodb;

import java.util.List;
import java.util.Objects;
import org.bson.Document;

/**
 * 全文检索（需先建 text 索引）。
 */
public final class MongoTextSearch {

	private final MongoCommands mongo;

	private final String collection;

	public MongoTextSearch(MongoCommands mongo, String collection) {
		this.mongo = Objects.requireNonNull(mongo, "mongo");
		this.collection = Objects.requireNonNull(collection, "collection");
	}

	public void ensureTextIndex(String... fields) {
		if (fields == null || fields.length == 0) {
			throw new IllegalArgumentException("fields must not be empty");
		}
		Document keys = new Document();
		for (String field : fields) {
			keys.append(field, "text");
		}
		mongo.createIndex(collection, keys, false);
	}

	public List<Document> search(String text, int limit) {
		Objects.requireNonNull(text, "text");
		Document filter = new Document("$text", new Document("$search", text));
		Document sort = new Document("score", new Document("$meta", "textScore"));
		Document projection = new Document("score", new Document("$meta", "textScore"));
		// find API 无 projection，用 aggregate 补齐分数
		List<Document> pipeline = List.of(new Document("$match", filter), new Document("$addFields", projection),
				new Document("$sort", sort), new Document("$limit", Math.max(1, limit)));
		return mongo.aggregate(collection, pipeline);
	}

}

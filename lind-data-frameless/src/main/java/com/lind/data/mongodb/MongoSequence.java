package com.lind.data.mongodb;

import com.mongodb.client.model.Updates;
import java.util.Objects;
import org.bson.Document;

/**
 * 分布式序列：findAndModify / $inc。
 */
public final class MongoSequence {

	private final MongoCommands mongo;

	private final String collection;

	public MongoSequence(MongoCommands mongo, String collection) {
		this.mongo = Objects.requireNonNull(mongo, "mongo");
		this.collection = Objects.requireNonNull(collection, "collection");
	}

	public long next(String sequenceName) {
		return next(sequenceName, 1L);
	}

	public long next(String sequenceName, long step) {
		Objects.requireNonNull(sequenceName, "sequenceName");
		if (step <= 0) {
			throw new IllegalArgumentException("step must be > 0");
		}
		Document after = mongo.findOneAndUpdate(collection, new Document("_id", sequenceName),
				Updates.inc("value", step), true, true);
		Object value = after.get("value");
		if (value instanceof Number number) {
			return number.longValue();
		}
		throw new IllegalStateException("sequence value missing for " + sequenceName);
	}

}

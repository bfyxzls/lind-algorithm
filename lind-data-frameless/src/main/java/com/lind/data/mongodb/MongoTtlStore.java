package com.lind.data.mongodb;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import org.bson.Document;

/**
 * TTL 过期文档（如验证码、会话）。
 */
public final class MongoTtlStore {

	private final MongoCommands mongo;

	private final String collection;

	private final String expireField;

	public MongoTtlStore(MongoCommands mongo, String collection) {
		this(mongo, collection, "expireAt");
	}

	public MongoTtlStore(MongoCommands mongo, String collection, String expireField) {
		this.mongo = Objects.requireNonNull(mongo, "mongo");
		this.collection = Objects.requireNonNull(collection, "collection");
		this.expireField = Objects.requireNonNull(expireField, "expireField");
	}

	public void ensureTtlIndex() {
		mongo.createTtlIndex(collection, expireField, 0);
	}

	public String put(Document data, Instant expireAt) {
		Document doc = new Document(Objects.requireNonNull(data, "data"));
		doc.put(expireField, Date.from(Objects.requireNonNull(expireAt, "expireAt")));
		return mongo.insert(collection, doc);
	}

}

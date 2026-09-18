package com.lind.data.mongodb;

import java.util.Objects;

/**
 * MongoDB 场景门面：基于官方 Sync Driver。
 *
 * <pre>
 * try (LindMongo mongo = LindMongo.connect("mongodb://127.0.0.1:27017", "demo")) {
 *     mongo.documents("users").insert(new Document("name", "lind"));
 * }
 * </pre>
 */
public final class LindMongo implements AutoCloseable {

	private final MongoCommands commands;

	public LindMongo(MongoCommands commands) {
		this.commands = Objects.requireNonNull(commands, "commands");
	}

	public static LindMongo connect(String connectionString, String database) {
		return new LindMongo(SyncMongoCommands.connect(connectionString, database));
	}

	public MongoCommands commands() {
		return commands;
	}

	public MongoDocuments documents(String collection) {
		return new MongoDocuments(commands, collection);
	}

	public MongoQuery query(String collection) {
		return new MongoQuery(commands, collection);
	}

	public MongoAggregation aggregation(String collection) {
		return new MongoAggregation(commands, collection);
	}

	public MongoGeo geo(String collection) {
		return new MongoGeo(commands, collection);
	}

	public MongoGeo geo(String collection, String locationField) {
		return new MongoGeo(commands, collection, locationField);
	}

	public MongoTtlStore ttl(String collection) {
		return new MongoTtlStore(commands, collection);
	}

	public MongoTextSearch textSearch(String collection) {
		return new MongoTextSearch(commands, collection);
	}

	public MongoSequence sequence(String collection) {
		return new MongoSequence(commands, collection);
	}

	@Override
	public void close() {
		commands.close();
	}

}

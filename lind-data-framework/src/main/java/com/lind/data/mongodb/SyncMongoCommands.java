package com.lind.data.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

/**
 * 基于官方 {@code mongodb-driver-sync} 的 {@link MongoCommands} 实现。
 */
public final class SyncMongoCommands implements MongoCommands {

	private final MongoClient client;

	private final MongoDatabase database;

	private final boolean closeOnClose;

	public SyncMongoCommands(MongoClient client, String databaseName) {
		this(client, databaseName, true);
	}

	public SyncMongoCommands(MongoClient client, String databaseName, boolean closeOnClose) {
		this.client = Objects.requireNonNull(client, "client");
		this.database = client.getDatabase(Objects.requireNonNull(databaseName, "databaseName"));
		this.closeOnClose = closeOnClose;
	}

	public static SyncMongoCommands connect(String connectionString, String databaseName) {
		return new SyncMongoCommands(MongoClients.create(connectionString), databaseName);
	}

	private MongoCollection<Document> col(String collection) {
		return database.getCollection(Objects.requireNonNull(collection, "collection"));
	}

	@Override
	public String insert(String collection, Document document) {
		Document doc = new Document(Objects.requireNonNull(document, "document"));
		if (!doc.containsKey("_id")) {
			doc.put("_id", new ObjectId());
		}
		col(collection).insertOne(doc);
		return String.valueOf(doc.get("_id"));
	}

	@Override
	public Optional<Document> findById(String collection, String id) {
		return Optional.ofNullable(col(collection).find(idFilter(id)).first());
	}

	@Override
	public Optional<Document> findOne(String collection, Bson filter) {
		return Optional.ofNullable(col(collection).find(Objects.requireNonNull(filter, "filter")).first());
	}

	@Override
	public List<Document> find(String collection, Bson filter, int skip, int limit, Bson sort) {
		var iterable = col(collection).find(filter == null ? new Document() : filter).skip(Math.max(0, skip));
		if (limit > 0) {
			iterable = iterable.limit(limit);
		}
		if (sort != null) {
			iterable = iterable.sort(sort);
		}
		List<Document> list = new ArrayList<>();
		iterable.into(list);
		return list;
	}

	@Override
	public long updateById(String collection, String id, Document fields) {
		UpdateResult result = col(collection).updateOne(idFilter(id), new Document("$set", fields));
		return result.getModifiedCount();
	}

	@Override
	public long updateMany(String collection, Bson filter, Document fields) {
		UpdateResult result = col(collection).updateMany(filter, new Document("$set", fields));
		return result.getModifiedCount();
	}

	@Override
	public long deleteById(String collection, String id) {
		DeleteResult result = col(collection).deleteOne(idFilter(id));
		return result.getDeletedCount();
	}

	@Override
	public long deleteMany(String collection, Bson filter) {
		return col(collection).deleteMany(filter).getDeletedCount();
	}

	@Override
	public long count(String collection, Bson filter) {
		return col(collection).countDocuments(filter == null ? new Document() : filter);
	}

	@Override
	public List<Document> aggregate(String collection, List<? extends Bson> pipeline) {
		List<Document> list = new ArrayList<>();
		col(collection).aggregate(pipeline).into(list);
		return list;
	}

	@Override
	public String createIndex(String collection, Bson keys, boolean unique) {
		IndexOptions options = new IndexOptions().unique(unique);
		return col(collection).createIndex(keys, options);
	}

	@Override
	public String createTtlIndex(String collection, String dateField, long expireAfterSeconds) {
		IndexOptions options = new IndexOptions().expireAfter(expireAfterSeconds, TimeUnit.SECONDS);
		return col(collection).createIndex(new Document(dateField, 1), options);
	}

	@Override
	public Document findOneAndUpdate(String collection, Bson filter, Bson update, boolean upsert, boolean returnAfter) {
		FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().upsert(upsert)
				.returnDocument(returnAfter ? ReturnDocument.AFTER : ReturnDocument.BEFORE);
		return col(collection).findOneAndUpdate(filter, update, options);
	}

	private static Bson idFilter(String id) {
		Objects.requireNonNull(id, "id");
		if (ObjectId.isValid(id)) {
			return new Document("_id", new ObjectId(id));
		}
		return new Document("_id", id);
	}

	@Override
	public void close() {
		if (closeOnClose) {
			client.close();
		}
	}

}

package com.lind.data.mongodb;

import java.util.List;
import java.util.Optional;
import org.bson.Document;
import org.bson.conversions.Bson;

/**
 * MongoDB 命令抽象，便于场景封装与单测替换。
 */
public interface MongoCommands extends AutoCloseable {

	String insert(String collection, Document document);

	Optional<Document> findById(String collection, String id);

	Optional<Document> findOne(String collection, Bson filter);

	List<Document> find(String collection, Bson filter, int skip, int limit, Bson sort);

	long updateById(String collection, String id, Document fields);

	long updateMany(String collection, Bson filter, Document fields);

	long deleteById(String collection, String id);

	long deleteMany(String collection, Bson filter);

	long count(String collection, Bson filter);

	List<Document> aggregate(String collection, List<? extends Bson> pipeline);

	String createIndex(String collection, Bson keys, boolean unique);

	String createTtlIndex(String collection, String dateField, long expireAfterSeconds);

	Document findOneAndUpdate(String collection, Bson filter, Bson update, boolean upsert, boolean returnAfter);

	@Override
	void close();

}

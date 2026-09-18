package com.lind.data.starter.mongodb;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * 基于 {@link MongoTemplate} 的文档 CRUD。
 */
public class SpringMongoDocuments {

	private final MongoTemplate mongo;

	public SpringMongoDocuments(MongoTemplate mongo) {
		this.mongo = Objects.requireNonNull(mongo, "mongo");
	}

	public <T> T insert(T document) {
		return mongo.insert(Objects.requireNonNull(document, "document"));
	}

	public <T> Optional<T> findById(Object id, Class<T> entityClass) {
		return Optional.ofNullable(mongo.findById(Objects.requireNonNull(id, "id"), entityClass));
	}

	public <T> List<T> find(Query query, Class<T> entityClass) {
		return mongo.find(Objects.requireNonNull(query, "query"), entityClass);
	}

	public <T> T save(T document) {
		return mongo.save(Objects.requireNonNull(document, "document"));
	}

	public long remove(Query query, Class<?> entityClass) {
		return mongo.remove(Objects.requireNonNull(query, "query"), entityClass).getDeletedCount();
	}

	/**
	 * 原子自增序列（集合 {@code counters}，字段 {@code seq}）。
	 */
	public long nextSequence(String name) {
		Objects.requireNonNull(name, "name");
		Query query = Query.query(Criteria.where("_id").is(name));
		Update update = new Update().inc("seq", 1);
		FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true).upsert(true);
		Counter counter = mongo.findAndModify(query, update, options, Counter.class, "counters");
		return counter == null ? 0L : counter.getSeq();
	}

	public static class Counter {

		@Id
		private String id;

		private long seq;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public long getSeq() {
			return seq;
		}

		public void setSeq(long seq) {
			this.seq = seq;
		}

	}

}

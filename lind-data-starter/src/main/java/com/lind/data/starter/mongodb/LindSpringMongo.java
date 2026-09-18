package com.lind.data.starter.mongodb;

import java.util.Objects;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * MongoDB 场景门面（Spring Data MongoDB）。
 */
public class LindSpringMongo {

	private final MongoTemplate mongo;

	public LindSpringMongo(MongoTemplate mongo) {
		this.mongo = Objects.requireNonNull(mongo, "mongo");
	}

	public MongoTemplate template() {
		return mongo;
	}

	public SpringMongoDocuments documents() {
		return new SpringMongoDocuments(mongo);
	}

	public SpringMongoQuery query() {
		return new SpringMongoQuery(mongo);
	}

	public SpringMongoAggregation aggregation() {
		return new SpringMongoAggregation(mongo);
	}

}

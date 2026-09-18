package com.lind.data.starter.mongodb;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * 多条件检索（AND / OR）与分页排序。
 */
public class SpringMongoQuery {

	private final MongoTemplate mongo;

	public SpringMongoQuery(MongoTemplate mongo) {
		this.mongo = Objects.requireNonNull(mongo, "mongo");
	}

	/**
	 * 多条件 AND 查询并分页。
	 * @param page 从 1 开始
	 */
	public <T> List<T> findAnd(Class<T> entityClass, int page, int size, Criteria... conditions) {
		return find(and(conditions), entityClass, page, size, null);
	}

	/**
	 * 多条件 AND + 排序。
	 */
	public <T> List<T> findAnd(Class<T> entityClass, int page, int size, Sort sort, Criteria... conditions) {
		return find(and(conditions), entityClass, page, size, sort);
	}

	/**
	 * 多条件 OR 查询并分页。
	 */
	public <T> List<T> findOr(Class<T> entityClass, int page, int size, Criteria... conditions) {
		return find(or(conditions), entityClass, page, size, null);
	}

	/**
	 * 等值 Map 转 AND 条件（field → value）。
	 */
	public <T> List<T> findByEquals(Map<String, Object> equals, Class<T> entityClass, int page, int size) {
		Objects.requireNonNull(equals, "equals");
		if (equals.isEmpty()) {
			throw new IllegalArgumentException("equals must not be empty");
		}
		Criteria[] conditions = equals.entrySet().stream()
				.map(e -> Criteria.where(e.getKey()).is(e.getValue()))
				.toArray(Criteria[]::new);
		return findAnd(entityClass, page, size, conditions);
	}

	public <T> List<T> find(Criteria criteria, Class<T> entityClass, int page, int size) {
		return find(criteria, entityClass, page, size, null);
	}

	public <T> List<T> find(Criteria criteria, Class<T> entityClass, int page, int size, Sort sort) {
		Objects.requireNonNull(criteria, "criteria");
		Objects.requireNonNull(entityClass, "entityClass");
		requirePage(page, size);
		Query query = new Query(criteria).skip((long) (page - 1) * size).limit(size);
		if (sort != null) {
			query.with(sort);
		}
		return mongo.find(query, entityClass);
	}

	public long count(Criteria criteria, Class<?> entityClass) {
		Objects.requireNonNull(criteria, "criteria");
		Objects.requireNonNull(entityClass, "entityClass");
		return mongo.count(Query.query(criteria), entityClass);
	}

	public long countAnd(Class<?> entityClass, Criteria... conditions) {
		return count(and(conditions), entityClass);
	}

	public static Criteria and(Criteria... conditions) {
		Objects.requireNonNull(conditions, "conditions");
		if (conditions.length == 0) {
			throw new IllegalArgumentException("conditions must not be empty");
		}
		if (conditions.length == 1) {
			return Objects.requireNonNull(conditions[0], "condition");
		}
		return new Criteria().andOperator(conditions);
	}

	public static Criteria or(Criteria... conditions) {
		Objects.requireNonNull(conditions, "conditions");
		if (conditions.length == 0) {
			throw new IllegalArgumentException("conditions must not be empty");
		}
		if (conditions.length == 1) {
			return Objects.requireNonNull(conditions[0], "condition");
		}
		return new Criteria().orOperator(conditions);
	}

	private static void requirePage(int page, int size) {
		if (page < 1 || size < 1) {
			throw new IllegalArgumentException("page/size must be >= 1");
		}
	}

}

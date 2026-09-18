package com.lind.data.starter.elasticsearch;

import java.util.Objects;
import java.util.Optional;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;

/**
 * 基于 {@link ElasticsearchOperations} 的文档与检索。
 */
public class SpringEsDocuments {

	private final ElasticsearchOperations operations;

	public SpringEsDocuments(ElasticsearchOperations operations) {
		this.operations = Objects.requireNonNull(operations, "operations");
	}

	public <T> T save(T entity) {
		return operations.save(Objects.requireNonNull(entity, "entity"));
	}

	public <T> Optional<T> get(String id, Class<T> entityClass) {
		return Optional.ofNullable(operations.get(Objects.requireNonNull(id, "id"), entityClass));
	}

	public String delete(String id, Class<?> entityClass) {
		return operations.delete(Objects.requireNonNull(id, "id"), entityClass);
	}

	public <T> SearchHits<T> search(Query query, Class<T> entityClass) {
		return operations.search(Objects.requireNonNull(query, "query"), entityClass);
	}

	public <T> SearchHits<T> match(String field, String value, Class<T> entityClass) {
		CriteriaQuery query = new CriteriaQuery(Criteria.where(field).matches(value));
		return operations.search(query, entityClass);
	}

	public boolean indexExists(String indexName) {
		return operations.indexOps(IndexCoordinates.of(indexName)).exists();
	}

}

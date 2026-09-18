package com.lind.data.starter.elasticsearch;

import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;

/**
 * 分词检索（match / multi_match），基于 Elasticsearch 分析器对文本分词后检索。
 */
public class SpringEsSearch {

	private final ElasticsearchOperations operations;

	public SpringEsSearch(ElasticsearchOperations operations) {
		this.operations = Objects.requireNonNull(operations, "operations");
	}

	/**
	 * 单字段 match（分词）：适用于 title / content 等 text 字段。
	 * @param page 从 1 开始
	 * @param size 每页条数
	 */
	public <T> SearchHits<T> match(String field, String text, int page, int size, Class<T> entityClass) {
		Objects.requireNonNull(field, "field");
		Objects.requireNonNull(text, "text");
		Objects.requireNonNull(entityClass, "entityClass");
		requirePage(page, size);
		Query query = NativeQuery.builder()
				.withQuery(q -> q.match(m -> m.field(field).query(text)))
				.withPageable(PageRequest.of(page - 1, size))
				.build();
		return operations.search(query, entityClass);
	}

	/**
	 * 多字段 multi_match（分词）：同一关键词在多个字段中检索。
	 */
	public <T> SearchHits<T> multiMatch(String text, List<String> fields, int page, int size, Class<T> entityClass) {
		Objects.requireNonNull(text, "text");
		Objects.requireNonNull(fields, "fields");
		Objects.requireNonNull(entityClass, "entityClass");
		if (fields.isEmpty()) {
			throw new IllegalArgumentException("fields must not be empty");
		}
		requirePage(page, size);
		Query query = NativeQuery.builder()
				.withQuery(q -> q.multiMatch(m -> m.query(text).fields(fields)))
				.withPageable(PageRequest.of(page - 1, size))
				.build();
		return operations.search(query, entityClass);
	}

	/**
	 * 短语 match_phrase：要求词项顺序与邻近度（仍走分词器）。
	 */
	public <T> SearchHits<T> matchPhrase(String field, String text, int page, int size, Class<T> entityClass) {
		Objects.requireNonNull(field, "field");
		Objects.requireNonNull(text, "text");
		Objects.requireNonNull(entityClass, "entityClass");
		requirePage(page, size);
		Query query = NativeQuery.builder()
				.withQuery(q -> q.matchPhrase(m -> m.field(field).query(text)))
				.withPageable(PageRequest.of(page - 1, size))
				.build();
		return operations.search(query, entityClass);
	}

	private static void requirePage(int page, int size) {
		if (page < 1 || size < 1) {
			throw new IllegalArgumentException("page/size must be >= 1");
		}
	}

}

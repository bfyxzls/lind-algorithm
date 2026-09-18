package com.lind.data.starter.elasticsearch;

import java.util.Objects;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

/**
 * Elasticsearch 场景门面（Spring Data Elasticsearch）。
 */
public class LindSpringElasticsearch {

	private final ElasticsearchOperations operations;

	public LindSpringElasticsearch(ElasticsearchOperations operations) {
		this.operations = Objects.requireNonNull(operations, "operations");
	}

	public ElasticsearchOperations operations() {
		return operations;
	}

	public SpringEsDocuments documents() {
		return new SpringEsDocuments(operations);
	}

}

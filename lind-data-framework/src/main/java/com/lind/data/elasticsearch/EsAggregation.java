package com.lind.data.elasticsearch;

import java.util.Map;
import java.util.Objects;

/**
 * 聚合统计。
 */
public final class EsAggregation {

	private final ElasticsearchCommands es;

	private final String index;

	public EsAggregation(ElasticsearchCommands es, String index) {
		this.es = Objects.requireNonNull(es, "es");
		this.index = Objects.requireNonNull(index, "index");
	}

	public Map<String, Long> terms(String field, int size) {
		return es.termsAgg(index, Objects.requireNonNull(field, "field"), size);
	}

}

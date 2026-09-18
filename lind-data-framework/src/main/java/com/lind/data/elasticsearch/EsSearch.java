package com.lind.data.elasticsearch;

import java.util.List;
import java.util.Objects;

/**
 * 全文检索 / 多字段检索。
 */
public final class EsSearch {

	private final ElasticsearchCommands es;

	private final String index;

	public EsSearch(ElasticsearchCommands es, String index) {
		this.es = Objects.requireNonNull(es, "es");
		this.index = Objects.requireNonNull(index, "index");
	}

	public EsSearchResult match(String field, String text, int page, int size) {
		if (page < 1 || size < 1) {
			throw new IllegalArgumentException("page/size must be >= 1");
		}
		return es.search(index, field, text, (page - 1) * size, size);
	}

	public EsSearchResult multiMatch(String text, List<String> fields, int page, int size) {
		if (page < 1 || size < 1) {
			throw new IllegalArgumentException("page/size must be >= 1");
		}
		return es.multiMatch(index, text, fields, (page - 1) * size, size);
	}

}

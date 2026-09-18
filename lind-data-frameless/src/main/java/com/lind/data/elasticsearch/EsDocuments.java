package com.lind.data.elasticsearch;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 文档索引 / 读写。
 */
public final class EsDocuments {

	private final ElasticsearchCommands es;

	private final String index;

	public EsDocuments(ElasticsearchCommands es, String index) {
		this.es = Objects.requireNonNull(es, "es");
		this.index = Objects.requireNonNull(index, "index");
	}

	public void ensureIndex() {
		es.createIndex(index);
	}

	public String save(String id, Map<String, Object> document) {
		return es.index(index, id, Objects.requireNonNull(document, "document"));
	}

	public Optional<Map<String, Object>> get(String id) {
		return es.get(index, id);
	}

	public boolean update(String id, Map<String, Object> partial) {
		return es.update(index, id, Objects.requireNonNull(partial, "partial"));
	}

	public boolean delete(String id) {
		return es.delete(index, id);
	}

	public int bulkSave(List<Map<String, Object>> documents) {
		return es.bulkIndex(index, documents);
	}

}

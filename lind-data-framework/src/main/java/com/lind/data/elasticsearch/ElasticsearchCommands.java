package com.lind.data.elasticsearch;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Elasticsearch 命令抽象，便于场景封装与单测替换。
 */
public interface ElasticsearchCommands extends AutoCloseable {

	boolean createIndex(String index);

	boolean deleteIndex(String index);

	boolean indexExists(String index);

	String index(String index, String id, Map<String, Object> document);

	Optional<Map<String, Object>> get(String index, String id);

	boolean update(String index, String id, Map<String, Object> partial);

	boolean delete(String index, String id);

	EsSearchResult search(String index, String field, String text, int from, int size);

	EsSearchResult multiMatch(String index, String text, List<String> fields, int from, int size);

	EsSearchResult geoDistance(String index, String geoField, double lat, double lon, String distance, int size);

	Map<String, Long> termsAgg(String index, String field, int size);

	int bulkIndex(String index, List<Map<String, Object>> documents);

	@Override
	void close();

}

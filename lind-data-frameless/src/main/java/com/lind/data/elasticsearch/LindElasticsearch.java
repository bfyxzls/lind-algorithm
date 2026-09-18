package com.lind.data.elasticsearch;

import java.util.Objects;

/**
 * Elasticsearch 场景门面：基于官方 Java API Client。
 *
 * <pre>
 * try (LindElasticsearch es = LindElasticsearch.connect("127.0.0.1", 9200)) {
 *     es.documents("products").save("1", Map.of("title", "phone"));
 *     es.search("products").match("title", "phone", 1, 10);
 * }
 * </pre>
 */
public final class LindElasticsearch implements AutoCloseable {

	private final ElasticsearchCommands commands;

	public LindElasticsearch(ElasticsearchCommands commands) {
		this.commands = Objects.requireNonNull(commands, "commands");
	}

	public static LindElasticsearch connect(String host, int port) {
		return new LindElasticsearch(JavaApiElasticsearchCommands.connect(host, port));
	}

	public ElasticsearchCommands commands() {
		return commands;
	}

	public EsDocuments documents(String index) {
		return new EsDocuments(commands, index);
	}

	public EsSearch search(String index) {
		return new EsSearch(commands, index);
	}

	public EsAggregation aggregation(String index) {
		return new EsAggregation(commands, index);
	}

	public EsGeoSearch geo(String index) {
		return new EsGeoSearch(commands, index);
	}

	public EsGeoSearch geo(String index, String geoField) {
		return new EsGeoSearch(commands, index, geoField);
	}

	@Override
	public void close() {
		commands.close();
	}

}

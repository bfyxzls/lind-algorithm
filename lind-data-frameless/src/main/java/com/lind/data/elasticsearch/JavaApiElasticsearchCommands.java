package com.lind.data.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;

/**
 * 基于官方 {@code elasticsearch-java} 的 {@link ElasticsearchCommands} 实现。
 */
public final class JavaApiElasticsearchCommands implements ElasticsearchCommands {

	private final RestClient restClient;

	private final ElasticsearchClient client;

	private final boolean closeOnClose;

	public JavaApiElasticsearchCommands(RestClient restClient, ElasticsearchClient client) {
		this(restClient, client, true);
	}

	public JavaApiElasticsearchCommands(RestClient restClient, ElasticsearchClient client, boolean closeOnClose) {
		this.restClient = Objects.requireNonNull(restClient, "restClient");
		this.client = Objects.requireNonNull(client, "client");
		this.closeOnClose = closeOnClose;
	}

	public static JavaApiElasticsearchCommands connect(String host, int port) {
		RestClient rest = RestClient.builder(new HttpHost(host, port)).build();
		RestClientTransport transport = new RestClientTransport(rest, new JacksonJsonpMapper(new ObjectMapper()));
		return new JavaApiElasticsearchCommands(rest, new ElasticsearchClient(transport));
	}

	@Override
	public boolean createIndex(String index) {
		try {
			if (indexExists(index)) {
				return false;
			}
			client.indices().create(c -> c.index(index));
			return true;
		}
		catch (IOException e) {
			throw new IllegalStateException("createIndex failed: " + index, e);
		}
	}

	@Override
	public boolean deleteIndex(String index) {
		try {
			if (!indexExists(index)) {
				return false;
			}
			client.indices().delete(d -> d.index(index));
			return true;
		}
		catch (IOException e) {
			throw new IllegalStateException("deleteIndex failed: " + index, e);
		}
	}

	@Override
	public boolean indexExists(String index) {
		try {
			return client.indices().exists(e -> e.index(index)).value();
		}
		catch (IOException e) {
			throw new IllegalStateException("indexExists failed: " + index, e);
		}
	}

	@Override
	public String index(String index, String id, Map<String, Object> document) {
		try {
			var response = client.index(i -> {
				var builder = i.index(index).document(document);
				if (id != null && !id.isBlank()) {
					builder = builder.id(id);
				}
				return builder;
			});
			return response.id();
		}
		catch (IOException e) {
			throw new IllegalStateException("index failed", e);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Optional<Map<String, Object>> get(String index, String id) {
		try {
			GetResponse<Map> response = client.get(g -> g.index(index).id(id), Map.class);
			if (!response.found() || response.source() == null) {
				return Optional.empty();
			}
			return Optional.of(response.source());
		}
		catch (IOException e) {
			throw new IllegalStateException("get failed", e);
		}
	}

	@Override
	public boolean update(String index, String id, Map<String, Object> partial) {
		try {
			client.update(u -> u.index(index).id(id).doc(partial), Map.class);
			return true;
		}
		catch (IOException e) {
			throw new IllegalStateException("update failed", e);
		}
	}

	@Override
	public boolean delete(String index, String id) {
		try {
			client.delete(d -> d.index(index).id(id));
			return true;
		}
		catch (IOException e) {
			throw new IllegalStateException("delete failed", e);
		}
	}

	@Override
	public EsSearchResult search(String index, String field, String text, int from, int size) {
		try {
			SearchResponse<Map> response = client.search(
					s -> s.index(index).from(from).size(size).query(q -> q.match(m -> m.field(field).query(text))),
					Map.class);
			return toResult(response);
		}
		catch (IOException e) {
			throw new IllegalStateException("search failed", e);
		}
	}

	@Override
	public EsSearchResult multiMatch(String index, String text, List<String> fields, int from, int size) {
		try {
			SearchResponse<Map> response = client.search(s -> s.index(index).from(from).size(size)
					.query(q -> q.multiMatch(m -> m.query(text).fields(fields))), Map.class);
			return toResult(response);
		}
		catch (IOException e) {
			throw new IllegalStateException("multiMatch failed", e);
		}
	}

	@Override
	public EsSearchResult geoDistance(String index, String geoField, double lat, double lon, String distance,
			int size) {
		try {
			SearchResponse<Map> response = client.search(s -> s.index(index).size(size).query(q -> q.geoDistance(
					g -> g.field(geoField).distance(distance).location(l -> l.latlon(ll -> ll.lat(lat).lon(lon))))),
					Map.class);
			return toResult(response);
		}
		catch (IOException e) {
			throw new IllegalStateException("geoDistance failed", e);
		}
	}

	@Override
	public Map<String, Long> termsAgg(String index, String field, int size) {
		try {
			SearchResponse<Map> response = client.search(
					s -> s.index(index).size(0).aggregations("by_field", a -> a.terms(t -> t.field(field).size(size))),
					Map.class);
			Aggregate aggregate = response.aggregations().get("by_field");
			Map<String, Long> result = new LinkedHashMap<>();
			if (aggregate != null && aggregate.isSterms()) {
				for (StringTermsBucket bucket : aggregate.sterms().buckets().array()) {
					FieldValue key = bucket.key();
					result.put(key.stringValue(), bucket.docCount());
				}
			}
			return result;
		}
		catch (IOException e) {
			throw new IllegalStateException("termsAgg failed", e);
		}
	}

	@Override
	public int bulkIndex(String index, List<Map<String, Object>> documents) {
		Objects.requireNonNull(documents, "documents");
		if (documents.isEmpty()) {
			return 0;
		}
		try {
			List<BulkOperation> ops = new ArrayList<>(documents.size());
			for (Map<String, Object> doc : documents) {
				ops.add(BulkOperation.of(b -> b.index(i -> i.index(index).document(doc))));
			}
			var response = client.bulk(BulkRequest.of(b -> b.operations(ops)));
			long success = response.items().stream().filter(item -> item.error() == null).count();
			return (int) success;
		}
		catch (IOException e) {
			throw new IllegalStateException("bulkIndex failed", e);
		}
	}

	@SuppressWarnings("unchecked")
	private static EsSearchResult toResult(SearchResponse<Map> response) {
		long total = response.hits().total() == null ? 0 : response.hits().total().value();
		List<Map<String, Object>> hits = new ArrayList<>();
		for (Hit<Map> hit : response.hits().hits()) {
			if (hit.source() != null) {
				Map<String, Object> source = new LinkedHashMap<>(hit.source());
				source.put("_id", hit.id());
				hits.add(source);
			}
		}
		return new EsSearchResult(total, hits);
	}

	@Override
	public void close() {
		if (closeOnClose) {
			try {
				restClient.close();
			}
			catch (IOException e) {
				throw new IllegalStateException("close RestClient failed", e);
			}
		}
	}

}

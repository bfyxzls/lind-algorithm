package com.lind.architecture;

import com.lind.architecture.api.ToyAsyncClient;
import com.lind.architecture.api.ToyClient;
import com.lind.architecture.api.documents.GetResponse;
import com.lind.architecture.api.documents.IndexResponse;
import com.lind.architecture.api.search.SearchResponse;
import com.lind.architecture.api.search.query.Query;
import com.lind.architecture.codegen.ApiSpec;
import com.lind.architecture.codegen.SpecDrivenEndpoints;
import com.lind.architecture.json.JacksonJsonMapper;
import com.lind.architecture.json.JsonMapper;
import com.lind.architecture.json.SimpleJsonMapper;
import com.lind.architecture.transport.Endpoint;
import com.lind.architecture.transport.HttpMethod;
import com.lind.architecture.transport.InMemoryTransport;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 对照 docs/elasticsearch-java的学习.md §3：七个架构级知识点的可运行断言。
 */
class ToyDocSdkTest {

	@Test
	void fluentBuilder_and_namespaces_roundTrip() {
		try (ToyClient client = ToyClient.inMemory()) {
			IndexResponse indexed = client.documents()
					.index(i -> i.index("orders").id("1").document(Map.of("status", "OPEN", "title", "hello world")));
			assertThat(indexed.result()).isEqualTo("created");

			GetResponse got = client.documents().get(g -> g.index("orders").id("1"));
			assertThat(got.found()).isTrue();
			assertThat(got.source()).containsEntry("status", "OPEN");

			SearchResponse found = client.search(s -> s.index("orders")
					.query(q -> q.bool(b -> b.must(m -> m.term(t -> t.field("status").value("OPEN")))
							.must(m -> m.match(mt -> mt.field("title").query("hello"))))));
			assertThat(found.hits().total()).isEqualTo(1);
			assertThat(found.hits().hits().get(0).id()).isEqualTo("1");
		}
	}

	@Test
	void transport_is_single_kernel_for_all_apis() {
		InMemoryTransport transport = new InMemoryTransport();
		try (ToyClient client = new ToyClient(transport, new JacksonJsonMapper())) {
			client.documents().index(i -> i.index("a").id("1").document(Map.of("x", 1)));
			client.search(s -> s.index("a").query(q -> q.term(t -> t.field("x").value("1"))));
		}
		assertThat(transport.calls()).extracting(InMemoryTransport.Call::endpointId).containsExactly("documents.index",
				"search");
		assertThat(transport.calls()).extracting(InMemoryTransport.Call::method).containsExactly(HttpMethod.PUT,
				HttpMethod.POST);
	}

	@Test
	void jsonMapper_is_swappable_for_request_serialization() throws Exception {
		Query query = Query.of(q -> q.term(t -> t.field("status").value("OPEN")));
		Object tree = query.toJsonTree();

		JsonMapper jackson = new JacksonJsonMapper();
		JsonMapper simple = new SimpleJsonMapper();

		String j1 = jackson.writeToString(tree);
		String j2 = simple.writeToString(tree);

		assertThat(j1).contains("\"term\"").contains("\"status\"").contains("\"OPEN\"");
		assertThat(j2).contains("\"term\"").contains("\"status\"").contains("\"OPEN\"");

		@SuppressWarnings("unchecked")
		Map<String, Object> back = simple.read(j2, Map.class);
		assertThat(back).containsKey("term");
	}

	@Test
	void variant_query_serializes_as_tagged_json() {
		Query term = Query.of(q -> q.term(t -> t.field("status").value("OPEN")));
		Query match = Query.of(q -> q.match(m -> m.field("title").query("hello")));
		Query bool = Query.of(q -> q.bool(b -> b.must(term).must(match)));

		assertThat(term.kind()).isEqualTo(Query.Kind.TERM);
		assertThat(match.kind()).isEqualTo(Query.Kind.MATCH);
		assertThat(bool.kind()).isEqualTo(Query.Kind.BOOL);

		@SuppressWarnings("unchecked")
		Map<String, Object> tree = (Map<String, Object>) bool.toJsonTree();
		assertThat(tree).containsOnlyKeys("bool");
		@SuppressWarnings("unchecked")
		Map<String, Object> boolNode = (Map<String, Object>) tree.get("bool");
		assertThat(boolNode.get("must")).asList().hasSize(2);
	}

	@Test
	void spec_driven_endpoint_renders_path_without_handwritten_http() throws Exception {
		ApiSpec spec = new ApiSpec("cluster.health", HttpMethod.GET, "/_cluster/health/{level}", false, Map.class);
		Endpoint<Map<String, String>, Map> endpoint = SpecDrivenEndpoints.fromSpec(spec, req -> req);

		assertThat(endpoint.path(Map.of("level", "cluster"))).isEqualTo("/_cluster/health/cluster");
		assertThat(endpoint.id()).isEqualTo("cluster.health");

		InMemoryTransport transport = new InMemoryTransport();
		@SuppressWarnings("unchecked")
		Map<String, Object> res = transport.performRequest(endpoint, Map.of("level", "cluster"),
				new JacksonJsonMapper());
		assertThat(res).containsEntry("status", "green");
	}

	@Test
	void async_client_shares_same_models() throws Exception {
		InMemoryTransport transport = new InMemoryTransport();
		ToyAsyncClient async = new ToyAsyncClient(transport, new JacksonJsonMapper());

		IndexResponse indexed = async
				.index(i -> i.index("orders").id("9").document(new LinkedHashMap<>(Map.of("status", "OPEN"))))
				.get(2, TimeUnit.SECONDS);
		assertThat(indexed.id()).isEqualTo("9");

		SearchResponse search = async
				.search(s -> s.index("orders").query(q -> q.term(t -> t.field("status").value("OPEN"))))
				.get(2, TimeUnit.SECONDS);
		assertThat(search.hits().total()).isEqualTo(1);
	}

}

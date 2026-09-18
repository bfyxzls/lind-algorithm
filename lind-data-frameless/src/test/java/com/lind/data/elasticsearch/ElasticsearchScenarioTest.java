package com.lind.data.elasticsearch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Elasticsearch 场景封装单元测试（mock {@link ElasticsearchCommands}）。
 */
@ExtendWith(MockitoExtension.class)
class ElasticsearchScenarioTest {

	@Mock
	private ElasticsearchCommands es;

	@Test
	void documentsCrudAndBulk() {
		when(es.index(eq("products"), eq("1"), anyMap())).thenReturn("1");
		when(es.get("products", "1")).thenReturn(Optional.of(Map.of("title", "phone")));
		when(es.update(eq("products"), eq("1"), anyMap())).thenReturn(true);
		when(es.delete("products", "1")).thenReturn(true);
		when(es.bulkIndex(eq("products"), anyList())).thenReturn(2);

		EsDocuments docs = new EsDocuments(es, "products");
		docs.ensureIndex();
		verify(es).createIndex("products");
		assertEquals("1", docs.save("1", Map.of("title", "phone")));
		assertEquals("phone", docs.get("1").orElseThrow().get("title"));
		assertTrue(docs.update("1", Map.of("price", 99)));
		assertTrue(docs.delete("1"));
		assertEquals(2, docs.bulkSave(List.of(Map.of("a", 1), Map.of("b", 2))));
	}

	@Test
	void searchAggregationGeoAndFacade() {
		when(es.search(eq("products"), eq("title"), eq("phone"), eq(0), eq(10)))
				.thenReturn(new EsSearchResult(1, List.of(Map.of("title", "phone"))));
		when(es.multiMatch(eq("products"), eq("耳机"), anyList(), eq(0), eq(10)))
				.thenReturn(new EsSearchResult(1, List.of(Map.of("title", "耳机"))));
		when(es.termsAgg("products", "brand.keyword", 5)).thenReturn(Map.of("sony", 3L));
		when(es.geoDistance(eq("shops"), eq("location"), anyDouble(), anyDouble(), eq("5km"), eq(20)))
				.thenReturn(new EsSearchResult(1, List.of(Map.of("name", "store"))));

		assertEquals(1, new EsSearch(es, "products").match("title", "phone", 1, 10).hits().size());
		assertEquals(1, new EsSearch(es, "products").multiMatch("耳机", List.of("title", "desc"), 1, 10).total());
		assertEquals(3L, new EsAggregation(es, "products").terms("brand.keyword", 5).get("sony"));
		assertEquals(1, new EsGeoSearch(es, "shops").nearby(39.9, 116.4, "5km", 20).hits().size());

		LindElasticsearch lind = new LindElasticsearch(es);
		assertTrue(lind.documents("i") != null);
		assertTrue(lind.search("i") != null);
		assertTrue(lind.aggregation("i") != null);
		assertTrue(lind.geo("i") != null);
	}

}

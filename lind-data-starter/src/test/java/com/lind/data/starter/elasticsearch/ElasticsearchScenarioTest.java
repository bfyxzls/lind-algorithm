package com.lind.data.starter.elasticsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;

/**
 * Elasticsearch 场景单测（mock {@link ElasticsearchOperations}，无需本机 ES）。
 */
@ExtendWith(MockitoExtension.class)
class ElasticsearchScenarioTest {

	@Mock
	private ElasticsearchOperations operations;

	@Test
	void facadeExposesDocuments() {
		LindSpringElasticsearch es = new LindSpringElasticsearch(operations);
		assertThat(es.operations()).isSameAs(operations);
		assertThat(es.documents()).isNotNull();
	}

	@Test
	void saveGetDelete() {
		Product product = new Product("1", "无线耳机");
		when(operations.save(product)).thenReturn(product);
		when(operations.get("1", Product.class)).thenReturn(product);
		when(operations.delete("1", Product.class)).thenReturn("1");

		SpringEsDocuments docs = new SpringEsDocuments(operations);
		assertThat(docs.save(product)).isSameAs(product);
		assertThat(docs.get("1", Product.class)).contains(product);
		assertThat(docs.delete("1", Product.class)).isEqualTo("1");
	}

	@Test
	void getMissingReturnsEmpty() {
		when(operations.get("x", Product.class)).thenReturn(null);
		assertThat(new SpringEsDocuments(operations).get("x", Product.class)).isEmpty();
	}

	@Test
	void matchAndSearch() {
		@SuppressWarnings("unchecked")
		SearchHits<Product> hits = mock(SearchHits.class);
		when(operations.search(any(Query.class), eq(Product.class))).thenReturn(hits);

		SpringEsDocuments docs = new SpringEsDocuments(operations);
		assertThat(docs.match("title", "耳机", Product.class)).isSameAs(hits);

		ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
		verify(operations).search(queryCaptor.capture(), eq(Product.class));
		assertThat(queryCaptor.getValue()).isInstanceOf(CriteriaQuery.class);

		assertThat(docs.search(new CriteriaQuery(Criteria.where("title").contains("无线")), Product.class))
				.isSameAs(hits);
	}

	@Test
	void indexExists() {
		IndexOperations indexOps = mock(IndexOperations.class);
		when(operations.indexOps(any(IndexCoordinates.class))).thenReturn(indexOps);
		when(indexOps.exists()).thenReturn(true);

		assertThat(new SpringEsDocuments(operations).indexExists("products")).isTrue();
		verify(operations).indexOps(IndexCoordinates.of("products"));
	}

	static class Product {

		private String id;

		private String title;

		Product(String id, String title) {
			this.id = id;
			this.title = title;
		}

		public String getId() {
			return id;
		}

		public String getTitle() {
			return title;
		}

	}

}

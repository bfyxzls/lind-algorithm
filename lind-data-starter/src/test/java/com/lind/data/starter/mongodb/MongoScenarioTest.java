package com.lind.data.starter.mongodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mongodb.client.result.DeleteResult;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * MongoDB 场景单测（mock {@link MongoTemplate}，无需本机 MongoDB）。
 */
@ExtendWith(MockitoExtension.class)
class MongoScenarioTest {

	@Mock
	private MongoTemplate mongoTemplate;

	@Test
	void facadeExposesDocumentsQueryAggregation() {
		LindSpringMongo mongo = new LindSpringMongo(mongoTemplate);
		assertThat(mongo.template()).isSameAs(mongoTemplate);
		assertThat(mongo.documents()).isNotNull();
		assertThat(mongo.query()).isNotNull();
		assertThat(mongo.aggregation()).isNotNull();
	}

	@Test
	void insertFindSaveRemove() {
		User user = new User("1", "lind");
		when(mongoTemplate.insert(user)).thenReturn(user);
		when(mongoTemplate.findById("1", User.class)).thenReturn(user);
		when(mongoTemplate.find(any(Query.class), eq(User.class))).thenReturn(List.of(user));
		when(mongoTemplate.save(user)).thenReturn(user);
		DeleteResult deleteResult = DeleteResult.acknowledged(1);
		when(mongoTemplate.remove(any(Query.class), eq(User.class))).thenReturn(deleteResult);

		SpringMongoDocuments docs = new SpringMongoDocuments(mongoTemplate);
		assertThat(docs.insert(user)).isSameAs(user);
		assertThat(docs.findById("1", User.class)).contains(user);
		assertThat(docs.find(Query.query(Criteria.where("name").is("lind")), User.class)).hasSize(1);
		assertThat(docs.save(user)).isSameAs(user);
		assertThat(docs.remove(Query.query(Criteria.where("name").is("lind")), User.class)).isEqualTo(1L);
	}

	@Test
	void findByIdEmpty() {
		when(mongoTemplate.findById("missing", User.class)).thenReturn(null);
		assertThat(new SpringMongoDocuments(mongoTemplate).findById("missing", User.class)).isEmpty();
	}

	@Test
	void nextSequenceUsesCountersUpsert() {
		SpringMongoDocuments.Counter counter = new SpringMongoDocuments.Counter();
		counter.setId("order");
		counter.setSeq(1001L);
		when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class),
				eq(SpringMongoDocuments.Counter.class), eq("counters"))).thenReturn(counter);

		long seq = new SpringMongoDocuments(mongoTemplate).nextSequence("order");
		assertThat(seq).isEqualTo(1001L);

		ArgumentCaptor<FindAndModifyOptions> optionsCaptor = ArgumentCaptor.forClass(FindAndModifyOptions.class);
		verify(mongoTemplate).findAndModify(any(Query.class), any(Update.class), optionsCaptor.capture(),
				eq(SpringMongoDocuments.Counter.class), eq("counters"));
		assertThat(optionsCaptor.getValue().isReturnNew()).isTrue();
		assertThat(optionsCaptor.getValue().isUpsert()).isTrue();
	}

	@Test
	void multiConditionFindAndOrAndCount() {
		Order order = new Order("1", "PAID", 200);
		when(mongoTemplate.find(any(Query.class), eq(Order.class))).thenReturn(List.of(order));
		when(mongoTemplate.count(any(Query.class), eq(Order.class))).thenReturn(1L);

		SpringMongoQuery query = new SpringMongoQuery(mongoTemplate);
		List<Order> andResult = query.findAnd(Order.class, 1, 20, Sort.by(Sort.Direction.DESC, "amount"),
				Criteria.where("status").is("PAID"), Criteria.where("amount").gte(100));
		assertThat(andResult).containsExactly(order);

		assertThat(query.findOr(Order.class, 1, 10, Criteria.where("status").is("PAID"),
				Criteria.where("status").is("REFUND"))).containsExactly(order);

		assertThat(query.findByEquals(Map.of("userId", "u1", "status", "PAID"), Order.class, 1, 20))
				.containsExactly(order);

		assertThat(query.countAnd(Order.class, Criteria.where("status").is("PAID"), Criteria.where("amount").gte(100)))
				.isEqualTo(1L);

		ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
		verify(mongoTemplate, org.mockito.Mockito.atLeastOnce()).find(queryCaptor.capture(), eq(Order.class));
		Query first = queryCaptor.getAllValues().get(0);
		assertThat(first.getSkip()).isZero();
		assertThat(first.getLimit()).isEqualTo(20);
	}

	@Test
	void queryRejectsEmptyConditions() {
		SpringMongoQuery query = new SpringMongoQuery(mongoTemplate);
		assertThatThrownBy(() -> SpringMongoQuery.and()).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> query.findByEquals(Map.of(), Order.class, 1, 10))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> query.find(Criteria.where("a").is(1), Order.class, 0, 10))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void aggregationGroupCountSumAvg() {
		Document bucket = new Document("_id", "PAID").append("count", 3);
		@SuppressWarnings("unchecked")
		AggregationResults<Document> results = org.mockito.Mockito.mock(AggregationResults.class);
		when(results.getMappedResults()).thenReturn(List.of(bucket));
		when(mongoTemplate.aggregate(any(Aggregation.class), eq("orders"), eq(Document.class))).thenReturn(results);
		when(mongoTemplate.aggregate(any(Aggregation.class), eq(Order.class), eq(Document.class))).thenReturn(results);

		SpringMongoAggregation aggregation = new SpringMongoAggregation(mongoTemplate);
		assertThat(aggregation.groupCount("orders", "status")).containsExactly(bucket);
		assertThat(aggregation.groupCount(Order.class, "status")).containsExactly(bucket);
		assertThat(aggregation.sum("orders", "amount")).containsExactly(bucket);
		assertThat(aggregation.avg("orders", "amount")).containsExactly(bucket);

		Aggregation custom = Aggregation.newAggregation(Aggregation.group("userId").sum("amount").as("total"));
		assertThat(aggregation.aggregate(custom, "orders", Document.class)).containsExactly(bucket);
	}

	static class User {

		private String id;

		private String name;

		User(String id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getId() {
			return id;
		}

		public String getName() {
			return name;
		}

	}

	static class Order {

		private String id;

		private String status;

		private int amount;

		Order(String id, String status, int amount) {
			this.id = id;
			this.status = status;
			this.amount = amount;
		}

		public String getId() {
			return id;
		}

		public String getStatus() {
			return status;
		}

		public int getAmount() {
			return amount;
		}

	}

}

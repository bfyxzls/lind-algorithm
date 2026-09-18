package com.lind.data.starter.mongodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mongodb.client.result.DeleteResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
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
	void facadeExposesDocuments() {
		LindSpringMongo mongo = new LindSpringMongo(mongoTemplate);
		assertThat(mongo.template()).isSameAs(mongoTemplate);
		assertThat(mongo.documents()).isNotNull();
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

		ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
		ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
		ArgumentCaptor<FindAndModifyOptions> optionsCaptor = ArgumentCaptor.forClass(FindAndModifyOptions.class);
		verify(mongoTemplate).findAndModify(queryCaptor.capture(), updateCaptor.capture(), optionsCaptor.capture(),
				eq(SpringMongoDocuments.Counter.class), eq("counters"));
		assertThat(optionsCaptor.getValue().isReturnNew()).isTrue();
		assertThat(optionsCaptor.getValue().isUpsert()).isTrue();
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

}

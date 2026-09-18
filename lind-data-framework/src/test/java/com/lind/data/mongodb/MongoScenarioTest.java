package com.lind.data.mongodb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * MongoDB 场景封装单元测试（mock {@link MongoCommands}）。
 */
@ExtendWith(MockitoExtension.class)
class MongoScenarioTest {

	@Mock
	private MongoCommands mongo;

	@Test
	void documentsCrud() {
		when(mongo.insert(eq("users"), any(Document.class))).thenReturn("1");
		when(mongo.findById("users", "1")).thenReturn(Optional.of(new Document("name", "lind")));
		when(mongo.updateById(eq("users"), eq("1"), any(Document.class))).thenReturn(1L);
		when(mongo.deleteById("users", "1")).thenReturn(1L);

		MongoDocuments docs = new MongoDocuments(mongo, "users");
		assertEquals("1", docs.insert(new Document("name", "lind")));
		assertEquals("lind", docs.findById("1").orElseThrow().getString("name"));
		assertEquals(1L, docs.updateById("1", new Document("name", "x")));
		assertEquals(1L, docs.deleteById("1"));
	}

	@Test
	void queryAndAggregation() {
		when(mongo.find(eq("orders"), any(Bson.class), eq(0), eq(10), any()))
				.thenReturn(List.of(new Document("id", 1)));
		when(mongo.aggregate(eq("orders"), anyList()))
				.thenReturn(List.of(new Document("_id", "PAID").append("count", 3)));

		assertEquals(1, new MongoQuery(mongo, "orders").find(new Document("status", "PAID"), 1, 10).size());
		assertEquals(3, new MongoAggregation(mongo, "orders").groupCount("status").get(0).getInteger("count"));
	}

	@Test
	void geoTtlTextSequenceAndFacade() {
		when(mongo.insert(eq("shops"), any(Document.class))).thenReturn("s1");
		when(mongo.find(eq("shops"), any(Bson.class), eq(0), eq(5), any()))
				.thenReturn(List.of(new Document("name", "a")));
		MongoGeo geo = new MongoGeo(mongo, "shops");
		geo.ensureGeoIndex();
		verify(mongo).createIndex(eq("shops"), any(Bson.class), eq(false));
		assertEquals("s1", geo.insertPoint(new Document("name", "a"), 116.4, 39.9));
		assertEquals(1, geo.near(116.4, 39.9, 1000, 5).size());

		when(mongo.insert(eq("sessions"), any(Document.class))).thenReturn("t1");
		assertEquals("t1",
				new MongoTtlStore(mongo, "sessions").put(new Document("uid", "u1"), Instant.now().plusSeconds(60)));

		when(mongo.aggregate(eq("articles"), anyList())).thenReturn(List.of(new Document("title", "hello")));
		assertEquals(1, new MongoTextSearch(mongo, "articles").search("hello", 10).size());

		when(mongo.findOneAndUpdate(anyString(), any(Bson.class), any(Bson.class), anyBoolean(), anyBoolean()))
				.thenReturn(new Document("value", 100L));
		assertEquals(100L, new MongoSequence(mongo, "counters").next("order"));

		LindMongo lind = new LindMongo(mongo);
		assertTrue(lind.documents("c") != null);
		assertTrue(lind.query("c") != null);
		assertTrue(lind.aggregation("c") != null);
		assertTrue(lind.geo("c") != null);
		assertTrue(lind.ttl("c") != null);
		assertTrue(lind.textSearch("c") != null);
		assertTrue(lind.sequence("c") != null);
	}

}

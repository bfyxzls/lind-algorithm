package com.lind.data.mongodb;

import java.util.List;
import java.util.Objects;
import org.bson.Document;

/**
 * 地理位置：2dsphere + $near。
 */
public final class MongoGeo {

	private final MongoCommands mongo;

	private final String collection;

	private final String locationField;

	public MongoGeo(MongoCommands mongo, String collection) {
		this(mongo, collection, "location");
	}

	public MongoGeo(MongoCommands mongo, String collection, String locationField) {
		this.mongo = Objects.requireNonNull(mongo, "mongo");
		this.collection = Objects.requireNonNull(collection, "collection");
		this.locationField = Objects.requireNonNull(locationField, "locationField");
	}

	public void ensureGeoIndex() {
		mongo.createIndex(collection, new Document(locationField, "2dsphere"), false);
	}

	public String insertPoint(Document fields, double longitude, double latitude) {
		Document doc = new Document(Objects.requireNonNull(fields, "fields"));
		doc.put(locationField, new Document("type", "Point").append("coordinates", List.of(longitude, latitude)));
		return mongo.insert(collection, doc);
	}

	public List<Document> near(double longitude, double latitude, double maxDistanceMeters, int limit) {
		Document filter = new Document(locationField,
				new Document("$near",
						new Document("$geometry",
								new Document("type", "Point").append("coordinates", List.of(longitude, latitude)))
										.append("$maxDistance", maxDistanceMeters)));
		return mongo.find(collection, filter, 0, limit, null);
	}

}

package com.lind.data.elasticsearch;

import java.util.Objects;

/**
 * 地理位置距离检索。
 */
public final class EsGeoSearch {

	private final ElasticsearchCommands es;

	private final String index;

	private final String geoField;

	public EsGeoSearch(ElasticsearchCommands es, String index) {
		this(es, index, "location");
	}

	public EsGeoSearch(ElasticsearchCommands es, String index, String geoField) {
		this.es = Objects.requireNonNull(es, "es");
		this.index = Objects.requireNonNull(index, "index");
		this.geoField = Objects.requireNonNull(geoField, "geoField");
	}

	public EsSearchResult nearby(double lat, double lon, String distance, int size) {
		return es.geoDistance(index, geoField, lat, lon, Objects.requireNonNull(distance, "distance"), size);
	}

}

package com.lind.data.redis;

import java.util.List;
import java.util.Objects;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.resps.GeoRadiusResponse;

/**
 * 地理位置：GEOADD / GEORADIUS。
 */
public final class RedisGeo {

	private final RedisCommands redis;

	private final String key;

	public RedisGeo(RedisCommands redis, String key) {
		this.redis = Objects.requireNonNull(redis, "redis");
		this.key = Objects.requireNonNull(key, "key");
	}

	public boolean add(String member, double longitude, double latitude) {
		Objects.requireNonNull(member, "member");
		return redis.geoAdd(key, longitude, latitude, member) > 0;
	}

	public List<GeoRadiusResponse> radiusKm(double longitude, double latitude, double radiusKm) {
		return redis.geoRadius(key, longitude, latitude, radiusKm, GeoUnit.KM,
				GeoRadiusParam.geoRadiusParam().withDist().withCoord().sortAscending());
	}

}

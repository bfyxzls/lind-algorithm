package com.lind.data.redis;

import java.util.Objects;

/**
 * HyperLogLog：近似 UV / 基数统计。
 */
public final class RedisHyperLogLog {

	private final RedisCommands redis;

	private final String key;

	public RedisHyperLogLog(RedisCommands redis, String key) {
		this.redis = Objects.requireNonNull(redis, "redis");
		this.key = Objects.requireNonNull(key, "key");
	}

	public boolean add(String element) {
		return redis.pfAdd(key, Objects.requireNonNull(element, "element")) > 0;
	}

	public long addAll(String... elements) {
		Objects.requireNonNull(elements, "elements");
		return redis.pfAdd(key, elements);
	}

	public long count() {
		return redis.pfCount(key);
	}

}

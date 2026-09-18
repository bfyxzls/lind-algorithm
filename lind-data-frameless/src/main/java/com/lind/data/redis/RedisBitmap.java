package com.lind.data.redis;

import java.util.Objects;

/**
 * Redis 位图：签到、开关标记等。
 */
public final class RedisBitmap {

	private final RedisCommands redis;

	private final String key;

	public RedisBitmap(RedisCommands redis, String key) {
		this.redis = Objects.requireNonNull(redis, "redis");
		this.key = Objects.requireNonNull(key, "key");
	}

	public boolean set(long offset, boolean value) {
		if (offset < 0) {
			throw new IllegalArgumentException("offset must be >= 0");
		}
		return redis.setBit(key, offset, value);
	}

	public boolean get(long offset) {
		if (offset < 0) {
			throw new IllegalArgumentException("offset must be >= 0");
		}
		return redis.getBit(key, offset);
	}

	public long cardinality() {
		return redis.bitCount(key);
	}

}

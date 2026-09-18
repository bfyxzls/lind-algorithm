package com.lind.data.redis;

import java.util.Objects;

/**
 * 分布式 ID / 全局序列：INCR / INCRBY。
 */
public final class RedisIdGenerator {

	private final RedisCommands redis;

	private final String key;

	public RedisIdGenerator(RedisCommands redis, String key) {
		this.redis = Objects.requireNonNull(redis, "redis");
		this.key = Objects.requireNonNull(key, "key");
		if (key.isBlank()) {
			throw new IllegalArgumentException("key must not be blank");
		}
	}

	public long nextId() {
		return redis.incr(key);
	}

	public long nextId(long step) {
		if (step <= 0) {
			throw new IllegalArgumentException("step must be > 0");
		}
		return redis.incrBy(key, step);
	}

	public long current() {
		String value = redis.get(key);
		return value == null ? 0L : Long.parseLong(value);
	}

}

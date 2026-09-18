package com.lind.data.redis;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import redis.clients.jedis.params.SetParams;

/**
 * 分布式缓存：String KV + TTL。
 */
public final class RedisCache {

	private final RedisCommands redis;

	public RedisCache(RedisCommands redis) {
		this.redis = Objects.requireNonNull(redis, "redis");
	}

	public void set(String key, String value) {
		redis.set(requireKey(key), Objects.requireNonNull(value, "value"));
	}

	public void set(String key, String value, Duration ttl) {
		Objects.requireNonNull(ttl, "ttl");
		if (ttl.isNegative() || ttl.isZero()) {
			throw new IllegalArgumentException("ttl must be > 0");
		}
		redis.set(requireKey(key), Objects.requireNonNull(value, "value"), SetParams.setParams().ex(ttl.toSeconds()));
	}

	public Optional<String> get(String key) {
		return Optional.ofNullable(redis.get(requireKey(key)));
	}

	public boolean delete(String key) {
		return redis.del(requireKey(key)) > 0;
	}

	public boolean exists(String key) {
		return redis.exists(requireKey(key));
	}

	private static String requireKey(String key) {
		Objects.requireNonNull(key, "key");
		if (key.isBlank()) {
			throw new IllegalArgumentException("key must not be blank");
		}
		return key;
	}

}

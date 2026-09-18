package com.lind.data.starter.redis;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 基于 {@link StringRedisTemplate} 的分布式缓存。
 */
public class SpringRedisCache {

	private final StringRedisTemplate redis;

	public SpringRedisCache(StringRedisTemplate redis) {
		this.redis = Objects.requireNonNull(redis, "redis");
	}

	public void set(String key, String value) {
		redis.opsForValue().set(requireKey(key), Objects.requireNonNull(value, "value"));
	}

	public void set(String key, String value, Duration ttl) {
		Objects.requireNonNull(ttl, "ttl");
		redis.opsForValue().set(requireKey(key), Objects.requireNonNull(value, "value"), ttl);
	}

	public Optional<String> get(String key) {
		return Optional.ofNullable(redis.opsForValue().get(requireKey(key)));
	}

	public boolean delete(String key) {
		Boolean deleted = redis.delete(requireKey(key));
		return Boolean.TRUE.equals(deleted);
	}

	private static String requireKey(String key) {
		Objects.requireNonNull(key, "key");
		if (key.isBlank()) {
			throw new IllegalArgumentException("key must not be blank");
		}
		return key;
	}

}

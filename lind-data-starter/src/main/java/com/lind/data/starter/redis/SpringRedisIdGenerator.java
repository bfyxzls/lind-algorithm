package com.lind.data.starter.redis;

import java.util.Objects;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 分布式 ID：INCR。
 */
public class SpringRedisIdGenerator {

	private final StringRedisTemplate redis;

	private final String key;

	public SpringRedisIdGenerator(StringRedisTemplate redis, String key) {
		this.redis = Objects.requireNonNull(redis, "redis");
		this.key = Objects.requireNonNull(key, "key");
	}

	public long nextId() {
		Long id = redis.opsForValue().increment(key);
		return id == null ? 0L : id;
	}

	public long nextId(long step) {
		if (step <= 0) {
			throw new IllegalArgumentException("step must be > 0");
		}
		Long id = redis.opsForValue().increment(key, step);
		return id == null ? 0L : id;
	}

}

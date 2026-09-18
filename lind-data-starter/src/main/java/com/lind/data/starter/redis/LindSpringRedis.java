package com.lind.data.starter.redis;

import java.time.Duration;
import java.util.Objects;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 场景门面（Spring Data Redis）。
 */
public class LindSpringRedis {

	private final StringRedisTemplate redis;

	public LindSpringRedis(StringRedisTemplate redis) {
		this.redis = Objects.requireNonNull(redis, "redis");
	}

	public StringRedisTemplate template() {
		return redis;
	}

	public SpringRedisCache cache() {
		return new SpringRedisCache(redis);
	}

	public SpringRedisLock lock(String key, Duration lease) {
		return new SpringRedisLock(redis, key, lease);
	}

	public SpringRedisRateLimiter rateLimiter(String key, int limit, Duration window) {
		return new SpringRedisRateLimiter(redis, key, limit, window);
	}

	public SpringRedisDelayQueue delayQueue(String key) {
		return new SpringRedisDelayQueue(redis, key);
	}

	public SpringRedisIdGenerator idGenerator(String key) {
		return new SpringRedisIdGenerator(redis, key);
	}

}

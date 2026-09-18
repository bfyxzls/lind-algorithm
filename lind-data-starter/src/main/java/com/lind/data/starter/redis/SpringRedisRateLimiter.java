package com.lind.data.starter.redis;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 滑动窗口限流（ZSET score=时间戳）。
 */
public class SpringRedisRateLimiter {

	private final StringRedisTemplate redis;

	private final String key;

	private final int limit;

	private final long windowMillis;

	public SpringRedisRateLimiter(StringRedisTemplate redis, String key, int limit, Duration window) {
		this.redis = Objects.requireNonNull(redis, "redis");
		this.key = Objects.requireNonNull(key, "key");
		if (limit <= 0) {
			throw new IllegalArgumentException("limit must be > 0");
		}
		Objects.requireNonNull(window, "window");
		this.limit = limit;
		this.windowMillis = window.toMillis();
	}

	public boolean tryAcquire() {
		return tryAcquire(1);
	}

	public boolean tryAcquire(int permits) {
		if (permits <= 0) {
			throw new IllegalArgumentException("permits must be > 0");
		}
		long now = System.currentTimeMillis();
		long min = now - windowMillis;
		redis.opsForZSet().removeRangeByScore(key, 0, min);
		Long count = redis.opsForZSet().zCard(key);
		long current = count == null ? 0 : count;
		if (current + permits > limit) {
			return false;
		}
		for (int i = 0; i < permits; i++) {
			redis.opsForZSet().add(key, now + ":" + UUID.randomUUID(), now);
		}
		redis.expire(key, Math.max(1, windowMillis / 1000 + 1), TimeUnit.SECONDS);
		return true;
	}

}

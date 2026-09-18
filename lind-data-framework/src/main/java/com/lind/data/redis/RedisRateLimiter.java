package com.lind.data.redis;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * 分布式滑动窗口限流：ZSET score=时间戳，窗口外成员剔除后计数。
 */
public final class RedisRateLimiter {

	private final RedisCommands redis;

	private final String key;

	private final int limit;

	private final long windowMillis;

	public RedisRateLimiter(RedisCommands redis, String key, int limit, Duration window) {
		this.redis = Objects.requireNonNull(redis, "redis");
		this.key = Objects.requireNonNull(key, "key");
		if (key.isBlank()) {
			throw new IllegalArgumentException("key must not be blank");
		}
		if (limit <= 0) {
			throw new IllegalArgumentException("limit must be > 0");
		}
		Objects.requireNonNull(window, "window");
		if (window.isZero() || window.isNegative()) {
			throw new IllegalArgumentException("window must be > 0");
		}
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
		redis.zRemRangeByScore(key, 0, min);
		long count = redis.zCard(key);
		if (count + permits > limit) {
			return false;
		}
		for (int i = 0; i < permits; i++) {
			redis.zAdd(key, now, now + ":" + UUID.randomUUID());
		}
		redis.expire(key, Math.max(1, windowMillis / 1000 + 1));
		return true;
	}

}

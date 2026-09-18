package com.lind.data.starter.redis;

import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * 基于 {@link StringRedisTemplate} 的分布式锁（SET NX PX + Lua 解锁）。
 */
public class SpringRedisLock {

	private static final DefaultRedisScript<Long> UNLOCK = new DefaultRedisScript<>("""
			if redis.call('get', KEYS[1]) == ARGV[1] then
			  return redis.call('del', KEYS[1])
			else
			  return 0
			end
			""", Long.class);

	private final StringRedisTemplate redis;

	private final String key;

	private final String token;

	private final Duration lease;

	public SpringRedisLock(StringRedisTemplate redis, String key, Duration lease) {
		this.redis = Objects.requireNonNull(redis, "redis");
		this.key = Objects.requireNonNull(key, "key");
		if (key.isBlank()) {
			throw new IllegalArgumentException("key must not be blank");
		}
		Objects.requireNonNull(lease, "lease");
		if (lease.isZero() || lease.isNegative()) {
			throw new IllegalArgumentException("lease must be > 0");
		}
		this.lease = lease;
		this.token = UUID.randomUUID().toString();
	}

	public boolean tryLock() {
		Boolean ok = redis.opsForValue().setIfAbsent(key, token, lease);
		return Boolean.TRUE.equals(ok);
	}

	public boolean unlock() {
		Long result = redis.execute(UNLOCK, Collections.singletonList(key), token);
		return result != null && result == 1L;
	}

}

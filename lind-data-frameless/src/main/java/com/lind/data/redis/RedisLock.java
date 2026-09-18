package com.lind.data.redis;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import redis.clients.jedis.params.SetParams;

/**
 * 分布式锁：SET NX PX + Lua 安全解锁（仅持有者可删）。
 */
public final class RedisLock {

	private static final String UNLOCK_SCRIPT = """
			if redis.call('get', KEYS[1]) == ARGV[1] then
			  return redis.call('del', KEYS[1])
			else
			  return 0
			end
			""";

	private final RedisCommands redis;

	private final String key;

	private final String token;

	private final Duration lease;

	public RedisLock(RedisCommands redis, String key, Duration lease) {
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
		String result = redis.set(key, token, SetParams.setParams().nx().px(lease.toMillis()));
		return "OK".equalsIgnoreCase(result);
	}

	public boolean unlock() {
		Object result = redis.eval(UNLOCK_SCRIPT, List.of(key), List.of(token));
		return result instanceof Long l && l == 1L;
	}

	public String key() {
		return key;
	}

	public String token() {
		return token;
	}

}

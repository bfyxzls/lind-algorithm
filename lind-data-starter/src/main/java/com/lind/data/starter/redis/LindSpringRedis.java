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

	/**
	 * 看门狗锁：默认租约 30s，持有期间自动续期（对齐 Redisson 未传 lease 行为）。
	 */
	public SpringRedisLock lock(String key) {
		return new SpringRedisLock(redis, key);
	}

	/**
	 * 固定租约锁：到期自动释放，不续期。
	 */
	public SpringRedisLock lock(String key, Duration lease) {
		return new SpringRedisLock(redis, key, lease);
	}

	/**
	 * @param watchdogEnabled {@code true} 时按 lease/3 自动续期
	 */
	public SpringRedisLock lock(String key, Duration lease, boolean watchdogEnabled) {
		return new SpringRedisLock(redis, key, lease, watchdogEnabled);
	}

	/**
	 * 看门狗锁 + 自动释放：拿不到锁返回 false，拿到则执行 action 后自动 unlock。
	 */
	public boolean tryRun(String key, Runnable action) {
		return lock(key).tryRun(action);
	}

	/**
	 * 看门狗锁 + 自动释放：拿不到锁抛异常。
	 */
	public void run(String key, Runnable action) {
		lock(key).run(action);
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

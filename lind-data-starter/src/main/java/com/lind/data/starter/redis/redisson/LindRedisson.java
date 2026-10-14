package com.lind.data.starter.redis.redisson;

import java.time.Duration;
import java.util.Objects;
import org.redisson.api.RedissonClient;

/**
 * Redisson 场景门面：分布式锁 + 延迟队列（延时任务）。
 */
public final class LindRedisson {

	private final RedissonClient client;

	public LindRedisson(RedissonClient client) {
		this.client = Objects.requireNonNull(client, "client");
	}

	public RedissonClient client() {
		return client;
	}

	/**
	 * 看门狗锁：持有期间自动续期。
	 */
	public RedissonDistributedLock lock(String key) {
		return new RedissonDistributedLock(client, key);
	}

	/**
	 * 固定租约锁：到期自动释放，不续期。
	 */
	public RedissonDistributedLock lock(String key, Duration lease) {
		return new RedissonDistributedLock(client, key, lease);
	}

	/**
	 * 看门狗锁 + 自动释放：拿不到锁返回 false。
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

	/**
	 * 延迟任务队列。同一 {@code name} 的生产端与消费端共享。
	 */
	public RedissonDelayTaskQueue delayQueue(String name) {
		return new RedissonDelayTaskQueue(client, name);
	}

}

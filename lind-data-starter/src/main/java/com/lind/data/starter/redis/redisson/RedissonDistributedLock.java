package com.lind.data.starter.redis.redisson;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

/**
 * Redisson 分布式锁封装，API 对齐 {@code SpringRedisLock}。
 * <ul>
 * <li>未指定 lease → {@link RLock#lock()}，看门狗自动续期</li>
 * <li>显式 lease → {@link RLock#lock(long, TimeUnit)}，到期释放不续期</li>
 * </ul>
 */
public final class RedissonDistributedLock implements AutoCloseable {

	private final RLock lock;

	private final Duration lease;

	private final boolean watchdogEnabled;

	private volatile boolean held;

	/**
	 * 看门狗模式。
	 */
	public RedissonDistributedLock(RedissonClient client, String key) {
		this(client, key, null, true);
	}

	/**
	 * 固定租约模式。
	 */
	public RedissonDistributedLock(RedissonClient client, String key, Duration lease) {
		this(client, key, lease, false);
	}

	RedissonDistributedLock(RedissonClient client, String key, Duration lease, boolean watchdogEnabled) {
		Objects.requireNonNull(client, "client");
		Objects.requireNonNull(key, "key");
		if (key.isBlank()) {
			throw new IllegalArgumentException("key must not be blank");
		}
		if (!watchdogEnabled) {
			Objects.requireNonNull(lease, "lease");
			if (lease.isZero() || lease.isNegative()) {
				throw new IllegalArgumentException("lease must be > 0");
			}
		}
		this.lock = client.getLock(key);
		this.lease = lease;
		this.watchdogEnabled = watchdogEnabled;
	}

	public void run(Runnable action) {
		Objects.requireNonNull(action, "action");
		if (!tryLock()) {
			throw new IllegalStateException("Failed to acquire lock: " + lock.getName());
		}
		try {
			action.run();
		}
		finally {
			unlock();
		}
	}

	public boolean tryRun(Runnable action) {
		Objects.requireNonNull(action, "action");
		if (!tryLock()) {
			return false;
		}
		try {
			action.run();
			return true;
		}
		finally {
			unlock();
		}
	}

	public <T> T call(Callable<T> action) throws Exception {
		Objects.requireNonNull(action, "action");
		if (!tryLock()) {
			throw new IllegalStateException("Failed to acquire lock: " + lock.getName());
		}
		try {
			return action.call();
		}
		finally {
			unlock();
		}
	}

	public <T> T supply(Supplier<T> action) {
		Objects.requireNonNull(action, "action");
		try {
			return call(action::get);
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public <T> Optional<T> tryCall(Callable<T> action) throws Exception {
		Objects.requireNonNull(action, "action");
		if (!tryLock()) {
			return Optional.empty();
		}
		try {
			return Optional.ofNullable(action.call());
		}
		finally {
			unlock();
		}
	}

	/**
	 * 立即尝试获取锁（不等待）。
	 */
	public boolean tryLock() {
		try {
			boolean ok;
			if (watchdogEnabled) {
				ok = lock.tryLock(0, -1, TimeUnit.MILLISECONDS);
			}
			else {
				ok = lock.tryLock(0, lease.toMillis(), TimeUnit.MILLISECONDS);
			}
			held = ok;
			return ok;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	/**
	 * 在 waitTime 内等待获取锁。
	 */
	public boolean tryLock(Duration waitTime) {
		Objects.requireNonNull(waitTime, "waitTime");
		try {
			boolean ok;
			if (watchdogEnabled) {
				ok = lock.tryLock(waitTime.toMillis(), -1, TimeUnit.MILLISECONDS);
			}
			else {
				ok = lock.tryLock(waitTime.toMillis(), lease.toMillis(), TimeUnit.MILLISECONDS);
			}
			held = ok;
			return ok;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	public boolean unlock() {
		if (!held && !lock.isHeldByCurrentThread()) {
			return false;
		}
		try {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
				held = false;
				return true;
			}
		}
		catch (IllegalMonitorStateException ignored) {
			// 未持有
		}
		held = false;
		return false;
	}

	@Override
	public void close() {
		unlock();
	}

	public boolean isLocked() {
		return held || lock.isHeldByCurrentThread();
	}

	public boolean isWatchdogEnabled() {
		return watchdogEnabled;
	}

	public String key() {
		return lock.getName();
	}

	public RLock raw() {
		return lock;
	}

}

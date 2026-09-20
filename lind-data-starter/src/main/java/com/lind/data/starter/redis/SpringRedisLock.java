package com.lind.data.starter.redis;

import java.lang.ref.Cleaner;
import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * 基于 {@link StringRedisTemplate} 的分布式锁（SET NX PX + Lua 解锁）。
 * <p>
 * 行为对齐 Redisson：
 * <ul>
 * <li>未指定 lease → 默认租约 30s，并启动看门狗按 {@code lease/3} 周期续期</li>
 * <li>显式指定 lease → 固定过期，不启动看门狗</li>
 * </ul>
 * 自动释放（减少对 finally 的依赖）：
 * <ul>
 * <li><b>推荐</b>：{@link #run(Runnable)} / {@link #tryRun(Runnable)} /
 * {@link #call(Callable)}，作用域结束自动 unlock</li>
 * <li>看门狗发现持有线程已死亡时自动停续期并释放（专用线程场景）</li>
 * <li>锁对象被 GC 且未 unlock 时，{@link Cleaner} 自动释放，避免看门狗无限续期</li>
 * </ul>
 */
public class SpringRedisLock implements AutoCloseable {

	/** 与 Redisson 默认 {@code lockWatchdogTimeout} 一致。 */
	public static final Duration DEFAULT_WATCHDOG_TIMEOUT = Duration.ofSeconds(30);

	private static final Cleaner CLEANER = Cleaner.create();

	private static final DefaultRedisScript<Long> UNLOCK = new DefaultRedisScript<>("""
			if redis.call('get', KEYS[1]) == ARGV[1] then
			  return redis.call('del', KEYS[1])
			else
			  return 0
			end
			""", Long.class);

	private static final DefaultRedisScript<Long> RENEW = new DefaultRedisScript<>("""
			if redis.call('get', KEYS[1]) == ARGV[1] then
			  return redis.call('pexpire', KEYS[1], ARGV[2])
			else
			  return 0
			end
			""", Long.class);

	private static final ScheduledExecutorService WATCHDOG_EXECUTOR = createWatchdogExecutor();

	private final StringRedisTemplate redis;

	private final String key;

	private final String token;

	private final Duration lease;

	private final boolean watchdogEnabled;

	private final Duration renewInterval;

	private final AtomicBoolean locked = new AtomicBoolean(false);

	private final AtomicReference<ScheduledFuture<?>> watchdogFuture = new AtomicReference<>();

	private final AtomicReference<Thread> ownerThread = new AtomicReference<>();

	/** 与本锁共享的清理状态；Cleaner 只持有它，不持有 SpringRedisLock 本身。 */
	private final ReleaseAction releaseAction;

	private volatile Cleaner.Cleanable cleanable;

	/**
	 * 看门狗模式：默认租约 30s，自动续期。
	 */
	public SpringRedisLock(StringRedisTemplate redis, String key) {
		this(redis, key, DEFAULT_WATCHDOG_TIMEOUT, true);
	}

	/**
	 * 固定租约模式：到达 lease 后自动释放，不续期。
	 */
	public SpringRedisLock(StringRedisTemplate redis, String key, Duration lease) {
		this(redis, key, lease, false);
	}

	/**
	 * @param watchdogEnabled {@code true} 时按 {@code lease/3} 续期（至少 1s）
	 */
	public SpringRedisLock(StringRedisTemplate redis, String key, Duration lease, boolean watchdogEnabled) {
		this(redis, key, lease, watchdogEnabled, renewIntervalOf(lease));
	}

	/**
	 * 测试 / 自定义续期间隔。
	 */
	SpringRedisLock(StringRedisTemplate redis, String key, Duration lease, boolean watchdogEnabled,
			Duration renewInterval) {
		this.redis = Objects.requireNonNull(redis, "redis");
		this.key = Objects.requireNonNull(key, "key");
		if (key.isBlank()) {
			throw new IllegalArgumentException("key must not be blank");
		}
		Objects.requireNonNull(lease, "lease");
		if (lease.isZero() || lease.isNegative()) {
			throw new IllegalArgumentException("lease must be > 0");
		}
		Objects.requireNonNull(renewInterval, "renewInterval");
		if (renewInterval.isZero() || renewInterval.isNegative()) {
			throw new IllegalArgumentException("renewInterval must be > 0");
		}
		this.lease = lease;
		this.watchdogEnabled = watchdogEnabled;
		this.renewInterval = renewInterval;
		this.token = UUID.randomUUID().toString();
		this.releaseAction = new ReleaseAction(redis, key, token, locked, watchdogFuture, ownerThread);
	}

	/**
	 * 获取锁并执行，结束后自动 unlock（异常同样释放）。拿不到锁则抛异常。
	 */
	public void run(Runnable action) {
		Objects.requireNonNull(action, "action");
		if (!tryLock()) {
			throw new IllegalStateException("Failed to acquire lock: " + key);
		}
		try {
			action.run();
		}
		finally {
			unlock();
		}
	}

	/**
	 * 尝试获取锁并执行；拿不到锁返回 {@code false}，拿到则执行后自动 unlock。
	 */
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

	/**
	 * 获取锁并执行有返回值的逻辑，结束后自动 unlock。
	 */
	public <T> T call(Callable<T> action) throws Exception {
		Objects.requireNonNull(action, "action");
		if (!tryLock()) {
			throw new IllegalStateException("Failed to acquire lock: " + key);
		}
		try {
			return action.call();
		}
		finally {
			unlock();
		}
	}

	/**
	 * 同 {@link #call(Callable)}，使用 {@link Supplier}（不声明受检异常）。
	 */
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

	/**
	 * 尝试获取锁并执行；失败返回 {@link Optional#empty()}，成功返回结果并自动 unlock。
	 */
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

	public boolean tryLock() {
		Boolean ok = redis.opsForValue().setIfAbsent(key, token, lease);
		if (!Boolean.TRUE.equals(ok)) {
			return false;
		}
		releaseAction.resetForRelock();
		locked.set(true);
		ownerThread.set(Thread.currentThread());
		// 注册 Cleaner：若忘记 unlock 且锁对象不可达，GC 时自动释放，避免看门狗无限续期
		cleanable = CLEANER.register(this, releaseAction);
		if (watchdogEnabled) {
			startWatchdog();
		}
		return true;
	}

	public boolean unlock() {
		boolean ok = releaseAction.release();
		Cleaner.Cleanable registered = this.cleanable;
		this.cleanable = null;
		if (registered != null) {
			// 注销注册；内部 release 已幂等
			registered.clean();
		}
		return ok;
	}

	@Override
	public void close() {
		unlock();
	}

	public boolean isLocked() {
		return locked.get();
	}

	public boolean isWatchdogEnabled() {
		return watchdogEnabled;
	}

	public Duration lease() {
		return lease;
	}

	public Duration renewInterval() {
		return renewInterval;
	}

	public String key() {
		return key;
	}

	boolean renewOnce() {
		Long result = redis.execute(RENEW, Collections.singletonList(key), token, String.valueOf(lease.toMillis()));
		return result != null && result == 1L;
	}

	private void startWatchdog() {
		releaseAction.stopWatchdogOnly();
		long periodMs = renewInterval.toMillis();
		ScheduledFuture<?> future = WATCHDOG_EXECUTOR.scheduleAtFixedRate(() -> {
			try {
				if (!locked.get()) {
					return;
				}
				Thread owner = ownerThread.get();
				// 持有线程已结束（非线程池复用场景）：自动停续期并释放
				if (owner != null && !owner.isAlive()) {
					releaseAction.release();
					return;
				}
				if (!renewOnce()) {
					releaseAction.release();
				}
			}
			catch (Exception ignored) {
				// 续期失败不抛到调度线程
			}
		}, periodMs, periodMs, TimeUnit.MILLISECONDS);
		watchdogFuture.set(future);
	}

	static Duration renewIntervalOf(Duration lease) {
		long ms = Math.max(1000L, lease.toMillis() / 3);
		return Duration.ofMillis(ms);
	}

	private static ScheduledExecutorService createWatchdogExecutor() {
		ThreadFactory factory = runnable -> {
			Thread thread = new Thread(runnable, "lind-redis-lock-watchdog");
			thread.setDaemon(true);
			return thread;
		};
		ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, factory);
		executor.setRemoveOnCancelPolicy(true);
		return executor;
	}

	/**
	 * Cleaner / unlock / 看门狗共用的释放逻辑（不引用外层 SpringRedisLock，避免阻碍 GC）。
	 */
	static final class ReleaseAction implements Runnable {

		private final StringRedisTemplate redis;

		private final String key;

		private final String token;

		private final AtomicBoolean locked;

		private final AtomicReference<ScheduledFuture<?>> watchdogFuture;

		private final AtomicReference<Thread> ownerThread;

		private final AtomicBoolean released = new AtomicBoolean(false);

		private volatile boolean lastUnlockOk;

		ReleaseAction(StringRedisTemplate redis, String key, String token, AtomicBoolean locked,
				AtomicReference<ScheduledFuture<?>> watchdogFuture, AtomicReference<Thread> ownerThread) {
			this.redis = redis;
			this.key = key;
			this.token = token;
			this.locked = locked;
			this.watchdogFuture = watchdogFuture;
			this.ownerThread = ownerThread;
		}

		@Override
		public void run() {
			release();
		}

		boolean release() {
			stopWatchdogOnly();
			ownerThread.set(null);
			locked.set(false);
			if (!released.compareAndSet(false, true)) {
				return lastUnlockOk;
			}
			Long result = redis.execute(UNLOCK, Collections.singletonList(key), token);
			lastUnlockOk = result != null && result == 1L;
			return lastUnlockOk;
		}

		void resetForRelock() {
			released.set(false);
			lastUnlockOk = false;
		}

		void stopWatchdogOnly() {
			ScheduledFuture<?> future = watchdogFuture.getAndSet(null);
			if (future != null) {
				future.cancel(false);
			}
		}

	}

}

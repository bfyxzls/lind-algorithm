package com.lind.algorithm.ratelimit;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * 滑动窗口限流：统计最近窗口内请求次数，超过阈值则拒绝。
 */
public final class SlidingWindowRateLimiter implements RateLimiter {

	private final int limit;

	private final long windowNanos;

	private final LongSupplier nanoClock;

	private final Deque<Long> timestamps = new ArrayDeque<>();

	private final Object lock = new Object();

	public SlidingWindowRateLimiter(int limit, long window, TimeUnit unit) {
		this(limit, window, unit, System::nanoTime);
	}

	SlidingWindowRateLimiter(int limit, long window, TimeUnit unit, LongSupplier nanoClock) {
		if (limit <= 0) {
			throw new IllegalArgumentException("limit must be > 0");
		}
		Objects.requireNonNull(unit, "unit");
		long nanos = unit.toNanos(window);
		if (nanos <= 0) {
			throw new IllegalArgumentException("window must be > 0");
		}
		this.limit = limit;
		this.windowNanos = nanos;
		this.nanoClock = Objects.requireNonNull(nanoClock, "nanoClock");
	}

	@Override
	public boolean tryAcquire() {
		return tryAcquire(1);
	}

	@Override
	public boolean tryAcquire(int permits) {
		if (permits <= 0) {
			throw new IllegalArgumentException("permits must be > 0");
		}
		synchronized (lock) {
			long now = nanoClock.getAsLong();
			evict(now);
			if (timestamps.size() + permits > limit) {
				return false;
			}
			for (int i = 0; i < permits; i++) {
				timestamps.addLast(now);
			}
			return true;
		}
	}

	public int currentCount() {
		synchronized (lock) {
			evict(nanoClock.getAsLong());
			return timestamps.size();
		}
	}

	private void evict(long now) {
		long threshold = now - windowNanos;
		while (!timestamps.isEmpty() && timestamps.peekFirst() <= threshold) {
			timestamps.removeFirst();
		}
	}

}

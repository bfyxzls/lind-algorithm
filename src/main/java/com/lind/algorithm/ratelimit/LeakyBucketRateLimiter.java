package com.lind.algorithm.ratelimit;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * 漏桶限流：请求进入桶中排队，以固定速率漏出；桶满则拒绝（整形流量更平滑）。
 */
public final class LeakyBucketRateLimiter implements RateLimiter {

	private final long capacity;

	private final double leakPerNanos;

	private final LongSupplier nanoClock;

	private final Object lock = new Object();

	private double water;

	private long lastLeakNanos;

	public LeakyBucketRateLimiter(long capacity, double leakPerSecond) {
		this(capacity, leakPerSecond, System::nanoTime);
	}

	LeakyBucketRateLimiter(long capacity, double leakPerSecond, LongSupplier nanoClock) {
		if (capacity <= 0) {
			throw new IllegalArgumentException("capacity must be > 0");
		}
		if (leakPerSecond <= 0) {
			throw new IllegalArgumentException("leakPerSecond must be > 0");
		}
		this.capacity = capacity;
		this.leakPerNanos = leakPerSecond / TimeUnit.SECONDS.toNanos(1);
		this.nanoClock = Objects.requireNonNull(nanoClock, "nanoClock");
		this.water = 0;
		this.lastLeakNanos = nanoClock.getAsLong();
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
			leak();
			if (water + permits > capacity) {
				return false;
			}
			water += permits;
			return true;
		}
	}

	public double waterLevel() {
		synchronized (lock) {
			leak();
			return water;
		}
	}

	private void leak() {
		long now = nanoClock.getAsLong();
		long elapsed = now - lastLeakNanos;
		if (elapsed <= 0) {
			return;
		}
		water = Math.max(0, water - elapsed * leakPerNanos);
		lastLeakNanos = now;
	}

}

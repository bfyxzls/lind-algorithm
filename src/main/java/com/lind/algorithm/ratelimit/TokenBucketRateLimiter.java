package com.lind.algorithm.ratelimit;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * 令牌桶限流：以固定速率向桶中放入令牌，请求消耗令牌；突发流量最多吃满桶容量。
 */
public final class TokenBucketRateLimiter implements RateLimiter {

	private final long capacity;

	private final double refillPerNanos;

	private final LongSupplier nanoClock;

	private final Object lock = new Object();

	private double tokens;

	private long lastRefillNanos;

	public TokenBucketRateLimiter(long capacity, double permitsPerSecond) {
		this(capacity, permitsPerSecond, System::nanoTime);
	}

	TokenBucketRateLimiter(long capacity, double permitsPerSecond, LongSupplier nanoClock) {
		if (capacity <= 0) {
			throw new IllegalArgumentException("capacity must be > 0");
		}
		if (permitsPerSecond <= 0) {
			throw new IllegalArgumentException("permitsPerSecond must be > 0");
		}
		this.capacity = capacity;
		this.refillPerNanos = permitsPerSecond / TimeUnit.SECONDS.toNanos(1);
		this.nanoClock = Objects.requireNonNull(nanoClock, "nanoClock");
		this.tokens = capacity;
		this.lastRefillNanos = nanoClock.getAsLong();
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
			refill();
			if (tokens < permits) {
				return false;
			}
			tokens -= permits;
			return true;
		}
	}

	public long capacity() {
		return capacity;
	}

	public double availableTokens() {
		synchronized (lock) {
			refill();
			return tokens;
		}
	}

	private void refill() {
		long now = nanoClock.getAsLong();
		long elapsed = now - lastRefillNanos;
		if (elapsed <= 0) {
			return;
		}
		tokens = Math.min(capacity, tokens + elapsed * refillPerNanos);
		lastRefillNanos = now;
	}

}

package com.lind.algorithm.ratelimit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/**
 * 限流器单元测试。
 */
public class RateLimiterTest {

	@Test
	void tokenBucketAllowsBurstThenRejects() {
		AtomicLong now = new AtomicLong(0);
		TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(2, 1.0, now::get);
		assertTrue(limiter.tryAcquire());
		assertTrue(limiter.tryAcquire());
		assertFalse(limiter.tryAcquire());
		now.addAndGet(TimeUnit.SECONDS.toNanos(2));
		assertTrue(limiter.tryAcquire());
	}

	@Test
	void leakyBucketRejectsWhenFull() {
		AtomicLong now = new AtomicLong(0);
		LeakyBucketRateLimiter limiter = new LeakyBucketRateLimiter(2, 1.0, now::get);
		assertTrue(limiter.tryAcquire());
		assertTrue(limiter.tryAcquire());
		assertFalse(limiter.tryAcquire());
		now.addAndGet(TimeUnit.SECONDS.toNanos(2));
		assertTrue(limiter.tryAcquire());
	}

	@Test
	void slidingWindowCountsInWindow() {
		AtomicLong now = new AtomicLong(1_000_000_000L);
		SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(2, 1, TimeUnit.SECONDS, now::get);
		assertTrue(limiter.tryAcquire());
		assertTrue(limiter.tryAcquire());
		assertFalse(limiter.tryAcquire());
		now.addAndGet(TimeUnit.SECONDS.toNanos(1) + 1);
		assertTrue(limiter.tryAcquire());
	}

}

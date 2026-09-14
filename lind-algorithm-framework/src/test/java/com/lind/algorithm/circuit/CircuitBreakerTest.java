package com.lind.algorithm.circuit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/**
 * {@link CircuitBreaker} 单元测试。
 */
public class CircuitBreakerTest {

	@Test
	void opensAfterFailuresAndRecovers() {
		AtomicLong now = new AtomicLong(0);
		CircuitBreaker breaker = new CircuitBreaker(2, 1, 1, TimeUnit.SECONDS, now::get);

		assertThrows(RuntimeException.class, () -> breaker.execute(() -> {
			throw new RuntimeException("boom");
		}));
		assertThrows(RuntimeException.class, () -> breaker.execute(() -> {
			throw new RuntimeException("boom");
		}));
		assertEquals(CircuitBreaker.State.OPEN, breaker.getState());
		assertThrows(CircuitOpenException.class, () -> breaker.execute(() -> "x"));

		now.addAndGet(TimeUnit.SECONDS.toNanos(1));
		assertEquals(CircuitBreaker.State.HALF_OPEN, breaker.getState());
		assertEquals("ok", breaker.execute(() -> "ok"));
		assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
		assertTrue(breaker.allowRequest());
	}

}

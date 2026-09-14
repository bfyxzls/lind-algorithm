package com.lind.algorithm.retry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * {@link RetryTemplate} 单元测试。
 */
public class RetryTemplateTest {

	@Test
	void retriesUntilSuccess() throws Exception {
		AtomicInteger calls = new AtomicInteger();
		RetryTemplate retry = RetryTemplate.builder().maxAttempts(3).initialBackoff(0, TimeUnit.MILLISECONDS)
				.jitter(false).build();
		String result = retry.execute(() -> {
			if (calls.incrementAndGet() < 3) {
				throw new IllegalStateException("fail");
			}
			return "ok";
		});
		assertEquals("ok", result);
		assertEquals(3, calls.get());
	}

	@Test
	void exponentialBackoffWithoutJitter() {
		RetryTemplate retry = RetryTemplate.builder().initialBackoff(100, TimeUnit.MILLISECONDS).multiplier(2.0)
				.maxBackoff(1, TimeUnit.SECONDS).jitter(false).build();
		assertEquals(100, retry.backoffMillis(1));
		assertEquals(200, retry.backoffMillis(2));
		assertEquals(400, retry.backoffMillis(3));
	}

	@Test
	void respectsRetryOnPredicate() {
		RetryTemplate retry = RetryTemplate.builder().maxAttempts(3).initialBackoff(0, TimeUnit.MILLISECONDS)
				.retryOn(ex -> ex instanceof IllegalArgumentException).build();
		AtomicInteger calls = new AtomicInteger();
		assertThrows(IllegalStateException.class, () -> retry.execute(() -> {
			calls.incrementAndGet();
			throw new IllegalStateException("no-retry");
		}));
		assertEquals(1, calls.get());
		assertTrue(true);
	}

}

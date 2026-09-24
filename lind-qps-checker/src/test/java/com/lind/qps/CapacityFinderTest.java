package com.lind.qps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * {@link CapacityFinder} 单元测试。
 */
public class CapacityFinderTest {

	@Test
	void findsMaxConcurrencyBeforeSlaBreak() {
		AtomicInteger lastConcurrency = new AtomicInteger();
		LoadConfig base = LoadConfig.builder().url("http://localhost/unused").concurrency(1)
				.duration(Duration.ofMillis(50)).build();
		CapacityFinder finder = new CapacityFinder(base, 10, 10, 50, 99.0, 100, config -> {
			lastConcurrency.set(config.concurrency());
			boolean pass = config.concurrency() <= 30;
			long[] latencies = pass ? new long[] { 10, 20 } : new long[] { 10, 200 };
			long success = pass ? 100 : 50;
			long failure = pass ? 0 : 50;
			return LoadReport.fromLatencies(config.url().toString(), config.concurrency(), 50, success, failure,
					latencies);
		});
		CapacityFinder.CapacityResult result = finder.find();
		assertEquals(30, result.maxConcurrency());
		assertEquals(40, lastConcurrency.get());
		assertTrue(result.format().contains("最大可用并发数"));
		assertEquals(30, result.bestPassingReport().concurrency());
	}

}

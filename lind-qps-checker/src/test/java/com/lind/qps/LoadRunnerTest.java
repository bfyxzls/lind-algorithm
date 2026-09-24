package com.lind.qps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * {@link LoadRunner} / {@link LoadReport} 单元测试。
 */
public class LoadRunnerTest {

	@Test
	void runCollectsSuccessAndLatency() {
		AtomicInteger calls = new AtomicInteger();
		LoadConfig config = LoadConfig.builder().url("http://localhost/unused").concurrency(4)
				.duration(Duration.ofMillis(200)).requestTimeout(Duration.ofSeconds(1)).build();
		LoadRunner runner = new LoadRunner(config, () -> {
			calls.incrementAndGet();
			return RequestResult.ok(200, 5_000_000L);
		});
		LoadReport report = runner.run();
		assertTrue(report.totalRequests() > 0);
		assertEquals(report.totalRequests(), report.successCount());
		assertEquals(100.0, report.successRate(), 0.001);
		assertEquals(4, report.concurrency());
		assertEquals(5, report.maxLatencyMillis());
		assertTrue(report.qps() > 0);
		assertTrue(calls.get() > 0);
	}

	@Test
	void reportPercentiles() {
		long[] sorted = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
		LoadReport report = LoadReport.fromLatencies("http://x", 2, 1000, 10, 0, sorted);
		assertEquals(1, report.minLatencyMillis());
		assertEquals(10, report.maxLatencyMillis());
		assertEquals(10, report.p95LatencyMillis());
		assertEquals(10, report.p99LatencyMillis());
		assertTrue(report.format().contains("成功率"));
	}

}

package com.lind.qps.engine;

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

	@Test
	void runCollectsFailureStatusAndBody() {
		LoadConfig config = LoadConfig.builder().url("http://localhost/unused").concurrency(2)
				.duration(Duration.ofMillis(150)).requestTimeout(Duration.ofSeconds(1)).build();
		LoadRunner runner = new LoadRunner(config,
				() -> RequestResult.fail(500, 3_000_000L, "unexpected status 500", "{\"error\":\"boom\"}"));
		LoadReport report = runner.run();
		assertTrue(report.failureCount() > 0);
		assertTrue(report.failureSamples().size() > 0);
		FailureSample sample = report.failureSamples().get(0);
		assertEquals(500, sample.statusCode());
		assertEquals("{\"error\":\"boom\"}", sample.responseBody());
		assertTrue(report.format().contains("失败样本"));
		assertTrue(report.format().contains("status=500"));
	}

}

package com.lind.qps.engine;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 单个压测周期的统计报告。
 */
public final class LoadReport {

	private final String target;

	private final int concurrency;

	private final long durationMillis;

	private final long totalRequests;

	private final long successCount;

	private final long failureCount;

	private final long minLatencyMillis;

	private final long avgLatencyMillis;

	private final long maxLatencyMillis;

	private final long p95LatencyMillis;

	private final long p99LatencyMillis;

	private final double qps;

	private final List<FailureSample> failureSamples;

	public LoadReport(String target, int concurrency, long durationMillis, long totalRequests, long successCount,
			long failureCount, long minLatencyMillis, long avgLatencyMillis, long maxLatencyMillis,
			long p95LatencyMillis, long p99LatencyMillis, double qps) {
		this(target, concurrency, durationMillis, totalRequests, successCount, failureCount, minLatencyMillis,
				avgLatencyMillis, maxLatencyMillis, p95LatencyMillis, p99LatencyMillis, qps, List.of());
	}

	public LoadReport(String target, int concurrency, long durationMillis, long totalRequests, long successCount,
			long failureCount, long minLatencyMillis, long avgLatencyMillis, long maxLatencyMillis,
			long p95LatencyMillis, long p99LatencyMillis, double qps, List<FailureSample> failureSamples) {
		this.target = Objects.requireNonNull(target, "target");
		this.concurrency = concurrency;
		this.durationMillis = durationMillis;
		this.totalRequests = totalRequests;
		this.successCount = successCount;
		this.failureCount = failureCount;
		this.minLatencyMillis = minLatencyMillis;
		this.avgLatencyMillis = avgLatencyMillis;
		this.maxLatencyMillis = maxLatencyMillis;
		this.p95LatencyMillis = p95LatencyMillis;
		this.p99LatencyMillis = p99LatencyMillis;
		this.qps = qps;
		this.failureSamples = List.copyOf(failureSamples == null ? List.of() : failureSamples);
	}

	public static LoadReport fromLatencies(String target, int concurrency, long durationMillis, long successCount,
			long failureCount, long[] latencyMillisSorted) {
		return fromLatencies(target, concurrency, durationMillis, successCount, failureCount, latencyMillisSorted,
				List.of());
	}

	public static LoadReport fromLatencies(String target, int concurrency, long durationMillis, long successCount,
			long failureCount, long[] latencyMillisSorted, List<FailureSample> failureSamples) {
		long total = successCount + failureCount;
		long min = 0;
		long max = 0;
		long avg = 0;
		long p95 = 0;
		long p99 = 0;
		if (latencyMillisSorted.length > 0) {
			min = latencyMillisSorted[0];
			max = latencyMillisSorted[latencyMillisSorted.length - 1];
			long sum = 0;
			for (long v : latencyMillisSorted) {
				sum += v;
			}
			avg = sum / latencyMillisSorted.length;
			p95 = percentile(latencyMillisSorted, 0.95);
			p99 = percentile(latencyMillisSorted, 0.99);
		}
		double seconds = Math.max(durationMillis / 1000.0, 0.001);
		double qps = total / seconds;
		return new LoadReport(target, concurrency, durationMillis, total, successCount, failureCount, min, avg, max,
				p95, p99, qps, failureSamples);
	}

	private static long percentile(long[] sorted, double p) {
		if (sorted.length == 0) {
			return 0;
		}
		int index = (int) Math.ceil(p * sorted.length) - 1;
		index = Math.max(0, Math.min(sorted.length - 1, index));
		return sorted[index];
	}

	public String target() {
		return target;
	}

	public int concurrency() {
		return concurrency;
	}

	public long durationMillis() {
		return durationMillis;
	}

	public long totalRequests() {
		return totalRequests;
	}

	public long successCount() {
		return successCount;
	}

	public long failureCount() {
		return failureCount;
	}

	public double successRate() {
		if (totalRequests == 0) {
			return 0;
		}
		return successCount * 100.0 / totalRequests;
	}

	public long minLatencyMillis() {
		return minLatencyMillis;
	}

	public long avgLatencyMillis() {
		return avgLatencyMillis;
	}

	public long maxLatencyMillis() {
		return maxLatencyMillis;
	}

	public long p95LatencyMillis() {
		return p95LatencyMillis;
	}

	public long p99LatencyMillis() {
		return p99LatencyMillis;
	}

	public double qps() {
		return qps;
	}

	/**
	 * 失败样本（含 HTTP 状态码与响应体），数量有上限。
	 */
	public List<FailureSample> failureSamples() {
		return failureSamples;
	}

	public boolean meetsSla(double minSuccessRatePercent, long maxAllowedLatencyMillis) {
		return successRate() >= minSuccessRatePercent && maxLatencyMillis <= maxAllowedLatencyMillis;
	}

	public String format() {
		StringBuilder sb = new StringBuilder();
		sb.append("""
				========== QPS Checker 测试报告 ==========
				目标地址        : %s
				测试并发数      : %d
				测试周期        : %d ms
				总请求数        : %d
				成功数          : %d
				失败数          : %d
				成功率          : %s%%
				QPS             : %s
				最小响应时间    : %d ms
				平均响应时间    : %d ms
				最大响应时间    : %d ms
				P95 响应时间    : %d ms
				P99 响应时间    : %d ms
				""".formatted(target, concurrency, durationMillis, totalRequests, successCount, failureCount,
				String.format(Locale.ROOT, "%.2f", successRate()), String.format(Locale.ROOT, "%.2f", qps),
				minLatencyMillis, avgLatencyMillis, maxLatencyMillis, p95LatencyMillis, p99LatencyMillis));
		if (!failureSamples.isEmpty()) {
			sb.append("---------- 失败样本（最多 ").append(failureSamples.size()).append(" 条） ----------\n");
			int i = 1;
			for (FailureSample sample : failureSamples) {
				sb.append(String.format(Locale.ROOT, "#%d status=%d latency=%d ms error=%s%n  body=%s%n", i++,
						sample.statusCode(), sample.latencyMillis(),
						sample.errorMessage() == null || sample.errorMessage().isEmpty() ? "-" : sample.errorMessage(),
						sample.responseBody() == null || sample.responseBody().isEmpty() ? "-"
								: sample.responseBody().replace('\n', ' ')));
			}
		}
		sb.append("==========================================\n");
		return sb.toString();
	}

	@Override
	public String toString() {
		return format().trim();
	}

	static long[] sortedCopy(long[] values) {
		long[] copy = Arrays.copyOf(values, values.length);
		Arrays.sort(copy);
		return copy;
	}

}

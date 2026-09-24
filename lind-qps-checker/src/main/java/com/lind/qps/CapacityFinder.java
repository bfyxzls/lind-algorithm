package com.lind.qps;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/**
 * 阶梯加压：在多个短周期内逐步提高并发，找出仍满足 SLA 的最大并发数。
 */
public final class CapacityFinder {

	private final LoadConfig baseConfig;

	private final int startConcurrency;

	private final int step;

	private final int maxConcurrency;

	private final double minSuccessRatePercent;

	private final long maxAllowedLatencyMillis;

	private final Function<LoadConfig, LoadReport> runnerFactory;

	public CapacityFinder(LoadConfig baseConfig, int startConcurrency, int step, int maxConcurrency,
			double minSuccessRatePercent, long maxAllowedLatencyMillis) {
		this(baseConfig, startConcurrency, step, maxConcurrency, minSuccessRatePercent, maxAllowedLatencyMillis,
				config -> new LoadRunner(config).run());
	}

	CapacityFinder(LoadConfig baseConfig, int startConcurrency, int step, int maxConcurrency,
			double minSuccessRatePercent, long maxAllowedLatencyMillis,
			Function<LoadConfig, LoadReport> runnerFactory) {
		this.baseConfig = Objects.requireNonNull(baseConfig, "baseConfig");
		this.startConcurrency = startConcurrency;
		this.step = step;
		this.maxConcurrency = maxConcurrency;
		this.minSuccessRatePercent = minSuccessRatePercent;
		this.maxAllowedLatencyMillis = maxAllowedLatencyMillis;
		this.runnerFactory = Objects.requireNonNull(runnerFactory, "runnerFactory");
		if (startConcurrency <= 0 || step <= 0 || maxConcurrency < startConcurrency) {
			throw new IllegalArgumentException("invalid concurrency ramp range");
		}
	}

	public CapacityResult find() {
		List<LoadReport> steps = new ArrayList<>();
		LoadReport best = null;
		for (int concurrency = startConcurrency; concurrency <= maxConcurrency; concurrency += step) {
			LoadConfig stepConfig = baseConfig.toBuilder().concurrency(concurrency).build();
			LoadReport report = runnerFactory.apply(stepConfig);
			steps.add(report);
			if (!report.meetsSla(minSuccessRatePercent, maxAllowedLatencyMillis)) {
				break;
			}
			best = report;
		}
		int maxOk = best == null ? 0 : best.concurrency();
		LoadReport last = steps.isEmpty() ? null : steps.get(steps.size() - 1);
		return new CapacityResult(maxOk, best, last, List.copyOf(steps), minSuccessRatePercent,
				maxAllowedLatencyMillis);
	}

	/**
	 * 最大并发探测结果。
	 */
	public record CapacityResult(int maxConcurrency, LoadReport bestPassingReport, LoadReport lastReport,
			List<LoadReport> stepReports, double minSuccessRatePercent, long maxAllowedLatencyMillis) {

		public String format() {
			StringBuilder sb = new StringBuilder();
			sb.append("========== 最大并发能力探测报告 ==========\n");
			sb.append("SLA 成功率门槛  : ").append(String.format(Locale.ROOT, "%.2f", minSuccessRatePercent)).append("%\n");
			sb.append("SLA 最大响应时间: ").append(maxAllowedLatencyMillis).append(" ms\n");
			sb.append("最大可用并发数  : ").append(maxConcurrency).append('\n');
			if (bestPassingReport != null) {
				sb.append("对应 QPS         : ").append(String.format(Locale.ROOT, "%.2f", bestPassingReport.qps()))
						.append('\n');
				sb.append("对应最大响应时间: ").append(bestPassingReport.maxLatencyMillis()).append(" ms\n");
				sb.append("对应成功率      : ").append(String.format(Locale.ROOT, "%.2f", bestPassingReport.successRate()))
						.append("%\n");
			}
			sb.append("---------- 各阶梯明细 ----------\n");
			for (LoadReport step : stepReports) {
				sb.append(String.format(Locale.ROOT, "并发=%-5d 成功率=%6.2f%%  maxRT=%5d ms  QPS=%8.2f  总请求=%d%n",
						step.concurrency(), step.successRate(), step.maxLatencyMillis(), step.qps(),
						step.totalRequests()));
			}
			if (lastReport != null && (bestPassingReport == null || lastReport.concurrency() != maxConcurrency)) {
				sb.append("触发停止的阶梯  : 并发 ").append(lastReport.concurrency()).append("（未通过 SLA）\n");
			}
			sb.append("==========================================\n");
			return sb.toString();
		}
	}

	public static Builder builder(LoadConfig baseConfig) {
		return new Builder(baseConfig);
	}

	public static final class Builder {

		private final LoadConfig baseConfig;

		private int startConcurrency = 10;

		private int step = 10;

		private int maxConcurrency = 200;

		private double minSuccessRatePercent = 99.0;

		private long maxAllowedLatencyMillis = 3000;

		private Duration stepDuration;

		public Builder(LoadConfig baseConfig) {
			this.baseConfig = baseConfig;
		}

		public Builder startConcurrency(int startConcurrency) {
			this.startConcurrency = startConcurrency;
			return this;
		}

		public Builder step(int step) {
			this.step = step;
			return this;
		}

		public Builder maxConcurrency(int maxConcurrency) {
			this.maxConcurrency = maxConcurrency;
			return this;
		}

		public Builder minSuccessRatePercent(double minSuccessRatePercent) {
			this.minSuccessRatePercent = minSuccessRatePercent;
			return this;
		}

		public Builder maxAllowedLatencyMillis(long maxAllowedLatencyMillis) {
			this.maxAllowedLatencyMillis = maxAllowedLatencyMillis;
			return this;
		}

		public Builder stepDuration(Duration stepDuration) {
			this.stepDuration = stepDuration;
			return this;
		}

		public CapacityFinder build() {
			LoadConfig config = stepDuration == null ? baseConfig
					: baseConfig.toBuilder().duration(stepDuration).build();
			return new CapacityFinder(config, startConcurrency, step, maxConcurrency, minSuccessRatePercent,
					maxAllowedLatencyMillis);
		}

	}

}

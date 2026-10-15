package com.lind.qps.web.job;

import com.lind.qps.engine.CapacityFinder;
import com.lind.qps.engine.LoadConfig;
import com.lind.qps.engine.LoadReport;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 一次压测任务（内存态）。
 */
public final class LoadJob {

	private final String id;

	private final LoadMode mode;

	private final LoadConfig config;

	private final Instant createdAt;

	private final AtomicReference<LoadJobStatus> status = new AtomicReference<>(LoadJobStatus.PENDING);

	private final AtomicReference<String> message = new AtomicReference<>("");

	private final AtomicReference<LoadReport> report = new AtomicReference<>();

	private final AtomicReference<CapacityFinder.CapacityResult> capacityResult = new AtomicReference<>();

	private final AtomicReference<Instant> finishedAt = new AtomicReference<>();

	private volatile int startConcurrency;

	private volatile int step;

	private volatile int maxConcurrency;

	private volatile double minSuccessRatePercent = 99.0;

	private volatile long maxAllowedLatencyMillis = 3000;

	public LoadJob(String id, LoadMode mode, LoadConfig config) {
		this(id, mode, config, Instant.now());
	}

	public LoadJob(String id, LoadMode mode, LoadConfig config, Instant createdAt) {
		this.id = id;
		this.mode = mode;
		this.config = config;
		this.createdAt = createdAt == null ? Instant.now() : createdAt;
	}

	public String id() {
		return id;
	}

	public LoadMode mode() {
		return mode;
	}

	public LoadConfig config() {
		return config;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public LoadJobStatus status() {
		return status.get();
	}

	public void status(LoadJobStatus value) {
		status.set(value);
	}

	public String message() {
		return message.get();
	}

	public void message(String value) {
		message.set(value == null ? "" : value);
	}

	public LoadReport report() {
		return report.get();
	}

	public void report(LoadReport value) {
		report.set(value);
	}

	public CapacityFinder.CapacityResult capacityResult() {
		return capacityResult.get();
	}

	public void capacityResult(CapacityFinder.CapacityResult value) {
		capacityResult.set(value);
	}

	public Instant finishedAt() {
		return finishedAt.get();
	}

	public void finishedAt(Instant value) {
		finishedAt.set(value);
	}

	public int startConcurrency() {
		return startConcurrency;
	}

	public void startConcurrency(int startConcurrency) {
		this.startConcurrency = startConcurrency;
	}

	public int step() {
		return step;
	}

	public void step(int step) {
		this.step = step;
	}

	public int maxConcurrency() {
		return maxConcurrency;
	}

	public void maxConcurrency(int maxConcurrency) {
		this.maxConcurrency = maxConcurrency;
	}

	public double minSuccessRatePercent() {
		return minSuccessRatePercent;
	}

	public void minSuccessRatePercent(double minSuccessRatePercent) {
		this.minSuccessRatePercent = minSuccessRatePercent;
	}

	public long maxAllowedLatencyMillis() {
		return maxAllowedLatencyMillis;
	}

	public void maxAllowedLatencyMillis(long maxAllowedLatencyMillis) {
		this.maxAllowedLatencyMillis = maxAllowedLatencyMillis;
	}

	public List<LoadReport> stepReports() {
		CapacityFinder.CapacityResult result = capacityResult.get();
		if (result == null) {
			return List.of();
		}
		return new ArrayList<>(result.stepReports());
	}

}

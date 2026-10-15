package com.lind.qps.web.job;

import com.lind.qps.engine.CapacityFinder;
import com.lind.qps.engine.FailureSample;
import com.lind.qps.engine.LoadReport;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务视图 / API JSON 映射。
 */
final class LoadJobViews {

	private LoadJobViews() {
	}

	static Map<String, Object> toMap(LoadJob job) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("id", job.id());
		map.put("mode", job.mode().name());
		map.put("status", job.status().name());
		map.put("message", job.message());
		map.put("createdAt", job.createdAt().toString());
		Instant finished = job.finishedAt();
		map.put("finishedAt", finished == null ? null : finished.toString());
		map.put("url", job.config().url().toString());
		map.put("method", job.config().method());
		map.put("concurrency", job.config().concurrency());
		map.put("durationSeconds", job.config().duration().toSeconds());
		map.put("timeoutMs", job.config().requestTimeout().toMillis());
		map.put("report", job.report() == null ? null : reportMap(job.report()));
		CapacityFinder.CapacityResult capacity = job.capacityResult();
		if (capacity != null) {
			Map<String, Object> cap = new LinkedHashMap<>();
			cap.put("maxConcurrency", capacity.maxConcurrency());
			cap.put("minSuccessRatePercent", capacity.minSuccessRatePercent());
			cap.put("maxAllowedLatencyMillis", capacity.maxAllowedLatencyMillis());
			cap.put("text", capacity.format());
			List<Map<String, Object>> steps = new ArrayList<>();
			for (LoadReport step : capacity.stepReports()) {
				steps.add(reportMap(step));
			}
			cap.put("steps", steps);
			map.put("capacity", cap);
		}
		else {
			map.put("capacity", null);
		}
		if (job.report() != null) {
			map.put("reportText", job.report().format());
		}
		return map;
	}

	static Map<String, Object> reportMap(LoadReport report) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("target", report.target());
		map.put("concurrency", report.concurrency());
		map.put("durationMillis", report.durationMillis());
		map.put("totalRequests", report.totalRequests());
		map.put("successCount", report.successCount());
		map.put("failureCount", report.failureCount());
		map.put("successRate", report.successRate());
		map.put("qps", report.qps());
		map.put("minLatencyMillis", report.minLatencyMillis());
		map.put("avgLatencyMillis", report.avgLatencyMillis());
		map.put("maxLatencyMillis", report.maxLatencyMillis());
		map.put("p95LatencyMillis", report.p95LatencyMillis());
		map.put("p99LatencyMillis", report.p99LatencyMillis());
		List<Map<String, Object>> failures = new ArrayList<>();
		for (FailureSample sample : report.failureSamples()) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("statusCode", sample.statusCode());
			row.put("responseBody", sample.responseBody());
			row.put("errorMessage", sample.errorMessage());
			row.put("latencyMillis", sample.latencyMillis());
			failures.add(row);
		}
		map.put("failureSamples", failures);
		return map;
	}

}

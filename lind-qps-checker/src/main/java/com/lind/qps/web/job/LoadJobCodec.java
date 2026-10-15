package com.lind.qps.web.job;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lind.qps.engine.CapacityFinder;
import com.lind.qps.engine.FailureSample;
import com.lind.qps.engine.LoadConfig;
import com.lind.qps.engine.LoadReport;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 压测任务报告 / 配置 JSON 编解码（持久化用）。
 */
@Component
public class LoadJobCodec {

	private final ObjectMapper mapper;

	public LoadJobCodec(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	public String writeMap(Map<String, String> map) {
		try {
			return mapper.writeValueAsString(map == null ? Map.of() : map);
		}
		catch (Exception ex) {
			throw new IllegalStateException("serialize map failed", ex);
		}
	}

	public Map<String, String> readMap(String json) {
		if (json == null || json.isBlank()) {
			return Map.of();
		}
		try {
			return mapper.readValue(json, new TypeReference<LinkedHashMap<String, String>>() {
			});
		}
		catch (Exception ex) {
			throw new IllegalStateException("deserialize map failed", ex);
		}
	}

	public String writeReport(LoadReport report) {
		if (report == null) {
			return null;
		}
		try {
			return mapper.writeValueAsString(toReportNode(report));
		}
		catch (Exception ex) {
			throw new IllegalStateException("serialize report failed", ex);
		}
	}

	public LoadReport readReport(String json) {
		if (json == null || json.isBlank()) {
			return null;
		}
		try {
			return fromReportNode(mapper.readTree(json));
		}
		catch (Exception ex) {
			throw new IllegalStateException("deserialize report failed", ex);
		}
	}

	public String writeCapacity(CapacityFinder.CapacityResult result) {
		if (result == null) {
			return null;
		}
		try {
			ObjectNode node = mapper.createObjectNode();
			node.put("maxConcurrency", result.maxConcurrency());
			node.put("minSuccessRatePercent", result.minSuccessRatePercent());
			node.put("maxAllowedLatencyMillis", result.maxAllowedLatencyMillis());
			if (result.bestPassingReport() != null) {
				node.set("bestPassingReport", toReportNode(result.bestPassingReport()));
			}
			if (result.lastReport() != null) {
				node.set("lastReport", toReportNode(result.lastReport()));
			}
			ArrayNode steps = node.putArray("stepReports");
			for (LoadReport step : result.stepReports()) {
				steps.add(toReportNode(step));
			}
			return mapper.writeValueAsString(node);
		}
		catch (Exception ex) {
			throw new IllegalStateException("serialize capacity failed", ex);
		}
	}

	public CapacityFinder.CapacityResult readCapacity(String json) {
		if (json == null || json.isBlank()) {
			return null;
		}
		try {
			JsonNode node = mapper.readTree(json);
			LoadReport best = node.hasNonNull("bestPassingReport") ? fromReportNode(node.get("bestPassingReport"))
					: null;
			LoadReport last = node.hasNonNull("lastReport") ? fromReportNode(node.get("lastReport")) : null;
			List<LoadReport> steps = new ArrayList<>();
			JsonNode stepNode = node.get("stepReports");
			if (stepNode != null && stepNode.isArray()) {
				for (JsonNode child : stepNode) {
					steps.add(fromReportNode(child));
				}
			}
			return new CapacityFinder.CapacityResult(node.path("maxConcurrency").asInt(0), best, last,
					List.copyOf(steps), node.path("minSuccessRatePercent").asDouble(99.0),
					node.path("maxAllowedLatencyMillis").asLong(3000));
		}
		catch (Exception ex) {
			throw new IllegalStateException("deserialize capacity failed", ex);
		}
	}

	private ObjectNode toReportNode(LoadReport report) {
		ObjectNode node = mapper.createObjectNode();
		node.put("target", report.target());
		node.put("concurrency", report.concurrency());
		node.put("durationMillis", report.durationMillis());
		node.put("totalRequests", report.totalRequests());
		node.put("successCount", report.successCount());
		node.put("failureCount", report.failureCount());
		node.put("minLatencyMillis", report.minLatencyMillis());
		node.put("avgLatencyMillis", report.avgLatencyMillis());
		node.put("maxLatencyMillis", report.maxLatencyMillis());
		node.put("p95LatencyMillis", report.p95LatencyMillis());
		node.put("p99LatencyMillis", report.p99LatencyMillis());
		node.put("qps", report.qps());
		ArrayNode failures = node.putArray("failureSamples");
		for (FailureSample sample : report.failureSamples()) {
			ObjectNode row = failures.addObject();
			row.put("statusCode", sample.statusCode());
			row.put("responseBody", sample.responseBody());
			row.put("errorMessage", sample.errorMessage());
			row.put("latencyMillis", sample.latencyMillis());
		}
		return node;
	}

	private LoadReport fromReportNode(JsonNode node) {
		List<FailureSample> samples = new ArrayList<>();
		JsonNode failures = node.get("failureSamples");
		if (failures != null && failures.isArray()) {
			for (JsonNode child : failures) {
				samples.add(new FailureSample(child.path("statusCode").asInt(-1), child.path("responseBody").asText(""),
						child.path("errorMessage").asText(""), child.path("latencyMillis").asLong(0)));
			}
		}
		return new LoadReport(node.path("target").asText(""), node.path("concurrency").asInt(0),
				node.path("durationMillis").asLong(0), node.path("totalRequests").asLong(0),
				node.path("successCount").asLong(0), node.path("failureCount").asLong(0),
				node.path("minLatencyMillis").asLong(0), node.path("avgLatencyMillis").asLong(0),
				node.path("maxLatencyMillis").asLong(0), node.path("p95LatencyMillis").asLong(0),
				node.path("p99LatencyMillis").asLong(0), node.path("qps").asDouble(0), samples);
	}

	public LoadConfig toConfig(String url, String method, int concurrency, long durationSeconds, long timeoutMs,
			String headersJson, String queryJson, String formJson, String body) {
		LoadConfig.Builder builder = LoadConfig.builder().url(url).method(method).concurrency(concurrency)
				.durationSeconds(durationSeconds).requestTimeoutMillis(timeoutMs);
		readMap(headersJson).forEach(builder::header);
		readMap(queryJson).forEach(builder::queryParam);
		readMap(formJson).forEach(builder::formParam);
		if (body != null && !body.isBlank()) {
			builder.body(body);
		}
		return builder.build();
	}

}

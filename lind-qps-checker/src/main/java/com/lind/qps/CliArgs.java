package com.lind.qps;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 命令行参数解析。支持重复的 --header / --param / --form。
 */
public final class CliArgs {

	private final Map<String, String> values = new LinkedHashMap<>();

	private final List<String> headers = new ArrayList<>();

	private final List<String> queryParams = new ArrayList<>();

	private final List<String> formParams = new ArrayList<>();

	private CliArgs(String[] args) {
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (!arg.startsWith("--")) {
				throw new IllegalArgumentException("unknown argument: " + arg);
			}
			String key = arg.substring(2);
			if ("help".equals(key) || "h".equals(key)) {
				values.put("help", "true");
				continue;
			}
			if ("find-max".equals(key)) {
				values.put("find-max", "true");
				continue;
			}
			if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
				throw new IllegalArgumentException("missing value for --" + key);
			}
			String value = args[++i];
			switch (key) {
				case "header", "headers" -> appendPairs(headers, value);
				case "param", "params", "query" -> appendPairs(queryParams, value);
				case "form", "forms" -> appendPairs(formParams, value);
				default -> values.put(key, value);
			}
		}
	}

	private static void appendPairs(List<String> target, String raw) {
		if (raw == null || raw.isBlank()) {
			return;
		}
		// 允许一次写多个：a=1&b=2 或 Name:V1;Name2:V2
		if (raw.contains(";") && !raw.contains("=")) {
			for (String part : raw.split(";")) {
				if (!part.isBlank()) {
					target.add(part.trim());
				}
			}
			return;
		}
		if (raw.contains("&") && raw.contains("=")) {
			for (String part : raw.split("&")) {
				if (!part.isBlank()) {
					target.add(part.trim());
				}
			}
			return;
		}
		target.add(raw.trim());
	}

	public static CliArgs parse(String[] args) {
		return new CliArgs(args);
	}

	public boolean help() {
		return values.containsKey("help");
	}

	public boolean findMax() {
		return values.containsKey("find-max");
	}

	public String require(String key) {
		String value = values.get(key);
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("missing required --" + key);
		}
		return value;
	}

	public String get(String key, String defaultValue) {
		return values.getOrDefault(key, defaultValue);
	}

	public int getInt(String key, int defaultValue) {
		String raw = values.get(key);
		return raw == null ? defaultValue : Integer.parseInt(raw);
	}

	public long getLong(String key, long defaultValue) {
		String raw = values.get(key);
		return raw == null ? defaultValue : Long.parseLong(raw);
	}

	public double getDouble(String key, double defaultValue) {
		String raw = values.get(key);
		return raw == null ? defaultValue : Double.parseDouble(raw);
	}

	public LoadConfig toLoadConfig() {
		LoadConfig.Builder builder = LoadConfig.builder().url(require("url")).method(get("method", "GET"))
				.concurrency(getInt("concurrency", 10)).durationSeconds(getLong("duration", 10))
				.requestTimeoutMillis(getLong("timeout-ms", 5000));
		String body = values.get("body");
		if (body != null) {
			builder.body(body);
		}
		for (String header : headers) {
			String[] pair = splitHeader(header);
			builder.header(pair[0], pair[1]);
		}
		for (String param : queryParams) {
			String[] pair = splitKv(param, "param");
			builder.queryParam(pair[0], pair[1]);
		}
		for (String form : formParams) {
			String[] pair = splitKv(form, "form");
			builder.formParam(pair[0], pair[1]);
		}
		return builder.build();
	}

	/**
	 * Header：优先按第一个冒号拆分 Name:Value，也支持 Name=Value。
	 */
	static String[] splitHeader(String raw) {
		int colon = raw.indexOf(':');
		int eq = raw.indexOf('=');
		int idx;
		if (colon > 0 && (eq < 0 || colon < eq)) {
			idx = colon;
		}
		else if (eq > 0) {
			idx = eq;
		}
		else {
			throw new IllegalArgumentException("invalid --header, expect Name:Value or Name=Value, got: " + raw);
		}
		String name = raw.substring(0, idx).trim();
		String value = raw.substring(idx + 1).trim();
		if (name.isEmpty()) {
			throw new IllegalArgumentException("invalid --header name: " + raw);
		}
		return new String[] { name, value };
	}

	static String[] splitKv(String raw, String label) {
		int idx = raw.indexOf('=');
		if (idx <= 0) {
			throw new IllegalArgumentException("invalid --" + label + ", expect key=value, got: " + raw);
		}
		String name = raw.substring(0, idx).trim();
		String value = raw.substring(idx + 1).trim();
		if (name.isEmpty()) {
			throw new IllegalArgumentException("invalid --" + label + " name: " + raw);
		}
		return new String[] { name, value };
	}

	public CapacityFinder toCapacityFinder(LoadConfig base) {
		return CapacityFinder.builder(base).startConcurrency(getInt("start", 10)).step(getInt("step", 10))
				.maxConcurrency(getInt("max-concurrency", 200))
				.minSuccessRatePercent(getDouble("min-success-rate", 99.0))
				.maxAllowedLatencyMillis(getLong("max-rt-ms", 3000))
				.stepDuration(Duration.ofSeconds(getLong("duration", 5))).build();
	}

	public static String usage() {
		return """
				用法:
				  # GET + Query
				  java -jar lind-qps-checker.jar --url http://localhost:8080/api/list \\
				       --param page=1 --param size=20 --concurrency 50 --duration 30

				  # POST JSON + Header
				  java -jar lind-qps-checker.jar --url http://localhost:8080/api/login --method POST \\
				       --header "Content-Type:application/json" \\
				       --header "Authorization:Bearer token" \\
				       --body "{\\"username\\":\\"admin\\",\\"password\\":\\"123\\"}" \\
				       --concurrency 20 --duration 10

				  # POST 表单
				  java -jar lind-qps-checker.jar --url http://localhost:8080/login --method POST \\
				       --form username=admin --form password=123

				  # 阶梯加压探测最大并发
				  java -jar lind-qps-checker.jar --find-max --url http://localhost:8080/api/ping \\
				       --start 10 --step 10 --max-concurrency 200 --duration 5

				参数:
				  --url                 必填，目标 URL
				  --method              HTTP 方法，默认 GET（支持 POST/PUT/PATCH/DELETE 等）
				  --body                原始请求体（常用于 JSON）；不可与 --form 同时使用
				  --header              请求头，可重复；格式 Name:Value 或 Name=Value
				                        也可一次写多个：Name:V1;Name2:V2
				  --param / --query     Query 参数，可重复；格式 key=value 或 a=1&b=2
				  --form                表单参数（POST x-www-form-urlencoded），可重复；格式 key=value
				  --concurrency         固定并发模式的并发数，默认 10
				  --duration            单周期时长（秒），默认 10；find-max 时为每阶梯时长，默认 5
				  --timeout-ms          单次请求超时毫秒，默认 5000
				  --find-max            启用最大并发探测
				  --start               探测起始并发，默认 10
				  --step                并发递增步长，默认 10
				  --max-concurrency     探测上限并发，默认 200
				  --min-success-rate    SLA 成功率百分比，默认 99
				  --max-rt-ms           SLA 最大响应时间毫秒，默认 3000
				  --help                显示帮助
				""";
	}

}

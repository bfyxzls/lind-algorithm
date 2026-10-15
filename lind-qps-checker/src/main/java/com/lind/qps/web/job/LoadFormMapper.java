package com.lind.qps.web.job;

import com.lind.qps.engine.LoadConfig;
import java.util.ArrayList;
import java.util.List;

/**
 * 将 Web 表单转为 {@link LoadConfig}。
 */
public final class LoadFormMapper {

	private LoadFormMapper() {
	}

	public static LoadConfig toLoadConfig(LoadForm form) {
		LoadConfig.Builder builder = LoadConfig.builder().url(requireUrl(form.getUrl()))
				.method(blankToDefault(form.getMethod(), "GET")).concurrency(form.getConcurrency())
				.durationSeconds(form.getDurationSeconds()).requestTimeoutMillis(form.getTimeoutMs());
		String body = form.getBody();
		if (body != null && !body.isBlank()) {
			builder.body(body.trim());
		}
		for (String header : splitLines(form.getHeadersText())) {
			String[] pair = splitHeader(header);
			builder.header(pair[0], pair[1]);
		}
		for (String param : splitLines(form.getQueryParamsText())) {
			String[] pair = splitKv(param, "param");
			builder.queryParam(pair[0], pair[1]);
		}
		for (String formParam : splitLines(form.getFormParamsText())) {
			String[] pair = splitKv(formParam, "form");
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
			throw new IllegalArgumentException("Header 格式错误，期望 Name:Value 或 Name=Value，实际: " + raw);
		}
		String name = raw.substring(0, idx).trim();
		String value = raw.substring(idx + 1).trim();
		if (name.isEmpty()) {
			throw new IllegalArgumentException("Header 名称为空: " + raw);
		}
		return new String[] { name, value };
	}

	static String[] splitKv(String raw, String label) {
		int idx = raw.indexOf('=');
		if (idx <= 0) {
			throw new IllegalArgumentException(label + " 格式错误，期望 key=value，实际: " + raw);
		}
		String name = raw.substring(0, idx).trim();
		String value = raw.substring(idx + 1).trim();
		if (name.isEmpty()) {
			throw new IllegalArgumentException(label + " 名称为空: " + raw);
		}
		return new String[] { name, value };
	}

	private static String requireUrl(String url) {
		if (url == null || url.isBlank()) {
			throw new IllegalArgumentException("目标 URL 不能为空");
		}
		return url.trim();
	}

	private static String blankToDefault(String value, String defaultValue) {
		return value == null || value.isBlank() ? defaultValue : value.trim();
	}

	static List<String> splitLines(String text) {
		if (text == null || text.isBlank()) {
			return List.of();
		}
		List<String> lines = new ArrayList<>();
		for (String line : text.split("\\R")) {
			String trimmed = line.trim();
			if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
				lines.add(trimmed);
			}
		}
		return lines;
	}

}

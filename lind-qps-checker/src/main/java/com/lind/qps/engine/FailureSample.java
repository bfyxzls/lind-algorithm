package com.lind.qps.engine;

import java.util.Objects;

/**
 * 压测失败样本：HTTP 状态码 + 响应体（截断）+ 错误信息。
 */
public record FailureSample(int statusCode, String responseBody, String errorMessage, long latencyMillis) {

	public static final int MAX_BODY_CHARS = 2048;

	public FailureSample {
		responseBody = truncate(responseBody);
		errorMessage = errorMessage == null ? "" : errorMessage;
	}

	public static FailureSample from(RequestResult result) {
		Objects.requireNonNull(result, "result");
		return new FailureSample(result.statusCode(), result.responseBody(), result.errorMessage(),
				result.latencyMillis());
	}

	static String truncate(String body) {
		if (body == null) {
			return "";
		}
		if (body.length() <= MAX_BODY_CHARS) {
			return body;
		}
		return body.substring(0, MAX_BODY_CHARS) + "...(truncated)";
	}

	@Override
	public String toString() {
		return "status=" + statusCode + ", latency=" + latencyMillis + "ms, error=" + errorMessage + ", body="
				+ responseBody;
	}

}

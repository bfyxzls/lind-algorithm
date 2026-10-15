package com.lind.qps.engine;

/**
 * 单次请求结果。
 */
public record RequestResult(boolean success, int statusCode, long latencyNanos, String errorMessage,
		String responseBody) {

	public static RequestResult ok(int statusCode, long latencyNanos) {
		return new RequestResult(true, statusCode, latencyNanos, null, null);
	}

	public static RequestResult fail(int statusCode, long latencyNanos, String errorMessage) {
		return fail(statusCode, latencyNanos, errorMessage, null);
	}

	public static RequestResult fail(int statusCode, long latencyNanos, String errorMessage, String responseBody) {
		return new RequestResult(false, statusCode, latencyNanos, errorMessage, FailureSample.truncate(responseBody));
	}

	public static RequestResult error(long latencyNanos, String errorMessage) {
		return new RequestResult(false, -1, latencyNanos, errorMessage, null);
	}

	public long latencyMillis() {
		return latencyNanos / 1_000_000L;
	}

}

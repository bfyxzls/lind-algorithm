package com.lind.qps;

/**
 * 单次请求结果。
 */
public record RequestResult(boolean success, int statusCode, long latencyNanos, String errorMessage) {

	public static RequestResult ok(int statusCode, long latencyNanos) {
		return new RequestResult(true, statusCode, latencyNanos, null);
	}

	public static RequestResult fail(int statusCode, long latencyNanos, String errorMessage) {
		return new RequestResult(false, statusCode, latencyNanos, errorMessage);
	}

	public static RequestResult error(long latencyNanos, String errorMessage) {
		return new RequestResult(false, -1, latencyNanos, errorMessage);
	}

	public long latencyMillis() {
		return latencyNanos / 1_000_000L;
	}

}

package com.lind.data.starter.doris;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Doris Stream Load HTTP 响应解析结果。
 */
public final class DorisStreamLoadResult {

	private static final Pattern STATUS = Pattern.compile("\"Status\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);

	private static final Pattern MESSAGE = Pattern.compile("\"Message\"\\s*:\\s*\"([^\"]*)\"",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern LABEL = Pattern.compile("\"Label\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);

	private static final Pattern TXN_ID = Pattern.compile("\"TxnId\"\\s*:\\s*(-?\\d+)", Pattern.CASE_INSENSITIVE);

	private final int httpStatus;

	private final String rawBody;

	private final String status;

	private final String message;

	private final String label;

	private final long txnId;

	public DorisStreamLoadResult(int httpStatus, String rawBody) {
		this.httpStatus = httpStatus;
		this.rawBody = rawBody == null ? "" : rawBody;
		this.status = extract(STATUS, this.rawBody).orElse("");
		this.message = extract(MESSAGE, this.rawBody).orElse("");
		this.label = extract(LABEL, this.rawBody).orElse("");
		this.txnId = extract(TXN_ID, this.rawBody).map(Long::parseLong).orElse(-1L);
	}

	/**
	 * Doris 约定：{@code Success} / {@code Publish Timeout} 视为成功。
	 */
	public boolean success() {
		String normalized = status.toLowerCase(Locale.ROOT);
		return "success".equals(normalized) || "publish timeout".equals(normalized);
	}

	public int httpStatus() {
		return httpStatus;
	}

	public String rawBody() {
		return rawBody;
	}

	public String status() {
		return status;
	}

	public String message() {
		return message;
	}

	public String label() {
		return label;
	}

	public long txnId() {
		return txnId;
	}

	private static Optional<String> extract(Pattern pattern, String body) {
		Matcher matcher = pattern.matcher(body);
		if (matcher.find()) {
			return Optional.ofNullable(matcher.group(1));
		}
		return Optional.empty();
	}

	@Override
	public String toString() {
		return "DorisStreamLoadResult{httpStatus=" + httpStatus + ", status='" + status + "', label='" + label
				+ "', txnId=" + txnId + ", message='" + message + "'}";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof DorisStreamLoadResult that)) {
			return false;
		}
		return httpStatus == that.httpStatus && Objects.equals(rawBody, that.rawBody);
	}

	@Override
	public int hashCode() {
		return Objects.hash(httpStatus, rawBody);
	}

}

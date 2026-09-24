package com.lind.qps;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 压测配置：目标 URL、方法、Header、Query/Form 参数、Body、并发与周期等。
 */
public final class LoadConfig {

	private final URI url;

	private final String method;

	private final Map<String, String> headers;

	private final Map<String, String> queryParams;

	private final Map<String, String> formParams;

	private final String body;

	private final int concurrency;

	private final Duration duration;

	private final Duration requestTimeout;

	private final int successStatusMin;

	private final int successStatusMax;

	private LoadConfig(Builder builder) {
		this.url = Objects.requireNonNull(builder.url, "url");
		this.method = builder.method == null ? "GET" : builder.method.toUpperCase();
		this.headers = Map.copyOf(builder.headers);
		this.queryParams = Map.copyOf(builder.queryParams);
		this.formParams = Map.copyOf(builder.formParams);
		this.body = builder.body;
		this.concurrency = builder.concurrency;
		this.duration = builder.duration;
		this.requestTimeout = builder.requestTimeout;
		this.successStatusMin = builder.successStatusMin;
		this.successStatusMax = builder.successStatusMax;
		if (concurrency <= 0) {
			throw new IllegalArgumentException("concurrency must be > 0");
		}
		if (duration.isZero() || duration.isNegative()) {
			throw new IllegalArgumentException("duration must be > 0");
		}
		if (requestTimeout.isZero() || requestTimeout.isNegative()) {
			throw new IllegalArgumentException("requestTimeout must be > 0");
		}
		if (!formParams.isEmpty() && body != null && !body.isEmpty()) {
			throw new IllegalArgumentException("cannot use both --form and --body");
		}
	}

	public URI url() {
		return url;
	}

	/**
	 * 拼接 query 参数后的最终请求 URI。
	 */
	public URI requestUri() {
		if (queryParams.isEmpty()) {
			return url;
		}
		String base = url.toString();
		String query = encodePairs(queryParams);
		if (url.getRawQuery() == null || url.getRawQuery().isEmpty()) {
			char joiner = base.contains("?") ? '&' : '?';
			return URI.create(base + joiner + query);
		}
		return URI.create(base + "&" + query);
	}

	public String method() {
		return method;
	}

	public Map<String, String> headers() {
		return headers;
	}

	public Map<String, String> queryParams() {
		return queryParams;
	}

	public Map<String, String> formParams() {
		return formParams;
	}

	public String body() {
		return body;
	}

	/**
	 * 实际发送的请求体：优先 form 编码，否则原始 body。
	 */
	public String requestBody() {
		if (!formParams.isEmpty()) {
			return encodePairs(formParams);
		}
		return body;
	}

	public boolean hasRequestBody() {
		if (!formParams.isEmpty()) {
			return true;
		}
		return body != null;
	}

	public boolean isFormRequest() {
		return !formParams.isEmpty();
	}

	public int concurrency() {
		return concurrency;
	}

	public Duration duration() {
		return duration;
	}

	public Duration requestTimeout() {
		return requestTimeout;
	}

	public boolean isSuccessStatus(int status) {
		return status >= successStatusMin && status <= successStatusMax;
	}

	public Builder toBuilder() {
		Builder b = new Builder().url(url).method(method).body(body).concurrency(concurrency).duration(duration)
				.requestTimeout(requestTimeout).successStatusRange(successStatusMin, successStatusMax);
		headers.forEach(b::header);
		queryParams.forEach(b::queryParam);
		formParams.forEach(b::formParam);
		return b;
	}

	public static Builder builder() {
		return new Builder();
	}

	static String encodePairs(Map<String, String> pairs) {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> entry : pairs.entrySet()) {
			if (sb.length() > 0) {
				sb.append('&');
			}
			sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
			sb.append('=');
			sb.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
		}
		return sb.toString();
	}

	public static final class Builder {

		private URI url;

		private String method = "GET";

		private final Map<String, String> headers = new LinkedHashMap<>();

		private final Map<String, String> queryParams = new LinkedHashMap<>();

		private final Map<String, String> formParams = new LinkedHashMap<>();

		private String body;

		private int concurrency = 10;

		private Duration duration = Duration.ofSeconds(10);

		private Duration requestTimeout = Duration.ofSeconds(5);

		private int successStatusMin = 200;

		private int successStatusMax = 299;

		public Builder url(String url) {
			this.url = URI.create(Objects.requireNonNull(url, "url"));
			return this;
		}

		public Builder url(URI url) {
			this.url = Objects.requireNonNull(url, "url");
			return this;
		}

		public Builder method(String method) {
			this.method = method;
			return this;
		}

		public Builder header(String name, String value) {
			this.headers.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(value, "value"));
			return this;
		}

		public Builder headers(Map<String, String> headers) {
			Objects.requireNonNull(headers, "headers").forEach(this::header);
			return this;
		}

		public Builder queryParam(String name, String value) {
			this.queryParams.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(value, "value"));
			return this;
		}

		public Builder queryParams(Map<String, String> params) {
			Objects.requireNonNull(params, "params").forEach(this::queryParam);
			return this;
		}

		public Builder formParam(String name, String value) {
			this.formParams.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(value, "value"));
			return this;
		}

		public Builder formParams(Map<String, String> params) {
			Objects.requireNonNull(params, "params").forEach(this::formParam);
			return this;
		}

		public Builder body(String body) {
			this.body = body;
			return this;
		}

		public Builder concurrency(int concurrency) {
			this.concurrency = concurrency;
			return this;
		}

		public Builder duration(Duration duration) {
			this.duration = Objects.requireNonNull(duration, "duration");
			return this;
		}

		public Builder durationSeconds(long seconds) {
			this.duration = Duration.ofSeconds(seconds);
			return this;
		}

		public Builder requestTimeout(Duration requestTimeout) {
			this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout");
			return this;
		}

		public Builder requestTimeoutMillis(long millis) {
			this.requestTimeout = Duration.ofMillis(millis);
			return this;
		}

		public Builder successStatusRange(int minInclusive, int maxInclusive) {
			this.successStatusMin = minInclusive;
			this.successStatusMax = maxInclusive;
			return this;
		}

		public LoadConfig build() {
			return new LoadConfig(this);
		}

	}

}

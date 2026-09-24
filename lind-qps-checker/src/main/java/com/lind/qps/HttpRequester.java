package com.lind.qps;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 基于 JDK {@link HttpClient} 的单次 HTTP 探测，支持 GET/POST 等、Header、Query、Form、JSON Body。
 */
public final class HttpRequester {

	private final HttpClient client;

	private final LoadConfig config;

	public HttpRequester(LoadConfig config) {
		this.config = Objects.requireNonNull(config, "config");
		this.client = HttpClient.newBuilder().connectTimeout(config.requestTimeout())
				.followRedirects(HttpClient.Redirect.NORMAL).build();
	}

	HttpRequester(LoadConfig config, HttpClient client) {
		this.config = Objects.requireNonNull(config, "config");
		this.client = Objects.requireNonNull(client, "client");
	}

	public RequestResult execute() {
		long start = System.nanoTime();
		try {
			HttpRequest.Builder builder = HttpRequest.newBuilder(config.requestUri()).timeout(config.requestTimeout());
			applyHeaders(builder);
			String method = config.method();
			if (usesBody(method) && config.hasRequestBody()) {
				String body = config.requestBody() == null ? "" : config.requestBody();
				builder.method(method, HttpRequest.BodyPublishers.ofString(body));
				ensureContentType(builder);
			}
			else {
				builder.method(method, HttpRequest.BodyPublishers.noBody());
			}
			HttpResponse<Void> response = client.send(builder.build(), HttpResponse.BodyHandlers.discarding());
			long latency = System.nanoTime() - start;
			int status = response.statusCode();
			if (config.isSuccessStatus(status)) {
				return RequestResult.ok(status, latency);
			}
			return RequestResult.fail(status, latency, "unexpected status " + status);
		}
		catch (IOException | InterruptedException | RuntimeException ex) {
			if (ex instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			long latency = System.nanoTime() - start;
			return RequestResult.error(latency, ex.getClass().getSimpleName() + ": " + ex.getMessage());
		}
	}

	private void applyHeaders(HttpRequest.Builder builder) {
		for (Map.Entry<String, String> entry : config.headers().entrySet()) {
			builder.header(entry.getKey(), entry.getValue());
		}
	}

	private void ensureContentType(HttpRequest.Builder builder) {
		if (hasHeaderIgnoreCase("Content-Type")) {
			return;
		}
		if (config.isFormRequest()) {
			builder.header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		}
		else {
			builder.header("Content-Type", "application/json; charset=UTF-8");
		}
	}

	private boolean hasHeaderIgnoreCase(String name) {
		for (String key : config.headers().keySet()) {
			if (key.equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}

	private static boolean usesBody(String method) {
		return switch (method.toUpperCase(Locale.ROOT)) {
			case "POST", "PUT", "PATCH" -> true;
			default -> false;
		};
	}

	public Duration requestTimeout() {
		return config.requestTimeout();
	}

}

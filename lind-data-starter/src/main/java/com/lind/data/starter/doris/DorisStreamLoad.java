package com.lind.data.starter.doris;

import com.lind.data.starter.LindDataProperties;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

/**
 * Doris FE HTTP Stream Load：批量写入 CSV / JSON。
 * <p>
 * 请求路径：{@code PUT http://{fe}:{httpPort}/api/{db}/{table}/_stream_load}
 * <p>
 * 使用 {@link HttpURLConnection} 以便设置 Doris 所需的 {@code Expect: 100-continue}（JDK
 * {@code HttpClient} 默认禁止该受限头）。
 */
public class DorisStreamLoad {

	public static final String FORMAT_CSV = "csv";

	public static final String FORMAT_JSON = "json";

	@FunctionalInterface
	interface ConnectionFactory {

		HttpURLConnection open(URL url) throws IOException;

	}

	private final LindDataProperties.Doris config;

	private final ConnectionFactory connectionFactory;

	public DorisStreamLoad(LindDataProperties.Doris config) {
		this(config, url -> (HttpURLConnection) url.openConnection());
	}

	/**
	 * 测试可注入连接工厂。
	 */
	DorisStreamLoad(LindDataProperties.Doris config, ConnectionFactory connectionFactory) {
		this.config = Objects.requireNonNull(config, "config");
		this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory");
	}

	public DorisStreamLoadResult loadCsv(String table, String csvBody) {
		return load(table, csvBody.getBytes(StandardCharsets.UTF_8), FORMAT_CSV, null);
	}

	public DorisStreamLoadResult loadJson(String table, String jsonBody) {
		return load(table, jsonBody.getBytes(StandardCharsets.UTF_8), FORMAT_JSON, null);
	}

	/**
	 * @param table 表名
	 * @param payload 原始字节
	 * @param format {@link #FORMAT_CSV} / {@link #FORMAT_JSON}
	 * @param label 可选；为空时自动生成 {@code labelPrefix-uuid}
	 */
	public DorisStreamLoadResult load(String table, byte[] payload, String format, String label) {
		Objects.requireNonNull(table, "table");
		if (table.isBlank()) {
			throw new IllegalArgumentException("table must not be blank");
		}
		Objects.requireNonNull(payload, "payload");
		Objects.requireNonNull(format, "format");
		String resolvedLabel = (label == null || label.isBlank()) ? nextLabel() : label;
		String urlText = String.format("http://%s:%d/api/%s/%s/_stream_load", config.getFeHttpHost(),
				config.getFeHttpPort(), config.getDatabase(), table);
		HttpURLConnection connection = null;
		try {
			URL url = URI.create(urlText).toURL();
			connection = connectionFactory.open(url);
			connection.setConnectTimeout(Math.max(5_000, config.getTimeoutSeconds() * 100));
			connection.setReadTimeout(Math.max(5_000, config.getTimeoutSeconds() * 1000));
			connection.setDoOutput(true);
			connection.setRequestMethod("PUT");
			connection.setRequestProperty("Authorization", basicAuth(config.getUsername(), config.getPassword()));
			connection.setRequestProperty("Expect", "100-continue");
			connection.setRequestProperty("format", format.toLowerCase());
			connection.setRequestProperty("label", resolvedLabel);
			connection.setRequestProperty("max_filter_ratio", String.valueOf(config.getMaxFilterRatio()));
			if (FORMAT_JSON.equalsIgnoreCase(format)) {
				connection.setRequestProperty("strip_outer_array", "true");
				connection.setRequestProperty("Content-Type", "application/json");
			}
			else {
				connection.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");
			}
			try (OutputStream out = connection.getOutputStream()) {
				out.write(payload);
				out.flush();
			}
			int status = connection.getResponseCode();
			InputStream bodyStream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
			String body = readBody(bodyStream);
			return new DorisStreamLoadResult(status, body);
		}
		catch (IOException e) {
			throw new IllegalStateException("Doris Stream Load I/O failed: " + urlText, e);
		}
		finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	public LindDataProperties.Doris config() {
		return config;
	}

	private String nextLabel() {
		String prefix = config.getLabelPrefix() == null || config.getLabelPrefix().isBlank() ? "lind"
				: config.getLabelPrefix();
		return prefix + "-" + UUID.randomUUID();
	}

	private static String basicAuth(String username, String password) {
		String raw = (username == null ? "" : username) + ":" + (password == null ? "" : password);
		return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
	}

	private static String readBody(InputStream stream) throws IOException {
		if (stream == null) {
			return "";
		}
		try (InputStream in = stream) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

}

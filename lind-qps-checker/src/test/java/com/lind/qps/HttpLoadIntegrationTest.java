package com.lind.qps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

/**
 * 真实 HTTP 本地服务压测。
 */
public class HttpLoadIntegrationTest {

	private HttpServer server;

	private String baseUrl;

	private final AtomicReference<String> lastMethod = new AtomicReference<>();

	private final AtomicReference<String> lastQuery = new AtomicReference<>();

	private final AtomicReference<String> lastAuth = new AtomicReference<>();

	private final AtomicReference<String> lastBody = new AtomicReference<>();

	private final AtomicReference<String> lastContentType = new AtomicReference<>();

	@BeforeEach
	void setUp() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/ping", exchange -> {
			lastMethod.set(exchange.getRequestMethod());
			lastQuery.set(exchange.getRequestURI().getRawQuery());
			lastAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
			lastContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
			lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(body);
			}
		});
		server.setExecutor(Executors.newCachedThreadPool());
		server.start();
		baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/ping";
	}

	@AfterEach
	void tearDown() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	void fixedConcurrencyCycle() {
		LoadConfig config = LoadConfig.builder().url(baseUrl).concurrency(8).duration(Duration.ofMillis(300))
				.requestTimeout(Duration.ofSeconds(2)).build();
		LoadReport report = new LoadRunner(config).run();
		assertTrue(report.totalRequests() > 0);
		assertTrue(report.successRate() >= 99.0, report.format());
		assertEquals(8, report.concurrency());
	}

	@Test
	void postJsonWithHeaderAndQuery() {
		LoadConfig config = LoadConfig.builder().url(baseUrl).method("POST").header("Authorization", "Bearer tok")
				.queryParam("from", "test").body("{\"name\":\"lind\"}").concurrency(1).duration(Duration.ofMillis(100))
				.requestTimeout(Duration.ofSeconds(2)).build();
		RequestResult result = new HttpRequester(config).execute();
		assertTrue(result.success(), result.errorMessage());
		assertEquals("POST", lastMethod.get());
		assertEquals("from=test", lastQuery.get());
		assertEquals("Bearer tok", lastAuth.get());
		assertEquals("{\"name\":\"lind\"}", lastBody.get());
		assertTrue(lastContentType.get().startsWith("application/json"));
	}

	@Test
	void postFormParams() {
		LoadConfig config = LoadConfig.builder().url(baseUrl).method("POST").formParam("username", "admin")
				.formParam("password", "123").concurrency(1).duration(Duration.ofMillis(100))
				.requestTimeout(Duration.ofSeconds(2)).build();
		RequestResult result = new HttpRequester(config).execute();
		assertTrue(result.success(), result.errorMessage());
		assertEquals("username=admin&password=123", lastBody.get());
		assertTrue(lastContentType.get().contains("application/x-www-form-urlencoded"));
	}

	@Test
	void mainPostMode() {
		int code = QpsCheckerMain.run(new String[] { "--url", baseUrl, "--method", "POST", "--header", "X-Test:1",
				"--param", "q=1", "--body", "{}", "--concurrency", "2", "--duration", "1" });
		assertEquals(0, code);
		assertEquals("POST", lastMethod.get());
	}

}

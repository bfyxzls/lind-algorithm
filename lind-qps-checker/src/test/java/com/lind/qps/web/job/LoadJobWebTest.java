package com.lind.qps.web.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class LoadJobWebTest {

	private static HttpServer server;

	private static String baseUrl;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private LoadJobService loadJobService;

	@BeforeAll
	static void startServer() throws Exception {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/ping", exchange -> {
			byte[] body = "pong".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(body);
			}
		});
		server.setExecutor(Executors.newCachedThreadPool());
		server.start();
		baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/ping";
	}

	@AfterAll
	static void stopServer() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	void indexPageRenders() throws Exception {
		mockMvc.perform(get("/")).andExpect(status().isOk()).andExpect(view().name("index"));
	}

	@Test
	void submitFixedJobAndPollApi() throws Exception {
		MvcResult result = mockMvc
				.perform(post("/jobs").param("url", baseUrl).param("method", "GET").param("mode", "FIXED")
						.param("concurrency", "2").param("durationSeconds", "1").param("timeoutMs", "2000"))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/jobs/[a-f0-9]+")))
				.andReturn();
		String location = result.getResponse().getHeader("Location");
		assertThat(location).isNotBlank();
		String id = location.substring("/jobs/".length());

		awaitDone(id);

		mockMvc.perform(get("/api/jobs/" + id)).andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SUCCESS")).andExpect(jsonPath("$.report.qps").isNumber());

		mockMvc.perform(get("/jobs/" + id)).andExpect(status().isOk()).andExpect(view().name("job"));
		LoadJob persisted = loadJobService.find(id).orElseThrow();
		assertThat(persisted.status()).isEqualTo(LoadJobStatus.SUCCESS);
		assertThat(persisted.report()).isNotNull();
	}

	private void awaitDone(String id) throws InterruptedException {
		long deadline = System.currentTimeMillis() + 15_000;
		while (System.currentTimeMillis() < deadline) {
			LoadJob job = loadJobService.find(id).orElseThrow();
			if (job.status() == LoadJobStatus.SUCCESS || job.status() == LoadJobStatus.FAILED) {
				assertThat(job.status()).isEqualTo(LoadJobStatus.SUCCESS);
				return;
			}
			Thread.sleep(100);
		}
		throw new AssertionError("job did not finish: " + id);
	}

}

package com.lind.stream.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

/**
 * 流式接口集成测试。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ChatControllerTest {

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void openAiStyleStreamEndsWithDone() {
		webTestClient = webTestClient.mutate().responseTimeout(Duration.ofSeconds(30)).build();

		webTestClient.post().uri("/v1/chat/completions").contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.TEXT_EVENT_STREAM).bodyValue("""
						{
						  "model": "lind-demo",
						  "stream": true,
						  "messages": [{"role":"user","content":"hi"}]
						}
						""").exchange().expectStatus().isOk().expectHeader()
				.contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM).returnResult(String.class).getResponseBody()
				.as(StepVerifier::create).thenConsumeWhile(chunk -> !chunk.contains("[DONE]"))
				.expectNextMatches(chunk -> chunk.contains("[DONE]")).verifyComplete();
	}

	@Test
	void simpleGetStreamReturnsTokens() {
		webTestClient = webTestClient.mutate().responseTimeout(Duration.ofSeconds(30)).build();

		String body = webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path("/api/chat/stream").queryParam("q", "SSE").build())
				.accept(MediaType.TEXT_EVENT_STREAM).exchange().expectStatus().isOk().expectBody(String.class)
				.returnResult().getResponseBody();

		assertThat(body).isNotBlank();
		assertThat(body).contains("data:");
		assertThat(body).contains("[DONE]");
	}

	@Test
	void nonStreamReturnsJson() {
		webTestClient.post().uri("/v1/chat/completions").contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON).bodyValue("""
						{
						  "model": "lind-demo",
						  "stream": false,
						  "messages": [{"role":"user","content":"hello"}]
						}
						""").exchange().expectStatus().isOk().expectHeader()
				.contentTypeCompatibleWith(MediaType.APPLICATION_JSON).expectBody()
				.jsonPath("$.choices[0].message.role").isEqualTo("assistant").jsonPath("$.choices[0].message.content")
				.isNotEmpty();
	}

	@Test
	void tokenizeKeepsReadableChunks() {
		List<String> tokens = ChatCompletionService.tokenize("你好，世界");
		assertThat(tokens).isNotEmpty();
		assertThat(String.join("", tokens)).isEqualTo("你好，世界");
	}

	@Test
	void coalesceFalseEmitsMoreTokenEventsThanDefault() {
		webTestClient = webTestClient.mutate().responseTimeout(Duration.ofSeconds(30)).build();

		String coalesced = webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path("/api/chat/stream").queryParam("q", "对比攒包")
						.queryParam("coalesce", "true").build())
				.accept(MediaType.TEXT_EVENT_STREAM).exchange().expectStatus().isOk().expectBody(String.class)
				.returnResult().getResponseBody();

		String raw = webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path("/api/chat/stream").queryParam("q", "对比攒包")
						.queryParam("coalesce", "false").build())
				.accept(MediaType.TEXT_EVENT_STREAM).exchange().expectStatus().isOk().expectBody(String.class)
				.returnResult().getResponseBody();

		assertThat(coalesced).contains("[DONE]");
		assertThat(raw).contains("[DONE]");
		long coalescedEvents = coalesced.lines().filter(l -> l.startsWith("data:") && !l.contains("[DONE]")).count();
		long rawEvents = raw.lines().filter(l -> l.startsWith("data:") && !l.contains("[DONE]")).count();
		assertThat(rawEvents).isGreaterThan(coalescedEvents);
	}

}

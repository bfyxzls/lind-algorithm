package com.lind.stream.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 流式聊天接口：{@code text/event-stream} 逐步返回。
 */
@RestController
@RequestMapping
@CrossOrigin
public class ChatController {

	private final ChatCompletionService chatCompletionService;

	private final ObjectMapper objectMapper;

	public ChatController(ChatCompletionService chatCompletionService, ObjectMapper objectMapper) {
		this.chatCompletionService = chatCompletionService;
		this.objectMapper = objectMapper;
	}

	/**
	 * OpenAI 风格：POST /v1/chat/completions，stream=true 时返回 SSE。
	 */
	@PostMapping(value = "/v1/chat/completions",
			produces = { MediaType.TEXT_EVENT_STREAM_VALUE, MediaType.APPLICATION_JSON_VALUE })
	public Object chatCompletions(@RequestBody ChatCompletionRequest request) {
		if (!request.streaming()) {
			return Mono.just(chatCompletionService.complete(request));
		}
		return chatCompletionService.streamChat(request).map(this::toJson).concatWith(Mono.just("[DONE]"))
				.map(data -> ServerSentEvent.builder(data).build());
	}

	/**
	 * 简易演示：GET /api/chat/stream?q=你好
	 */
	@GetMapping(value = "/api/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> streamGet(@RequestParam(name = "q", defaultValue = "") String question) {
		return chatCompletionService.streamTokens(question)
				.map(token -> ServerSentEvent.builder(token).event("token").build())
				.concatWith(Mono.just(ServerSentEvent.builder("[DONE]").event("done").build()));
	}

	/**
	 * 简易演示：POST /api/chat/stream，body 为纯文本问题。
	 */
	@PostMapping(value = "/api/chat/stream", consumes = MediaType.TEXT_PLAIN_VALUE,
			produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> streamPost(@RequestBody Mono<String> body) {
		return body.defaultIfEmpty("")
				.flatMapMany(question -> chatCompletionService.streamTokens(question)
						.map(token -> ServerSentEvent.builder(token).event("token").build())
						.concatWith(Mono.just(ServerSentEvent.builder("[DONE]").event("done").build())));
	}

	/**
	 * 心跳保活示例（可选）：GET /api/chat/ping-stream
	 */
	@GetMapping(value = "/api/chat/ping-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> pingStream() {
		return Flux.interval(Duration.ofSeconds(1)).take(5)
				.map(i -> ServerSentEvent.builder("ping-" + i).event("ping").build());
	}

	private String toJson(ChatCompletionChunk chunk) {
		try {
			return objectMapper.writeValueAsString(chunk);
		}
		catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}

}

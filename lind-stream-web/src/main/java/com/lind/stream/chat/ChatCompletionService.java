package com.lind.stream.chat;

import com.lind.stream.buffer.CoalescingStringFlux;
import com.lind.stream.buffer.SseCoalesceProperties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 模拟大模型逐 token 生成，再按策略攒包后推送，减少 SSE 小事件风暴。
 */
@Service
public class ChatCompletionService {

	private static final Logger log = LoggerFactory.getLogger(ChatCompletionService.class);

	/** 模拟上游吐 token 的间隔；与 coalesce.max-delay 一起决定「时间窗会不会先于字符阈值触发」。 */
	private static final Duration TOKEN_INTERVAL = Duration.ofMillis(40);

	private final SseCoalesceProperties coalesceProperties;

	public ChatCompletionService(SseCoalesceProperties coalesceProperties) {
		this.coalesceProperties = coalesceProperties;
		log.info("SSE coalesce effective config: {} (tokenInterval={})", coalesceProperties, TOKEN_INTERVAL);
	}

	public String generateReply(String userContent) {
		String prompt = userContent == null ? "" : userContent.trim();
		if (prompt.isEmpty()) {
			return "你好，我是 lind-stream-web 演示助手。请输入问题，我会以 text/event-stream 逐步返回。";
		}
		return """
				收到你的问题：「%s」

				这是一段流式演示回复：后端通过 Server-Sent Events（Content-Type: text/event-stream）
				把内容拆成多个 data 事件逐步推送给前端，体验类似大模型接口的打字机效果。

				你也可以调用 POST /v1/chat/completions，并设置 "stream": true。
				""".formatted(prompt).strip();
	}

	/**
	 * 原始细粒度 token（含模拟生成延迟），再经 {@link CoalescingStringFlux} 攒包。
	 */
	public Flux<String> streamTokens(String userContent) {
		String full = generateReply(userContent);
		List<String> tokens = tokenize(full);
		Flux<String> raw = Flux.fromIterable(tokens).delayElements(TOKEN_INTERVAL);
		return CoalescingStringFlux.coalesce(raw, coalesceProperties);
	}

	/** 不攒包：对照旧行为 / 压测用。 */
	public Flux<String> streamTokensRaw(String userContent) {
		String full = generateReply(userContent);
		return Flux.fromIterable(tokenize(full)).delayElements(TOKEN_INTERVAL);
	}

	public Flux<ChatCompletionChunk> streamChat(ChatCompletionRequest request) {
		String id = "chatcmpl-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
		long created = System.currentTimeMillis() / 1000;
		String model = request.model() == null || request.model().isBlank() ? "lind-demo" : request.model();

		ChatCompletionChunk first = new ChatCompletionChunk(id, "chat.completion.chunk", created, model,
				List.of(new ChatCompletionChunk.Choice(0, ChatCompletionChunk.Delta.roleAssistant(), null)));

		AtomicInteger index = new AtomicInteger();
		Flux<ChatCompletionChunk> content = streamTokens(request.lastUserContent())
				.map(token -> new ChatCompletionChunk(id, "chat.completion.chunk", created, model,
						List.of(new ChatCompletionChunk.Choice(0, ChatCompletionChunk.Delta.content(token), null))));

		ChatCompletionChunk last = new ChatCompletionChunk(id, "chat.completion.chunk", created, model,
				List.of(new ChatCompletionChunk.Choice(0, new ChatCompletionChunk.Delta(null, null), "stop")));

		return Flux.concat(Flux.just(first), content, Flux.just(last)).doOnNext(c -> index.incrementAndGet());
	}

	public ChatCompletionResponse complete(ChatCompletionRequest request) {
		String id = "chatcmpl-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
		long created = System.currentTimeMillis() / 1000;
		String model = request.model() == null || request.model().isBlank() ? "lind-demo" : request.model();
		String content = generateReply(request.lastUserContent());
		return new ChatCompletionResponse(id, "chat.completion", created, model,
				List.of(new ChatCompletionResponse.Choice(0, new ChatMessage("assistant", content), "stop")));
	}

	static List<String> tokenize(String text) {
		List<String> tokens = new ArrayList<>();
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < text.length();) {
			int cp = text.codePointAt(i);
			buf.appendCodePoint(cp);
			i += Character.charCount(cp);
			boolean flush = Character.isWhitespace(cp) || "，。！？；：、,.!?;:\n".indexOf(cp) >= 0 || buf.length() >= 2;
			if (flush) {
				tokens.add(buf.toString());
				buf.setLength(0);
			}
		}
		if (!buf.isEmpty()) {
			tokens.add(buf.toString());
		}
		return tokens;
	}

}

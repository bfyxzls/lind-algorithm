package com.lind.stream.chat;

import java.util.List;

/**
 * 非流式完整响应。
 */
public record ChatCompletionResponse(
		String id,
		String object,
		long created,
		String model,
		List<Choice> choices) {

	public record Choice(int index, ChatMessage message, String finishReason) {
	}

}

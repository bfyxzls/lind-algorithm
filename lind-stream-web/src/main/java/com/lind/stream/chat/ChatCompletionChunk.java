package com.lind.stream.chat;

import java.util.List;

/**
 * 流式 chunk（对齐 OpenAI chat.completion.chunk 精简结构）。
 */
public record ChatCompletionChunk(
		String id,
		String object,
		long created,
		String model,
		List<Choice> choices) {

	public record Choice(int index, Delta delta, String finishReason) {
	}

	public record Delta(String role, String content) {

		public static Delta content(String content) {
			return new Delta(null, content);
		}

		public static Delta roleAssistant() {
			return new Delta("assistant", null);
		}

	}

}

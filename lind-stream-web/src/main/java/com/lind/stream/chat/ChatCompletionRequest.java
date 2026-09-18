package com.lind.stream.chat;

import java.util.List;

/**
 * 聊天请求（对齐常见大模型 API 的精简字段）。
 */
public record ChatCompletionRequest(String model, List<ChatMessage> messages, Boolean stream, Double temperature) {

	public ChatCompletionRequest {
		if (messages == null || messages.isEmpty()) {
			throw new IllegalArgumentException("messages must not be empty");
		}
	}

	public boolean streaming() {
		return stream == null || stream;
	}

	public String lastUserContent() {
		for (int i = messages.size() - 1; i >= 0; i--) {
			ChatMessage message = messages.get(i);
			if ("user".equalsIgnoreCase(message.role()) && message.content() != null) {
				return message.content();
			}
		}
		return messages.get(messages.size() - 1).content();
	}

}

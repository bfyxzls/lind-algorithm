package com.lind.stream.chat;

/**
 * 单条对话消息。
 */
public record ChatMessage(String role, String content) {
}

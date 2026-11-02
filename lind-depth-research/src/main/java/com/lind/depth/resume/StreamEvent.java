package com.lind.depth.resume;

import java.util.Objects;

/**
 * 一条可续传的流式事件：稳定 {@code id} + 文本载荷。
 *
 * <p>
 * 对应 SSE 里的 {@code id:} 字段；客户端重连时通过 {@code Last-Event-ID} 带回最后一个已收 id。
 */
public record StreamEvent(long id, String data) {

	public StreamEvent {
		if (id < 0) {
			throw new IllegalArgumentException("id 不能为负");
		}
		Objects.requireNonNull(data, "data");
	}

}

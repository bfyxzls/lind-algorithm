package com.lind.depth.resume;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <b>Redis Stream 的内存替身</b>：按递增 id 追加事件，支持从断点之后续读。
 *
 * <p>
 * 对应文档：「生成内容写进 Stream（带 message ID）」；客户端重连带 {@code Last-Event-ID} 后，
 * {@link #readAfter(long)} 返回 id 严格大于该值的后续事件。
 *
 * <p>
 * 生产替换：Redis {@code XADD} / {@code XREAD}，语义保持不变。
 */
public final class InMemoryEventStream {

	private final AtomicLong seq = new AtomicLong(0);

	private final List<StreamEvent> events = new CopyOnWriteArrayList<>();

	/**
	 * 追加一条事件，返回分配的稳定 id（从 1 递增）。
	 */
	public StreamEvent append(String data) {
		Objects.requireNonNull(data, "data");
		long id = seq.incrementAndGet();
		StreamEvent event = new StreamEvent(id, data);
		events.add(event);
		return event;
	}

	/**
	 * 读取 id &gt; {@code lastEventId} 的所有事件（不含 lastEventId 本身）。
	 * <p>
	 * SSE 语义：客户端已确认收到 lastEventId，服务端从下一条续推。
	 */
	public List<StreamEvent> readAfter(long lastEventId) {
		if (lastEventId < 0) {
			throw new IllegalArgumentException("lastEventId 不能为负");
		}
		List<StreamEvent> out = new ArrayList<>();
		for (StreamEvent e : events) {
			if (e.id() > lastEventId) {
				out.add(e);
			}
		}
		return List.copyOf(out);
	}

	public long latestId() {
		return seq.get();
	}

	public int size() {
		return events.size();
	}

}

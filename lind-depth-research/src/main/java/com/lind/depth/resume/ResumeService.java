package com.lind.depth.resume;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * 断点续推服务：根据客户端 {@code Last-Event-ID} 从 Stream 精确续读。
 *
 * <p>
 * 对应文档：
 *
 * <pre>
 * 客户端断开前收到 id=100
 * 重连带上 Last-Event-ID: 100
 * 服务端从 offset 101 开始推
 * </pre>
 *
 * WAL 用于运维/对账（知道会话进度）；<b>续传内容以 Stream 为准</b>。
 * WAL（Write-Ahead Logging，预写日志，先写日志，再写数据）
 */
public final class ResumeService {

	private final FileWal wal;

	private final InMemoryEventStream stream;

	public ResumeService(FileWal wal, InMemoryEventStream stream) {
		this.wal = Objects.requireNonNull(wal, "wal");
		this.stream = Objects.requireNonNull(stream, "stream");
	}

	/**
	 * @param sessionId 会话
	 * @param lastEventId 客户端已确认的最后 id；若从未收到过可传 0
	 * @return id &gt; lastEventId 的事件列表（可能为空，表示已追上）
	 */
	public List<StreamEvent> resumeAfter(String sessionId, long lastEventId) throws IOException {
		Objects.requireNonNull(sessionId, "sessionId");
		// 可选：与 WAL 对账，发现客户端声称的进度超过服务端则拒绝（防伪造）
		long walId = wal.lastEventId(sessionId).orElse(0L);
		if (lastEventId > walId) {
			throw new IllegalArgumentException("Last-Event-ID=" + lastEventId + " 超过服务端 WAL 进度 " + walId);
		}
		return stream.readAfter(lastEventId);
	}

}

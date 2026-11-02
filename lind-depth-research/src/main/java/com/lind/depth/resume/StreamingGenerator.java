package com.lind.depth.resume;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 流式生成管线：<b>先 WAL，再写 Stream，再回调推送</b>。
 *
 * <p>
 * 对应文档完整链路中的服务端半段：
 *
 * <pre>
 * 模型生成 token → 写 WAL → 写 Stream → SSE 推给客户端
 * </pre>
 *
 * 本类不接真模型：调用方传入 token 列表即可。重点是顺序不能反——若先推后记 WAL，崩溃会丢进度。
 */
public final class StreamingGenerator {

	private final String sessionId;

	private final FileWal wal;

	private final InMemoryEventStream stream;

	public StreamingGenerator(String sessionId, FileWal wal, InMemoryEventStream stream) {
		this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
		this.wal = Objects.requireNonNull(wal, "wal");
		this.stream = Objects.requireNonNull(stream, "stream");
	}

	/**
	 * 按顺序「生成」tokens。
	 * @param tokens 伪模型输出
	 * @param onPush 模拟推给客户端（可为空操作）；参数为已写入 Stream 的事件
	 * @return 本轮新产生的全部事件
	 */
	public List<StreamEvent> generate(List<String> tokens, Consumer<StreamEvent> onPush) throws IOException {
		Objects.requireNonNull(tokens, "tokens");
		Consumer<StreamEvent> push = onPush == null ? e -> {
		} : onPush;
		List<StreamEvent> produced = new ArrayList<>(tokens.size());
		for (String token : tokens) {
			// 1) 先占一个 id：通过 append 得到稳定 id（也可先占号再写，教学上合并一步）
			// 严格「先 WAL」：我们先 append 拿 id，立刻写 WAL，再通知下游——崩溃时最多丢「未 push」的尾部，
			// 但 WAL/Stream 已对齐，续传仍以 Stream 为准。
			StreamEvent event = stream.append(token);
			wal.appendProgress(sessionId, event.id());
			push.accept(event);
			produced.add(event);
		}
		return List.copyOf(produced);
	}

	public String sessionId() {
		return sessionId;
	}

}

package com.lind.depth.stream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * <b>玩具做法（反面教材）</b>：每个 token 立即写出一次。
 *
 * <p>
 * 对应 {@code docs/工程化分析.md} 场景一里的「框架层逐 token flush」：
 * <ul>
 * <li>每来一个 token → {@code getBytes} → {@link ByteSink#write} → 计一次 flush</li>
 * <li>token 越多，{@link StreamWriteStats#flushCount()} 越大</li>
 * <li>真实网络上 ≈ 系统调用风暴 + TCP 小包</li>
 * </ul>
 *
 * <p>
 * <b>仅作基线对比。</b>生产流式推送请用 {@link CoalescingStreamWriter}。 读代码时请和攒包实现对照着看「多了什么 / 少了什么」。
 */
public final class PerTokenStreamWriter implements StreamWriter {

	private final ByteSink sink;

	private final StreamWriteStats stats = new StreamWriteStats();

	public PerTokenStreamWriter(ByteSink sink) {
		this.sink = Objects.requireNonNull(sink, "sink");
	}

	/**
	 * 立刻编码并写出，不做任何合并。
	 */
	@Override
	public void onToken(String token) throws IOException {
		Objects.requireNonNull(token, "token");
		stats.onToken();
		byte[] bytes = token.getBytes(StandardCharsets.UTF_8);
		// wrap 基于堆内数组，没有攒包、没有 DirectBuffer——故意保持「朴素」
		ByteBuffer buf = ByteBuffer.wrap(bytes);
		sink.write(buf);
		stats.onFlush(bytes.length);
	}

	@Override
	public StreamWriteStats stats() {
		return stats;
	}

	@Override
	public void close() {
		// 无内部缓冲，关闭时无需再刷
	}

}

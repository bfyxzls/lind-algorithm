package com.lind.depth.stream;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 流式写出统计。
 *
 * <p>
 * <b>研究里最重要的指标是 {@link #flushCount()}</b>： 每次真正调用 {@link ByteSink#write}（刷出一块缓冲）计一次。 在真实
 * Socket 上，这近似于「系统调用 / 网络写出次数」——文档里说的 CPU 被拷贝和上下文切换吃满，往往就体现在这个数字过大。
 *
 * <ul>
 * <li>{@link #tokenCount()} —— 收到多少个 token（输入侧）</li>
 * <li>{@link #flushCount()} —— 刷了多少次（成本侧）</li>
 * <li>{@link #bytesWritten()} —— 写出总字节（正确性：两种 Writer 应相同）</li>
 * </ul>
 *
 * <p>
 * 包内方法 {@code onToken} / {@code onFlush} 仅供 Writer 更新计数，业务代码只读公开 getter。
 */
public final class StreamWriteStats {

	private final AtomicInteger tokenCount = new AtomicInteger();

	private final AtomicInteger flushCount = new AtomicInteger();

	private final AtomicLong bytesWritten = new AtomicLong();

	/** Writer 每收到一个 token 调用一次。 */
	void onToken() {
		tokenCount.incrementAndGet();
	}

	/**
	 * Writer 每完成一次向 {@link ByteSink} 的写出调用一次。
	 * @param bytes 本次写出的字节数；≤0 时忽略（空刷不计）
	 */
	void onFlush(int bytes) {
		if (bytes > 0) {
			flushCount.incrementAndGet();
			bytesWritten.addAndGet(bytes);
		}
	}

	/** 已处理的 token 个数。 */
	public int tokenCount() {
		return tokenCount.get();
	}

	/** 已发生的 flush（写出）次数；对比实验请重点看这个。 */
	public int flushCount() {
		return flushCount.get();
	}

	/** 已写出的总字节数。 */
	public long bytesWritten() {
		return bytesWritten.get();
	}

	@Override
	public String toString() {
		return "StreamWriteStats{tokens=" + tokenCount() + ", flushes=" + flushCount() + ", bytes=" + bytesWritten()
				+ '}';
	}

}

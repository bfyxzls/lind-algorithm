package com.lind.depth.stream;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 测试 / 演示用 {@link ByteSink}：不碰真实网络，只记录「写了几次、写了什么」。
 *
 * <p>
 * 用途：
 * <ul>
 * <li>对比实验里校验内容完整性（{@link #concatenated()}）</li>
 * <li>观察 {@link #writeCalls()} 是否与 Writer 的 {@link StreamWriteStats#flushCount()} 一致</li>
 * </ul>
 */
public final class CountingByteSink implements ByteSink {

	private final AtomicInteger writeCalls = new AtomicInteger();

	private final List<byte[]> chunks = new ArrayList<>();

	/** 为 true 时保留每次写出的字节副本，便于断言内容；生产对比大数据量时可关以省内存。 */
	private final boolean retainContent;

	/** 默认不保留内容副本，只计数。 */
	public CountingByteSink() {
		this(false);
	}

	/**
	 * @param retainContent true 则每次 {@link #write} 拷贝一份字节到内存列表
	 */
	public CountingByteSink(boolean retainContent) {
		this.retainContent = retainContent;
	}

	@Override
	public void write(ByteBuffer src) {
		int remaining = src.remaining();
		if (remaining <= 0) {
			return;
		}
		writeCalls.incrementAndGet();
		if (retainContent) {
			byte[] copy = new byte[remaining];
			src.get(copy); // 同时推进 position，消费完 buffer
			chunks.add(copy);
		}
		else {
			// 不保留内容时，直接把 position 推到 limit，表示「已读完」
			src.position(src.limit());
		}
	}

	/** Sink 被调用 {@link #write} 的次数。 */
	public int writeCalls() {
		return writeCalls.get();
	}

	/**
	 * 按写出顺序拼接的全部内容。
	 * @return 字节数组；若构造时 {@code retainContent=false} 则恒为空数组
	 */
	public byte[] concatenated() {
		int total = 0;
		for (byte[] c : chunks) {
			total += c.length;
		}
		byte[] all = new byte[total];
		int pos = 0;
		for (byte[] c : chunks) {
			System.arraycopy(c, 0, all, pos, c.length);
			pos += c.length;
		}
		return all;
	}

}

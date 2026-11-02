package com.lind.depth.stream;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 字节下游抽象：Writer 只负责「何时攒、何时刷」，真正写到哪由 Sink 决定。
 *
 * <p>
 * 常见实现：
 * <ul>
 * <li>{@link CountingByteSink} —— 单测 / 演示，统计写出次数与内容</li>
 * <li>{@link SocketChannelByteSink} —— 接到 {@link java.nio.channels.SocketChannel} /
 * {@link java.nio.channels.FileChannel}</li>
 * </ul>
 *
 * <p>
 * <b>约定：</b>调用方传入的 {@link ByteBuffer} 已处于可读状态（通常刚 {@code flip()}）， 实现方应消费完
 * {@code remaining()}，或推进 position 至 limit。
 */
@FunctionalInterface
public interface ByteSink {

	/**
	 * 写出当前 buffer 中可读的全部字节。
	 * @param src 可读 ByteBuffer（可能是 DirectBuffer）
	 */
	void write(ByteBuffer src) throws IOException;

}

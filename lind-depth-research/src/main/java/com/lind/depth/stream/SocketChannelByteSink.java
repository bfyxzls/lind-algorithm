package com.lind.depth.stream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;

/**
 * 把缓冲写到 NIO {@link WritableByteChannel}。
 *
 * <p>
 * 典型接入：
 * <ul>
 * <li>{@link java.nio.channels.SocketChannel} —— 对着 TCP 连接</li>
 * <li>{@link java.nio.channels.FileChannel} —— 写文件时也可验证 DirectBuffer 路径</li>
 * </ul>
 *
 * <p>
 * 与 {@link CoalescingStreamWriter} 搭配时：Writer 侧用 {@link ByteBuffer#allocateDirect(int)}
 * 攒包，本类再 {@code channel.write(directBuffer)}，尽量让数据少在 JVM 堆里打转——对应文档「堆外内存 + Channel」建议。
 * （Java 无法直接调 Linux {@code splice}，这是同思想下的可落地近似。）
 *
 * <p>
 * <b>注意：</b>{@link WritableByteChannel#write(ByteBuffer)} 可能一次写不完，必须循环直到
 * {@code !src.hasRemaining()}。
 */
public final class SocketChannelByteSink implements ByteSink {

	private final WritableByteChannel channel;

	public SocketChannelByteSink(WritableByteChannel channel) {
		this.channel = Objects.requireNonNull(channel, "channel");
	}

	@Override
	public void write(ByteBuffer src) throws IOException {
		while (src.hasRemaining()) {
			int n = channel.write(src);
			if (n < 0) {
				throw new IOException("channel 已关闭");
			}
			// n == 0：非阻塞 channel 暂时写不动，研究 demo 假设阻塞或外层会再调；此处简单自旋等待会有问题，
			// 生产应交给 Netty EventLoop / 选择器。教学代码保持直白循环即可。
		}
	}

}

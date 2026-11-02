package com.lind.depth.stream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <b>工程做法</b>：堆外 {@link ByteBuffer#allocateDirect(int)} 攒包，达到字节阈值或时间窗再 flush。
 *
 * <h2>对应文档要点（{@code docs/工程化分析.md} 场景一）</h2>
 * <ol>
 * <li>AI 吐 token 是流，网络写出也保持流 —— 但不「每 token 一包」</li>
 * <li>DirectBuffer：数据在堆外，减少 JVM 堆内搬迁（Java 侧靠近「零拷贝」思想）</li>
 * <li>阈值 ≈ MTU 有效载荷，降低系统调用与 TCP 小包</li>
 * <li>时间窗兜底：token 稀疏时也能在延迟上界内发出</li>
 * </ol>
 *
 * <h2>线程模型</h2> 本类用锁保护内部 buffer，可在多线程喂 token；更常见的是「每连接一个 Writer，绑定同一 EventLoop
 * 线程」，那时可去掉锁。研究代码优先保证正确性。
 *
 * <h2>生命周期</h2> {@link #close()} 必须调用，否则尾包可能留在 DirectBuffer 里发不出去。
 */
public final class CoalescingStreamWriter implements StreamWriter {

	private final ByteSink sink;

	private final FlushPolicy policy;

	/** 堆外缓冲：position=已写入字节，limit=capacity；flush 前会 flip。 */
	private final ByteBuffer buffer;

	private final StreamWriteStats stats = new StreamWriteStats();

	private final ReentrantLock lock = new ReentrantLock();

	/** 上次成功 flush 的时间戳（纳秒），用于时间窗判断。 */
	private long lastFlushNanos;

	private boolean closed;

	/**
	 * @param sink 下游；测试用 {@link CountingByteSink}，真网络用 {@link SocketChannelByteSink}
	 * @param policy 字节阈值 + 最大延迟
	 */
	public CoalescingStreamWriter(ByteSink sink, FlushPolicy policy) {
		this.sink = Objects.requireNonNull(sink, "sink");
		this.policy = Objects.requireNonNull(policy, "policy");
		// 容量 ≥ 阈值，并留余量，减少「刚好写满」时的边界抖动
		int capacity = Math.max(policy.flushBytes() * 2, 4096);
		this.buffer = ByteBuffer.allocateDirect(capacity);
		this.lastFlushNanos = System.nanoTime();
	}

	/**
	 * 编码 token 写入 DirectBuffer；满阈值则刷；否则检查时间窗。
	 * <p>
	 * 若单个 token 大于剩余空间，会先刷旧数据，再分段写入（极少见，但要正确）。
	 */
	@Override
	public void onToken(String token) throws IOException {
		Objects.requireNonNull(token, "token");
		byte[] bytes = token.getBytes(StandardCharsets.UTF_8);
		lock.lock();
		try {
			ensureOpen();
			stats.onToken();
			int offset = 0;
			while (offset < bytes.length) {
				// 缓冲已满：先刷空再继续写
				if (!buffer.hasRemaining()) {
					flushUnlocked(false);
				}
				int toPut = Math.min(buffer.remaining(), bytes.length - offset);
				buffer.put(bytes, offset, toPut);
				offset += toPut;
				// 达到字节阈值：立即刷，避免继续攒成超大包
				if (buffer.position() >= policy.flushBytes()) {
					flushUnlocked(false);
				}
			}
			// 本轮写入后，若距上次 flush 已超时，也刷一次（时间窗兜底）
			maybeTimeFlushUnlocked();
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * 主动检查时间窗。
	 * <p>
	 * 可在定时任务或 Netty EventLoop 空闲回调里调用。 若只依赖 {@link #onToken}，在「很久才来下一个
	 * token」时，上一段残留可能一直不发，直到超时被下一次 onToken 顺带触发，或 close 时刷出。
	 */
	public void tick() throws IOException {
		lock.lock();
		try {
			ensureOpen();
			maybeTimeFlushUnlocked();
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public StreamWriteStats stats() {
		return stats;
	}

	/**
	 * 刷出残留字节并标记关闭。可安全重复调用。
	 */
	@Override
	public void close() throws IOException {
		lock.lock();
		try {
			if (closed) {
				return;
			}
			flushUnlocked(true);
			closed = true;
		}
		finally {
			lock.unlock();
		}
	}

	/** 若缓冲非空且已超过 {@link FlushPolicy#maxDelay()}，则 flush。 */
	private void maybeTimeFlushUnlocked() throws IOException {
		if (buffer.position() == 0) {
			return;
		}
		long elapsed = System.nanoTime() - lastFlushNanos;
		if (elapsed >= policy.maxDelay().toNanos()) {
			flushUnlocked(false);
		}
	}

	/**
	 * 把当前缓冲 flip 后交给 Sink，然后 clear 复用同一块 DirectBuffer。
	 * @param force 关闭路径传入 true；当前实现与 false 相同，保留参数便于日后区分「强制刷空」语义
	 */
	private void flushUnlocked(boolean force) throws IOException {
		if (buffer.position() == 0) {
			if (force) {
				lastFlushNanos = System.nanoTime();
			}
			return;
		}
		buffer.flip(); // limit=position, position=0 → 变为可读
		int bytes = buffer.remaining();
		sink.write(buffer);
		stats.onFlush(bytes);
		buffer.clear(); // 复用这块堆外内存，避免反复 allocateDirect
		lastFlushNanos = System.nanoTime();
	}

	private void ensureOpen() {
		if (closed) {
			throw new IllegalStateException("writer 已关闭");
		}
	}

}

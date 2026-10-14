package com.lind.data.starter.redis.redisson;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;

/**
 * Redisson 延迟队列：基于 {@link RDelayedQueue} + {@link RBlockingQueue}。
 * <p>
 * 适合延时关单、延时通知等延时任务：生产者 {@link #scheduleAfter} / {@link #scheduleAt}，消费者 {@link #take} /
 * {@link #poll} 阻塞或短轮询取到期任务。
 */
public final class RedissonDelayTaskQueue implements AutoCloseable {

	private final String name;

	private final RBlockingQueue<String> blockingQueue;

	private final RDelayedQueue<String> delayedQueue;

	public RedissonDelayTaskQueue(RedissonClient client, String name) {
		Objects.requireNonNull(client, "client");
		Objects.requireNonNull(name, "name");
		if (name.isBlank()) {
			throw new IllegalArgumentException("name must not be blank");
		}
		this.name = name;
		this.blockingQueue = client.getBlockingQueue(name);
		this.delayedQueue = client.getDelayedQueue(blockingQueue);
	}

	/**
	 * 在 delay 之后投递任务。
	 */
	public void scheduleAfter(String message, Duration delay) {
		Objects.requireNonNull(message, "message");
		Objects.requireNonNull(delay, "delay");
		if (delay.isNegative()) {
			throw new IllegalArgumentException("delay must be >= 0");
		}
		delayedQueue.offer(message, delay.toMillis(), TimeUnit.MILLISECONDS);
	}

	/**
	 * 在绝对时间 executeAt 投递；若已过期则立即进入目标队列。
	 */
	public void scheduleAt(String message, Instant executeAt) {
		Objects.requireNonNull(message, "message");
		Objects.requireNonNull(executeAt, "executeAt");
		long delayMs = executeAt.toEpochMilli() - System.currentTimeMillis();
		scheduleAfter(message, Duration.ofMillis(Math.max(0, delayMs)));
	}

	/**
	 * 兼容 Spring ZSET 延迟队列命名。
	 */
	public void scheduleAfterMillis(String message, long delayMillis) {
		scheduleAfter(message, Duration.ofMillis(delayMillis));
	}

	/**
	 * 阻塞直到取到一条到期任务。
	 */
	public String take() throws InterruptedException {
		return blockingQueue.take();
	}

	/**
	 * 在 timeout 内等待一条到期任务。
	 */
	public Optional<String> poll(Duration timeout) throws InterruptedException {
		Objects.requireNonNull(timeout, "timeout");
		String value = blockingQueue.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
		return Optional.ofNullable(value);
	}

	/**
	 * 非阻塞取出当前已到期的全部任务（单次排水，适合定时拉取）。
	 */
	public List<String> pollDue() {
		List<String> due = new ArrayList<>();
		String item;
		while ((item = blockingQueue.poll()) != null) {
			due.add(item);
		}
		return List.copyOf(due);
	}

	/**
	 * 启动一个守护线程持续消费到期任务，直到 {@link #close()} 或线程中断。
	 * @return 已启动的消费者线程
	 */
	public Thread startConsumer(java.util.function.Consumer<String> handler) {
		Objects.requireNonNull(handler, "handler");
		Thread thread = new Thread(() -> {
			while (!Thread.currentThread().isInterrupted()) {
				try {
					String message = take();
					handler.accept(message);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
				catch (Exception ignored) {
					// 单条失败不中断消费循环
				}
			}
		}, "lind-redisson-delay-" + name);
		thread.setDaemon(true);
		thread.start();
		return thread;
	}

	public String name() {
		return name;
	}

	public RBlockingQueue<String> blockingQueue() {
		return blockingQueue;
	}

	public RDelayedQueue<String> delayedQueue() {
		return delayedQueue;
	}

	/**
	 * 销毁延迟队列（释放内部调度资源）。进程退出前建议调用。
	 */
	@Override
	public void close() {
		delayedQueue.destroy();
	}

}

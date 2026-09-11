package com.lind.algorithm.wheel;

import java.util.Objects;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 可落地的层级哈希时间轮定时器，面向大批量延时任务（订单超时、缓存过期、延迟消息等）。
 *
 * <p>
 * 设计要点（对齐 Kafka TimingWheel / Netty HashedWheelTimer 常见实践）：
 * <ul>
 * <li>单层跨度有限时自动创建上层溢出轮，长延时不占用巨大槽数组</li>
 * <li>用 {@link DelayQueue} 按槽到期精准推进，避免空 tick 空转</li>
 * <li>到期任务投递到工作线程池，推进线程不被业务逻辑阻塞</li>
 * <li>支持取消、优雅关闭</li>
 * </ul>
 *
 * <pre>{@code
 * try (HashedWheelTimer timer = new HashedWheelTimer()) {
 *     Timeout t = timer.newTimeout(timeout -> doSomething(), 5, TimeUnit.SECONDS);
 *     // t.cancel();
 * }
 * }</pre>
 */
public final class HashedWheelTimer implements AutoCloseable {

	private static final AtomicInteger THREAD_SEQ = new AtomicInteger();

	private static final int DEFAULT_WHEEL_SIZE = 20;

	private static final long DEFAULT_TICK_MS = 1L;

	private static final long DEFAULT_POLL_MS = 200L;

	private final DelayQueue<TimerTaskList> delayQueue = new DelayQueue<>();

	private final TimingWheel timingWheel;

	private final Executor taskExecutor;

	private final boolean shutdownTaskExecutor;

	private final ExecutorService bossExecutor;

	private final AtomicBoolean started = new AtomicBoolean();

	private final AtomicBoolean shutdown = new AtomicBoolean();

	private final AtomicInteger pending = new AtomicInteger();

	private final AtomicLong completed = new AtomicLong();

	private final TaskExceptionHandler exceptionHandler;

	private final long pollTimeoutMs;

	/**
	 * 默认：tick=1ms，wheelSize=20，单线程执行任务，daemon 推进线程。
	 */
	public HashedWheelTimer() {
		this(DEFAULT_TICK_MS, DEFAULT_WHEEL_SIZE, Executors.newSingleThreadExecutor(daemonFactory("wheel-worker-")),
				true, DEFAULT_POLL_MS, TaskExceptionHandler.NOOP);
	}

	/**
	 * @param tickMs 最内层时间格粒度（毫秒），决定调度精度下限
	 * @param wheelSize 每层槽数，建议 20~512
	 * @param taskExecutor 到期任务执行器（建议与业务隔离的线程池）
	 */
	public HashedWheelTimer(long tickMs, int wheelSize, Executor taskExecutor) {
		this(tickMs, wheelSize, taskExecutor, false, DEFAULT_POLL_MS, TaskExceptionHandler.NOOP);
	}

	public HashedWheelTimer(long tickMs, int wheelSize, Executor taskExecutor, boolean shutdownTaskExecutor,
			long pollTimeoutMs, TaskExceptionHandler exceptionHandler) {
		if (tickMs <= 0) {
			throw new IllegalArgumentException("tickMs must be > 0");
		}
		if (wheelSize <= 0) {
			throw new IllegalArgumentException("wheelSize must be > 0");
		}
		if (pollTimeoutMs <= 0) {
			throw new IllegalArgumentException("pollTimeoutMs must be > 0");
		}
		this.taskExecutor = Objects.requireNonNull(taskExecutor, "taskExecutor");
		this.shutdownTaskExecutor = shutdownTaskExecutor;
		this.pollTimeoutMs = pollTimeoutMs;
		this.exceptionHandler = exceptionHandler == null ? TaskExceptionHandler.NOOP : exceptionHandler;
		this.timingWheel = new TimingWheel(tickMs, wheelSize, System.currentTimeMillis(), delayQueue);
		this.bossExecutor = Executors.newSingleThreadExecutor(daemonFactory("wheel-boss-"));
	}

	/**
	 * 提交相对延时任务。
	 * @param task 到期回调
	 * @param delay 延时
	 * @param unit 时间单位
	 * @return 可取消的 {@link Timeout}
	 */
	public Timeout newTimeout(TimerTask task, long delay, TimeUnit unit) {
		Objects.requireNonNull(task, "task");
		Objects.requireNonNull(unit, "unit");
		if (delay < 0) {
			throw new IllegalArgumentException("delay must be >= 0");
		}
		if (shutdown.get()) {
			throw new IllegalStateException("timer already stopped");
		}
		startIfNeeded();
		long deadlineMs = System.currentTimeMillis() + Math.max(0, unit.toMillis(delay));
		TimerTaskEntry entry = new TimerTaskEntry(this, task, deadlineMs);
		pending.incrementAndGet();
		synchronized (this) {
			addEntry(entry);
		}
		return entry;
	}

	/**
	 * @return 尚未到期（含排队执行前）的任务数
	 */
	public int pendingTasks() {
		return pending.get();
	}

	/**
	 * @return 已成功触发执行的次数（含立即到期）
	 */
	public long completedTasks() {
		return completed.get();
	}

	/**
	 * 停止时间轮：不再接受新任务；推进线程退出；可选关闭内置 worker。 已入轮但未到期的任务将被丢弃（调用方可先 cancel 或自行迁移）。
	 */
	public void stop() {
		if (!shutdown.compareAndSet(false, true)) {
			return;
		}
		bossExecutor.shutdownNow();
		try {
			bossExecutor.awaitTermination(2, TimeUnit.SECONDS);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		if (shutdownTaskExecutor && taskExecutor instanceof ExecutorService) {
			((ExecutorService) taskExecutor).shutdown();
		}
	}

	@Override
	public void close() {
		stop();
	}

	void decrementPending() {
		pending.decrementAndGet();
	}

	private void startIfNeeded() {
		if (started.compareAndSet(false, true)) {
			bossExecutor.execute(this::runLoop);
		}
	}

	private void runLoop() {
		while (!shutdown.get()) {
			try {
				TimerTaskList bucket = delayQueue.poll(pollTimeoutMs, TimeUnit.MILLISECONDS);
				if (bucket != null) {
					synchronized (this) {
						timingWheel.advanceClock(bucket.getExpiration());
						do {
							bucket.flush(this::addEntry);
							bucket = delayQueue.poll();
						}
						while (bucket != null);
					}
				}
			}
			catch (InterruptedException e) {
				if (shutdown.get()) {
					return;
				}
				// 非关闭场景的中断不恢复中断标志，避免后续 poll 立刻失败导致空转
			}
		}
	}

	private void addEntry(TimerTaskEntry entry) {
		if (entry.isCancelled() || shutdown.get()) {
			if (!entry.isCancelled()) {
				pending.decrementAndGet();
			}
			return;
		}
		if (!timingWheel.add(entry)) {
			entry.markExpired();
			pending.decrementAndGet();
			completed.incrementAndGet();
			taskExecutor.execute(() -> runTask(entry));
		}
	}

	private void runTask(TimerTaskEntry entry) {
		if (entry.isCancelled()) {
			return;
		}
		try {
			entry.task().run(entry);
		}
		catch (Throwable t) {
			exceptionHandler.handle(entry.task(), t);
		}
	}

	private static ThreadFactory daemonFactory(String prefix) {
		return r -> {
			Thread t = new Thread(r, prefix + THREAD_SEQ.incrementAndGet());
			t.setDaemon(true);
			return t;
		};
	}

	/**
	 * 任务执行异常处理。
	 */
	@FunctionalInterface
	public interface TaskExceptionHandler {

		TaskExceptionHandler NOOP = (task, error) -> {
		};

		void handle(TimerTask task, Throwable error);

	}

}

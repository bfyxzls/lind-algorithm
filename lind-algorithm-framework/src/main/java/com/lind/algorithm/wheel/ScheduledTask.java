package com.lind.algorithm.wheel;

import java.time.Instant;
import java.util.Objects;

/**
 * 时间轮中尚未到期任务的快照：任务句柄及其计划执行时间。
 */
public final class ScheduledTask {

	private final Timeout timeout;

	private final long deadlineEpochMs;

	ScheduledTask(Timeout timeout, long deadlineEpochMs) {
		this.timeout = Objects.requireNonNull(timeout, "timeout");
		this.deadlineEpochMs = deadlineEpochMs;
	}

	/**
	 * @return 调度句柄（可 {@link Timeout#cancel()}）
	 */
	public Timeout timeout() {
		return timeout;
	}

	/**
	 * @return 到期回调
	 */
	public TimerTask task() {
		return timeout.task();
	}

	/**
	 * @return 计划执行时间（epoch 毫秒）
	 */
	public long deadlineEpochMs() {
		return deadlineEpochMs;
	}

	/**
	 * @return 计划执行时间
	 */
	public Instant deadline() {
		return Instant.ofEpochMilli(deadlineEpochMs);
	}

}

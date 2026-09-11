package com.lind.algorithm.wheel;

import java.util.concurrent.DelayQueue;

/**
 * 层级哈希时间轮（Kafka TimingWheel 模型）：短延时落当前轮，长延时溢出到上层轮，到期降级重入。
 */
final class TimingWheel {

	private final long tickMs;

	private final int wheelSize;

	private final long interval;

	private long currentTime;

	private final TimerTaskList[] buckets;

	private final DelayQueue<TimerTaskList> delayQueue;

	private volatile TimingWheel overflowWheel;

	TimingWheel(long tickMs, int wheelSize, long startMs, DelayQueue<TimerTaskList> delayQueue) {
		this.tickMs = tickMs;
		this.wheelSize = wheelSize;
		this.interval = tickMs * wheelSize;
		this.delayQueue = delayQueue;
		this.buckets = new TimerTaskList[wheelSize];
		for (int i = 0; i < wheelSize; i++) {
			buckets[i] = new TimerTaskList();
		}
		this.currentTime = startMs - (startMs % tickMs);
	}

	/**
	 * @return {@code false} 表示已到期，应由调用方立即执行；{@code true} 表示已入轮或上层轮
	 */
	boolean add(TimerTaskEntry entry) {
		long expiration = entry.deadlineMs();
		if (entry.isCancelled()) {
			return true;
		}
		if (expiration < currentTime + tickMs) {
			return false;
		}
		if (expiration < currentTime + interval) {
			long virtualId = expiration / tickMs;
			int index = (int) (virtualId % wheelSize);
			TimerTaskList bucket = buckets[index];
			bucket.add(entry);
			if (bucket.setExpiration(virtualId * tickMs)) {
				delayQueue.offer(bucket);
			}
			return true;
		}
		return overflowWheel().add(entry);
	}

	void advanceClock(long timeMs) {
		if (timeMs >= currentTime + tickMs) {
			currentTime = timeMs - (timeMs % tickMs);
			if (overflowWheel != null) {
				overflowWheel.advanceClock(currentTime);
			}
		}
	}

	private TimingWheel overflowWheel() {
		if (overflowWheel == null) {
			synchronized (this) {
				if (overflowWheel == null) {
					overflowWheel = new TimingWheel(interval, wheelSize, currentTime, delayQueue);
				}
			}
		}
		return overflowWheel;
	}

}

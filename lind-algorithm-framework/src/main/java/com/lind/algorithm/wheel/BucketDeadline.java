package com.lind.algorithm.wheel;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * DelayQueue 中的不可变到期项。槽位 {@link TimerTaskList} 的 expiration 可变，不能直接作为 Delayed
 * 入队，否则堆序失效会导致层级溢出任务偶发永不触发。
 */
final class BucketDeadline implements Delayed {

	private final TimerTaskList bucket;

	private final long expirationMs;

	BucketDeadline(TimerTaskList bucket, long expirationMs) {
		this.bucket = bucket;
		this.expirationMs = expirationMs;
	}

	TimerTaskList bucket() {
		return bucket;
	}

	long expirationMs() {
		return expirationMs;
	}

	/**
	 * 槽位已被重新调度或已 flush 时视为过期条目，应丢弃。
	 */
	boolean isStale() {
		return bucket.getExpiration() != expirationMs;
	}

	@Override
	public long getDelay(TimeUnit unit) {
		return Math.max(0, unit.convert(expirationMs - System.currentTimeMillis(), TimeUnit.MILLISECONDS));
	}

	@Override
	public int compareTo(Delayed other) {
		if (other instanceof BucketDeadline) {
			return Long.compare(expirationMs, ((BucketDeadline) other).expirationMs);
		}
		return 0;
	}

}

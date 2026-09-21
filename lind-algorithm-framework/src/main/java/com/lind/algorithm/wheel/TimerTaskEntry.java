package com.lind.algorithm.wheel;

/**
 * 时间轮槽上的任务节点（双向链表项）。
 */
final class TimerTaskEntry implements Timeout {

	private final HashedWheelTimer timer;

	private final TimerTask task;

	private final long deadlineMs;

	volatile TimerTaskList list;

	TimerTaskEntry prev;

	TimerTaskEntry next;

	private volatile boolean cancelled;

	private volatile boolean expired;

	TimerTaskEntry(HashedWheelTimer timer, TimerTask task, long deadlineMs) {
		this.timer = timer;
		this.task = task;
		this.deadlineMs = deadlineMs;
	}

	@Override
	public long deadlineMs() {
		return deadlineMs;
	}

	@Override
	public HashedWheelTimer timer() {
		return timer;
	}

	@Override
	public TimerTask task() {
		return task;
	}

	@Override
	public boolean cancel() {
		if (cancelled || expired) {
			return false;
		}
		cancelled = true;
		remove();
		timer.decrementPending();
		return true;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public boolean isExpired() {
		return expired;
	}

	void markExpired() {
		expired = true;
	}

	/**
	 * 从当前链表移除；可能与其他线程并发，循环直到脱离链表。
	 */
	void remove() {
		TimerTaskList current = list;
		while (current != null) {
			current.remove(this);
			current = list;
		}
	}

}

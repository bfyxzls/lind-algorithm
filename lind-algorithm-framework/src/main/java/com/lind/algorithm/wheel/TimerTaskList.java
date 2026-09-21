package com.lind.algorithm.wheel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * 时间轮单个槽：环形双向链表 + 槽到期时间（供 {@link java.util.concurrent.DelayQueue} 推进）。
 */
final class TimerTaskList implements Delayed {

	private final TimerTaskEntry root = new TimerTaskEntry(null, null, -1);

	private final AtomicLong expiration = new AtomicLong(-1L);

	TimerTaskList() {
		root.next = root;
		root.prev = root;
	}

	long getExpiration() {
		return expiration.get();
	}

	/**
	 * 设置槽过期时间；与旧值不同时返回 true（需重新入 DelayQueue）。
	 */
	boolean setExpiration(long expirationMs) {
		return expiration.getAndSet(expirationMs) != expirationMs;
	}

	void add(TimerTaskEntry entry) {
		boolean done = false;
		while (!done) {
			entry.remove();
			synchronized (this) {
				if (entry.list == null) {
					entry.list = this;
					TimerTaskEntry tail = root.prev;
					entry.prev = tail;
					entry.next = root;
					tail.next = entry;
					root.prev = entry;
					done = true;
				}
			}
		}
	}

	/**
	 * 遍历槽内任务（不移除）。调用方需自行跳过已取消节点。
	 */
	synchronized void forEach(Consumer<TimerTaskEntry> consumer) {
		TimerTaskEntry head = root.next;
		while (head != root) {
			consumer.accept(head);
			head = head.next;
		}
	}

	void remove(TimerTaskEntry entry) {
		synchronized (this) {
			if (entry.list == this) {
				entry.next.prev = entry.prev;
				entry.prev.next = entry.next;
				entry.next = null;
				entry.prev = null;
				entry.list = null;
			}
		}
	}

	/**
	 * 清空槽内任务并重置过期时间，再回调（先 reset 再重入，避免重入同一槽时 setExpiration 未入队随后又被置为 -1 导致任务永远卡住）。
	 */
	synchronized void flush(Consumer<TimerTaskEntry> consumer) {
		List<TimerTaskEntry> entries = new ArrayList<>();
		TimerTaskEntry head = root.next;
		while (head != root) {
			remove(head);
			entries.add(head);
			head = root.next;
		}
		expiration.set(-1L);
		for (TimerTaskEntry entry : entries) {
			consumer.accept(entry);
		}
	}

	@Override
	public long getDelay(TimeUnit unit) {
		return Math.max(0, unit.convert(expiration.get() - System.currentTimeMillis(), TimeUnit.MILLISECONDS));
	}

	@Override
	public int compareTo(Delayed other) {
		if (other instanceof TimerTaskList) {
			return Long.compare(expiration.get(), ((TimerTaskList) other).expiration.get());
		}
		return 0;
	}

}

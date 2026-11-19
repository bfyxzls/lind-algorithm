package com.lind.foundation.concurrent;

import java.util.Objects;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 3.2：{@link ReentrantLock} + 双 {@link Condition} 实现的有界环形缓冲（生产者-消费者）。
 * <p>
 * 概念见 {@code locks.md}。
 */
public final class BoundedBuffer<E> {

	private final Object[] items;

	private int putIndex;

	private int takeIndex;

	private int count;

	private final ReentrantLock lock = new ReentrantLock();

	/** 缓冲未满时可 put。 */
	private final Condition notFull = lock.newCondition();

	/** 缓冲非空时可 take。 */
	private final Condition notEmpty = lock.newCondition();

	public BoundedBuffer(int capacity) {
		if (capacity <= 0) {
			throw new IllegalArgumentException("capacity must be > 0");
		}
		this.items = new Object[capacity];
	}

	public int capacity() {
		return items.length;
	}

	public int size() {
		lock.lock();
		try {
			return count;
		}
		finally {
			lock.unlock();
		}
	}

	/** 满则在 notFull 上等待；放入后唤醒一个 take 方。 */
	public void put(E item) throws InterruptedException {
		Objects.requireNonNull(item, "item");
		lock.lock();
		try {
			while (count == items.length) {
				// 满：释放锁并等待；被 signal 后重新获锁，再检查 while（防止虚假唤醒）
				notFull.await();
			}
			items[putIndex] = item;
			putIndex = (putIndex + 1) % items.length;
			count++;
			notEmpty.signal(); // 只唤醒等「非空」的消费者，比 notifyAll 更精准
		}
		finally {
			lock.unlock();
		}
	}

	/** 空则在 notEmpty 上等待；取出后唤醒一个 put 方。 */
	@SuppressWarnings("unchecked")
	public E take() throws InterruptedException {
		lock.lock();
		try {
			while (count == 0) {
				notEmpty.await();
			}
			E item = (E) items[takeIndex];
			items[takeIndex] = null;
			takeIndex = (takeIndex + 1) % items.length;
			count--;
			notFull.signal();
			return item;
		}
		finally {
			lock.unlock();
		}
	}

}

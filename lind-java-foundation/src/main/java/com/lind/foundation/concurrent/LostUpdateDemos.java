package com.lind.foundation.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 3.1：多线程 {@code count++} 丢失更新对照 —— 无锁 / {@code synchronized} / {@link ReentrantLock}。
 * <p>
 * 概念见 {@code locks.md}。
 */
public final class LostUpdateDemos {

	public record Result(String mode, int threads, int perThread, int expected, int actual) {

		public boolean correct() {
			return actual == expected;
		}

		@Override
		public String toString() {
			return mode + ": expected=" + expected + ", actual=" + actual + (correct() ? " OK" : " LOST_UPDATES");
		}

	}

	private LostUpdateDemos() {
	}

	/** 无同步：几乎必然丢更新。 */
	public static Result runUnsafe(int threads, int perThread) throws InterruptedException {
		int[] box = { 0 };
		runParallel(threads, perThread, () -> box[0]++);
		return new Result("unsafe", threads, perThread, threads * perThread, box[0]);
	}

	/** synchronized 包住临界区。 */
	public static Result runSynchronized(int threads, int perThread) throws InterruptedException {
		int[] box = { 0 };
		Object lock = new Object();
		runParallel(threads, perThread, () -> {
			synchronized (lock) {
				box[0]++;
			}
		});
		return new Result("synchronized", threads, perThread, threads * perThread, box[0]);
	}

	/** 同一临界区用 ReentrantLock（为 3.2 热身）。 */
	public static Result runReentrantLock(int threads, int perThread) throws InterruptedException {
		int[] box = { 0 };
		ReentrantLock lock = new ReentrantLock();
		runParallel(threads, perThread, () -> {
			lock.lock();
			try {
				box[0]++;
			}
			finally {
				lock.unlock();
			}
		});
		return new Result("reentrantLock", threads, perThread, threads * perThread, box[0]);
	}

	private static void runParallel(int threads, int perThread, Runnable once) throws InterruptedException {
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch ready = new CountDownLatch(threads);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(threads);
		for (int t = 0; t < threads; t++) {
			pool.execute(() -> {
				ready.countDown();
				try {
					start.await();
					for (int i = 0; i < perThread; i++) {
						once.run();
					}
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				finally {
					done.countDown();
				}
			});
		}
		ready.await(5, TimeUnit.SECONDS);
		start.countDown();
		done.await(60, TimeUnit.SECONDS);
		pool.shutdownNow();
	}

	public static void main(String[] args) throws Exception {
		int threads = 8;
		int perThread = 100_000;
		System.out.println(runUnsafe(threads, perThread));
		System.out.println(runSynchronized(threads, perThread));
		System.out.println(runReentrantLock(threads, perThread));
	}

}

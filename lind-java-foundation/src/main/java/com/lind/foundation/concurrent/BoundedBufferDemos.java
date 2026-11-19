package com.lind.foundation.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * 多生产者 / 多消费者压一下 {@link BoundedBuffer}，校验「放入总数 = 取出总数」。
 */
public final class BoundedBufferDemos {

	public record PumpResult(int capacity, int producers, int consumers, int perProducer, long produced,
			long consumed) {

		public boolean conserved() {
			return produced == consumed && produced == (long) producers * perProducer;
		}

		@Override
		public String toString() {
			return "capacity=" + capacity + ", produced=" + produced + ", consumed=" + consumed
					+ (conserved() ? " OK" : " MISMATCH");
		}

	}

	private BoundedBufferDemos() {
	}

	public static PumpResult pump(int capacity, int producers, int consumers, int perProducer)
			throws InterruptedException {
		BoundedBuffer<Integer> buffer = new BoundedBuffer<>(capacity);
		LongAdder produced = new LongAdder();
		LongAdder consumed = new LongAdder();
		AtomicInteger nextValue = new AtomicInteger();
		int total = producers * perProducer;
		// 消费者用「票」领取 take 次数，避免多人阻塞在空队列上死等
		AtomicInteger takeTickets = new AtomicInteger(total);

		ExecutorService pool = Executors.newFixedThreadPool(producers + consumers);
		CountDownLatch doneProduce = new CountDownLatch(producers);
		CountDownLatch doneConsume = new CountDownLatch(consumers);

		for (int p = 0; p < producers; p++) {
			pool.execute(() -> {
				try {
					for (int i = 0; i < perProducer; i++) {
						buffer.put(nextValue.getAndIncrement());
						produced.increment();
					}
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				finally {
					doneProduce.countDown();
				}
			});
		}

		for (int c = 0; c < consumers; c++) {
			pool.execute(() -> {
				try {
					while (true) {
						int ticket = takeTickets.getAndDecrement();
						if (ticket <= 0) {
							break;
						}
						buffer.take();
						consumed.increment();
					}
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				finally {
					doneConsume.countDown();
				}
			});
		}

		doneProduce.await(60, TimeUnit.SECONDS);
		doneConsume.await(60, TimeUnit.SECONDS);
		pool.shutdownNow();
		return new PumpResult(capacity, producers, consumers, perProducer, produced.sum(), consumed.sum());
	}

	public static void main(String[] args) throws Exception {
		System.out.println(pump(16, 4, 4, 5_000));
	}

}

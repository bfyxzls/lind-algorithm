package com.lind.qps.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * 在一个测试周期内以固定并发持续压测，收集成功/失败与延迟；失败时采样记录状态码与响应体。
 */
public final class LoadRunner {

	/** 单次压测最多保留的失败样本数，避免高失败率时撑爆内存。 */
	public static final int MAX_FAILURE_SAMPLES = 20;

	private final LoadConfig config;

	private final Supplier<RequestResult> requester;

	public LoadRunner(LoadConfig config) {
		this(config, new HttpRequester(config)::execute);
	}

	public LoadRunner(LoadConfig config, Supplier<RequestResult> requester) {
		this.config = Objects.requireNonNull(config, "config");
		this.requester = Objects.requireNonNull(requester, "requester");
	}

	public LoadReport run() {
		int concurrency = config.concurrency();
		ExecutorService pool = Executors.newFixedThreadPool(concurrency);
		AtomicBoolean running = new AtomicBoolean(true);
		AtomicLong success = new AtomicLong();
		AtomicLong failure = new AtomicLong();
		List<Long> latencies = new ArrayList<>(Math.max(1024, concurrency * 64));
		List<FailureSample> failureSamples = new ArrayList<>(MAX_FAILURE_SAMPLES);
		Object lock = new Object();
		CountDownLatch started = new CountDownLatch(concurrency);
		long startMillis = System.currentTimeMillis();
		long deadlineNanos = System.nanoTime() + config.duration().toNanos();

		for (int i = 0; i < concurrency; i++) {
			pool.submit(() -> {
				started.countDown();
				while (running.get() && System.nanoTime() < deadlineNanos) {
					RequestResult result = requester.get();
					if (result.success()) {
						success.incrementAndGet();
					}
					else {
						failure.incrementAndGet();
					}
					synchronized (lock) {
						latencies.add(result.latencyMillis());
						if (!result.success() && failureSamples.size() < MAX_FAILURE_SAMPLES) {
							failureSamples.add(FailureSample.from(result));
						}
					}
				}
			});
		}

		try {
			started.await(5, TimeUnit.SECONDS);
			long remaining = deadlineNanos - System.nanoTime();
			if (remaining > 0) {
				TimeUnit.NANOSECONDS.sleep(remaining);
			}
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		finally {
			running.set(false);
			pool.shutdown();
			try {
				if (!pool.awaitTermination(Math.max(5, config.requestTimeout().toSeconds() + 2), TimeUnit.SECONDS)) {
					pool.shutdownNow();
				}
			}
			catch (InterruptedException ex) {
				pool.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}

		long durationMillis = Math.max(1, System.currentTimeMillis() - startMillis);
		long[] sorted;
		List<FailureSample> samplesCopy;
		synchronized (lock) {
			sorted = LoadReport.sortedCopy(latencies.stream().mapToLong(Long::longValue).toArray());
			samplesCopy = List.copyOf(failureSamples);
		}
		return LoadReport.fromLatencies(config.url().toString(), concurrency, durationMillis, success.get(),
				failure.get(), sorted, samplesCopy);
	}

}

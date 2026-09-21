package com.lind.algorithm.wheel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * {@link HashedWheelTimer} 延时任务测试。
 */
public class HashedWheelTimerTest {

	private HashedWheelTimer timer;

	@AfterEach
	void tearDown() {
		if (timer != null) {
			timer.stop();
		}
	}

	@Test
	void firesAfterDelay() throws Exception {
		timer = newTimer(10, 20);
		CountDownLatch latch = new CountDownLatch(1);
		long start = System.currentTimeMillis();

		timer.newTimeout(t -> latch.countDown(), 80, TimeUnit.MILLISECONDS);

		assertTrue(latch.await(2, TimeUnit.SECONDS));
		long elapsed = System.currentTimeMillis() - start;
		assertTrue(elapsed >= 70, "elapsed=" + elapsed);
		assertEquals(0, timer.pendingTasks());
		assertEquals(1, timer.completedTasks());
	}

	@Test
	void cancelBeforeFire() throws Exception {
		timer = newTimer(10, 20);
		AtomicInteger runs = new AtomicInteger();

		Timeout timeout = timer.newTimeout(t -> runs.incrementAndGet(), 200, TimeUnit.MILLISECONDS);
		assertTrue(timeout.cancel());
		assertTrue(timeout.isCancelled());
		assertFalse(timeout.cancel());

		Thread.sleep(350);
		assertEquals(0, runs.get());
		assertEquals(0, timer.pendingTasks());
		assertEquals(0, timer.completedTasks());
	}

	@Test
	void zeroDelayRunsSoon() throws Exception {
		timer = newTimer(10, 20);
		CountDownLatch latch = new CountDownLatch(1);
		timer.newTimeout(t -> latch.countDown(), 0, TimeUnit.MILLISECONDS);
		assertTrue(latch.await(1, TimeUnit.SECONDS));
	}

	@Test
	void hierarchicalOverflowAndCascade() throws Exception {
		// interval = 10 * 4 = 40ms，延时 120ms 会进入上层轮，再降级执行
		timer = newTimer(10, 4);
		CountDownLatch latch = new CountDownLatch(1);
		long start = System.currentTimeMillis();

		timer.newTimeout(t -> latch.countDown(), 120, TimeUnit.MILLISECONDS);

		assertTrue(latch.await(5, TimeUnit.SECONDS));
		long elapsed = System.currentTimeMillis() - start;
		assertTrue(elapsed >= 100, "elapsed=" + elapsed);
		assertEquals(1, timer.completedTasks());
	}

	@Test
	void multipleTasksOrderedByDeadline() throws Exception {
		timer = newTimer(5, 32);
		List<Integer> order = Collections.synchronizedList(new ArrayList<>());
		CountDownLatch latch = new CountDownLatch(3);

		timer.newTimeout(t -> {
			order.add(3);
			latch.countDown();
		}, 90, TimeUnit.MILLISECONDS);
		timer.newTimeout(t -> {
			order.add(1);
			latch.countDown();
		}, 30, TimeUnit.MILLISECONDS);
		timer.newTimeout(t -> {
			order.add(2);
			latch.countDown();
		}, 60, TimeUnit.MILLISECONDS);

		assertTrue(latch.await(5, TimeUnit.SECONDS), "tasks not fired in time, order=" + order);
		assertEquals(List.of(1, 2, 3), order);
	}

	@Test
	void taskExceptionDoesNotBreakTimer() throws Exception {
		AtomicReference<Throwable> captured = new AtomicReference<>();
		ExecutorService worker = Executors.newSingleThreadExecutor();
		timer = new HashedWheelTimer(10, 20, worker, true, 50, (task, error) -> captured.set(error));

		CountDownLatch badDone = new CountDownLatch(1);
		CountDownLatch goodDone = new CountDownLatch(1);

		timer.newTimeout(t -> {
			try {
				throw new IllegalStateException("boom");
			}
			finally {
				badDone.countDown();
			}
		}, 20, TimeUnit.MILLISECONDS);

		timer.newTimeout(t -> goodDone.countDown(), 40, TimeUnit.MILLISECONDS);

		assertTrue(badDone.await(5, TimeUnit.SECONDS));
		assertTrue(goodDone.await(5, TimeUnit.SECONDS));
		assertTrue(captured.get() instanceof IllegalStateException);
	}

	@Test
	void rejectAfterStop() {
		timer = newTimer(10, 20);
		timer.stop();
		assertThrows(IllegalStateException.class, () -> timer.newTimeout(t -> {
		}, 10, TimeUnit.MILLISECONDS));
	}

	@Test
	void pendingCountTracksLifecycle() throws Exception {
		timer = newTimer(10, 20);
		CountDownLatch latch = new CountDownLatch(1);
		Timeout t1 = timer.newTimeout(x -> latch.countDown(), 50, TimeUnit.MILLISECONDS);
		Timeout t2 = timer.newTimeout(x -> {
		}, 500, TimeUnit.MILLISECONDS);
		assertEquals(2, timer.pendingTasks());

		assertTrue(t2.cancel());
		assertEquals(1, timer.pendingTasks());
		assertTrue(latch.await(1, TimeUnit.SECONDS));
		assertEquals(0, timer.pendingTasks());
		assertTrue(t1.isExpired());
	}

	@Test
	void scheduledTasksReturnsPendingDeadlinesInOrder() {
		timer = newTimer(10, 4);
		long now = System.currentTimeMillis();
		Timeout later = timer.newTimeout(t -> {
		}, 5, TimeUnit.SECONDS);
		Timeout sooner = timer.newTimeout(t -> {
		}, 2, TimeUnit.SECONDS);
		Timeout cancelled = timer.newTimeout(t -> {
		}, 8, TimeUnit.SECONDS);
		assertTrue(cancelled.cancel());

		List<ScheduledTask> tasks = timer.scheduledTasks();
		assertEquals(2, tasks.size());
		assertEquals(sooner, tasks.get(0).timeout());
		assertEquals(later, tasks.get(1).timeout());
		assertTrue(tasks.get(0).deadlineEpochMs() >= now + 1_500);
		assertTrue(tasks.get(1).deadlineEpochMs() >= now + 4_500);
		assertTrue(tasks.get(0).deadlineEpochMs() <= tasks.get(1).deadlineEpochMs());
		assertEquals(tasks.get(0).deadlineEpochMs(), sooner.deadlineMs());
	}

	private static HashedWheelTimer newTimer(long tickMs, int wheelSize) {
		ExecutorService worker = Executors.newSingleThreadExecutor();
		return new HashedWheelTimer(tickMs, wheelSize, worker, true, 30, HashedWheelTimer.TaskExceptionHandler.NOOP);
	}

}

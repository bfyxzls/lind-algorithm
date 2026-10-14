package com.lind.data.starter.redis.redisson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

/**
 * Redisson 分布式锁 / 延迟队列场景单测（mock，无需本机 Redis）。
 */
@ExtendWith(MockitoExtension.class)
class RedissonScenarioTest {

	@Mock
	private RedissonClient client;

	@Mock
	private RLock rLock;

	@Mock
	private RBlockingQueue<String> blockingQueue;

	@Mock
	private RDelayedQueue<String> delayedQueue;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		lenient().when(client.getLock(anyString())).thenReturn(rLock);
		lenient().when(client.getBlockingQueue(anyString())).thenReturn((RBlockingQueue) blockingQueue);
		lenient().when(client.getDelayedQueue(blockingQueue)).thenReturn(delayedQueue);
		lenient().when(rLock.getName()).thenReturn("lock");
	}

	@Test
	void facadeExposesLockAndDelayQueue() {
		LindRedisson redisson = new LindRedisson(client);
		assertThat(redisson.client()).isSameAs(client);
		assertThat(redisson.lock("order:1").isWatchdogEnabled()).isTrue();
		assertThat(redisson.lock("order:1", Duration.ofSeconds(30)).isWatchdogEnabled()).isFalse();
		assertThat(redisson.delayQueue("jobs:delay")).isNotNull();
	}

	@Test
	void watchdogTryLockAndUnlock() throws Exception {
		when(rLock.tryLock(0L, -1L, TimeUnit.MILLISECONDS)).thenReturn(true);
		when(rLock.isHeldByCurrentThread()).thenReturn(true);

		RedissonDistributedLock lock = new RedissonDistributedLock(client, "lock:a");
		assertThat(lock.tryLock()).isTrue();
		assertThat(lock.unlock()).isTrue();
		verify(rLock).unlock();
	}

	@Test
	void fixedLeaseTryLock() throws Exception {
		when(rLock.tryLock(0L, 30_000L, TimeUnit.MILLISECONDS)).thenReturn(true);

		RedissonDistributedLock lock = new RedissonDistributedLock(client, "lock:b", Duration.ofSeconds(30));
		assertThat(lock.isWatchdogEnabled()).isFalse();
		assertThat(lock.tryLock()).isTrue();
	}

	@Test
	void tryRunAutoUnlock() throws Exception {
		when(rLock.tryLock(0L, -1L, TimeUnit.MILLISECONDS)).thenReturn(true);
		when(rLock.isHeldByCurrentThread()).thenReturn(true);

		AtomicBoolean ran = new AtomicBoolean(false);
		boolean ok = new LindRedisson(client).tryRun("k", () -> ran.set(true));
		assertThat(ok).isTrue();
		assertThat(ran).isTrue();
		verify(rLock).unlock();
	}

	@Test
	void tryRunReturnsFalseWhenLockBusy() throws Exception {
		when(rLock.tryLock(0L, -1L, TimeUnit.MILLISECONDS)).thenReturn(false);

		boolean ok = new LindRedisson(client).tryRun("k", () -> {
		});
		assertThat(ok).isFalse();
		verify(rLock, never()).unlock();
	}

	@Test
	void runThrowsWhenLockBusy() throws Exception {
		when(rLock.tryLock(0L, -1L, TimeUnit.MILLISECONDS)).thenReturn(false);
		when(rLock.getName()).thenReturn("k");

		assertThatThrownBy(() -> new LindRedisson(client).run("k", () -> {
		})).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void delayQueueScheduleAfterAndAt() {
		RedissonDelayTaskQueue queue = new RedissonDelayTaskQueue(client, "jobs:delay");
		queue.scheduleAfter("close-1", Duration.ofSeconds(30));
		queue.scheduleAfterMillis("close-2", 5_000);
		queue.scheduleAt("close-3", Instant.now().plusSeconds(10));

		verify(delayedQueue).offer(eq("close-1"), eq(30_000L), eq(TimeUnit.MILLISECONDS));
		verify(delayedQueue).offer(eq("close-2"), eq(5_000L), eq(TimeUnit.MILLISECONDS));
		verify(delayedQueue).offer(eq("close-3"), anyLong(), eq(TimeUnit.MILLISECONDS));
	}

	@Test
	void delayQueuePollDueDrainsBlockingQueue() {
		when(blockingQueue.poll()).thenReturn("a", "b", null);

		List<String> due = new RedissonDelayTaskQueue(client, "jobs:delay").pollDue();
		assertThat(due).containsExactly("a", "b");
	}

	@Test
	void delayQueuePollWithTimeout() throws Exception {
		when(blockingQueue.poll(1_000L, TimeUnit.MILLISECONDS)).thenReturn("task-1");

		assertThat(new RedissonDelayTaskQueue(client, "jobs:delay").poll(Duration.ofSeconds(1))).contains("task-1");
	}

	@Test
	void delayQueueCloseDestroys() {
		new RedissonDelayTaskQueue(client, "jobs:delay").close();
		verify(delayedQueue).destroy();
	}

	@Test
	void rejectsBlankKey() {
		assertThatThrownBy(() -> new RedissonDistributedLock(client, " ")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new RedissonDelayTaskQueue(client, " ")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void autoConfigurationCreatesBean() {
		RedissonStarterAutoConfiguration config = new RedissonStarterAutoConfiguration();
		LindRedisson bean = config.lindRedisson(client);
		assertThat(bean.client()).isSameAs(client);
	}

}

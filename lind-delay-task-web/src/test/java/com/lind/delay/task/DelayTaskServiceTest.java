package com.lind.delay.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lind.algorithm.wheel.HashedWheelTimer;
import com.lind.algorithm.wheel.Timeout;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DelayTaskServiceTest {

	@Mock
	private DelayTaskStore store;

	@Mock
	private HashedWheelTimer timer;

	@Mock
	private Timeout timeout;

	private DelayTaskHandlerRegistry registry;

	private DelayTaskService service;

	@BeforeEach
	void setUp() {
		registry = new DelayTaskHandlerRegistry();
		service = new DelayTaskService(store, timer, registry);
	}

	@Test
	void schedulePersistsCreatedAtPlusDelayAsExecuteAt() {
		AtomicLong capturedRemaining = new AtomicLong();
		when(timer.newTimeout(any(), anyLong(), eq(TimeUnit.MILLISECONDS))).thenAnswer(inv -> {
			capturedRemaining.set(inv.getArgument(1));
			return timeout;
		});

		long before = System.currentTimeMillis();
		String id = service.schedule("demo", "u1", 60_000L, "p");
		long after = System.currentTimeMillis();

		ArgumentCaptor<DelayTask> captor = ArgumentCaptor.forClass(DelayTask.class);
		verify(store).insert(captor.capture());
		DelayTask saved = captor.getValue();
		assertThat(id).isEqualTo(saved.getId());
		assertThat(saved.getDelayMs()).isEqualTo(60_000L);
		assertThat(saved.getExecuteAt()).isEqualTo(saved.getCreatedAt() + 60_000L);
		assertThat(saved.getCreatedAt()).isBetween(before, after);
		assertThat(capturedRemaining.get()).isBetween(55_000L, 60_000L);
	}

}

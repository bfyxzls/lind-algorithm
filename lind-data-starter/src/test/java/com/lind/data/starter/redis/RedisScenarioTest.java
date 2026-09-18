package com.lind.data.starter.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * Redis 场景单测（mock {@link StringRedisTemplate}，无需本机 Redis）。
 */
@ExtendWith(MockitoExtension.class)
class RedisScenarioTest {

	@Mock
	private StringRedisTemplate template;

	@Mock
	private ValueOperations<String, String> valueOps;

	@Mock
	private ZSetOperations<String, String> zSetOps;

	@BeforeEach
	void setUp() {
		lenient().when(template.opsForValue()).thenReturn(valueOps);
		lenient().when(template.opsForZSet()).thenReturn(zSetOps);
	}

	@Test
	void facadeExposesScenarioHelpers() {
		LindSpringRedis redis = new LindSpringRedis(template);
		assertThat(redis.template()).isSameAs(template);
		assertThat(redis.cache()).isNotNull();
		assertThat(redis.lock("k", Duration.ofSeconds(1))).isNotNull();
		assertThat(redis.rateLimiter("rl", 10, Duration.ofSeconds(1))).isNotNull();
		assertThat(redis.delayQueue("dq")).isNotNull();
		assertThat(redis.idGenerator("seq")).isNotNull();
	}

	@Test
	void cacheSetGetDelete() {
		when(valueOps.get("user:1")).thenReturn("{\"name\":\"lind\"}");
		when(template.delete("user:1")).thenReturn(true);

		SpringRedisCache cache = new SpringRedisCache(template);
		cache.set("user:1", "{\"name\":\"lind\"}", Duration.ofMinutes(10));
		cache.set("user:2", "plain");

		assertThat(cache.get("user:1")).contains("{\"name\":\"lind\"}");
		assertThat(cache.delete("user:1")).isTrue();

		verify(valueOps).set(eq("user:1"), eq("{\"name\":\"lind\"}"), eq(Duration.ofMinutes(10)));
		verify(valueOps).set("user:2", "plain");
		verify(template).delete("user:1");
	}

	@Test
	void cacheRejectsBlankKey() {
		SpringRedisCache cache = new SpringRedisCache(template);
		assertThatThrownBy(() -> cache.set(" ", "v")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void lockTryAndUnlock() {
		when(valueOps.setIfAbsent(eq("lock:order:1"), anyString(), eq(Duration.ofSeconds(30)))).thenReturn(true);
		when(template.execute(any(RedisScript.class), anyList(), any())).thenReturn(1L);

		SpringRedisLock lock = new SpringRedisLock(template, "lock:order:1", Duration.ofSeconds(30));
		assertThat(lock.tryLock()).isTrue();
		assertThat(lock.unlock()).isTrue();
	}

	@Test
	void lockFailsWhenAlreadyHeld() {
		when(valueOps.setIfAbsent(eq("lock:x"), anyString(), any(Duration.class))).thenReturn(false);

		SpringRedisLock lock = new SpringRedisLock(template, "lock:x", Duration.ofSeconds(5));
		assertThat(lock.tryLock()).isFalse();
	}

	@Test
	void rateLimiterAllowsUnderLimit() {
		when(zSetOps.zCard("rl:api")).thenReturn(2L);
		when(zSetOps.add(eq("rl:api"), anyString(), anyDouble())).thenReturn(true);

		SpringRedisRateLimiter limiter = new SpringRedisRateLimiter(template, "rl:api", 10, Duration.ofSeconds(1));
		assertThat(limiter.tryAcquire()).isTrue();

		verify(zSetOps).removeRangeByScore(eq("rl:api"), eq(0D), anyDouble());
		verify(template).expire(eq("rl:api"), anyLong(), eq(TimeUnit.SECONDS));
	}

	@Test
	void rateLimiterRejectsWhenOverLimit() {
		when(zSetOps.zCard("rl:api")).thenReturn(10L);

		SpringRedisRateLimiter limiter = new SpringRedisRateLimiter(template, "rl:api", 10, Duration.ofSeconds(1));
		assertThat(limiter.tryAcquire()).isFalse();
		verify(zSetOps, never()).add(anyString(), anyString(), anyDouble());
	}

	@Test
	void delayQueueScheduleAndPollDue() {
		@SuppressWarnings("unchecked")
		ZSetOperations.TypedTuple<String> tuple = mock(ZSetOperations.TypedTuple.class);
		when(tuple.getValue()).thenReturn("close-order-1");
		Set<ZSetOperations.TypedTuple<String>> due = new HashSet<>();
		due.add(tuple);
		when(zSetOps.rangeByScoreWithScores(eq("jobs:delay"), eq(0D), anyDouble())).thenReturn(due);

		SpringRedisDelayQueue queue = new SpringRedisDelayQueue(template, "jobs:delay");
		queue.schedule("close-order-1", Instant.ofEpochMilli(1));
		queue.scheduleAfterMillis("notify-1", 5_000);

		List<String> messages = queue.pollDue();
		assertThat(messages).containsExactly("close-order-1");
		verify(zSetOps).add(eq("jobs:delay"), eq("close-order-1"), eq(1D));
		verify(zSetOps).add(eq("jobs:delay"), eq("notify-1"), anyDouble());
		verify(zSetOps).remove("jobs:delay", "close-order-1");
	}

	@Test
	void delayQueuePollEmpty() {
		when(zSetOps.rangeByScoreWithScores(eq("jobs:delay"), eq(0D), anyDouble())).thenReturn(Collections.emptySet());
		assertThat(new SpringRedisDelayQueue(template, "jobs:delay").pollDue()).isEmpty();
	}

	@Test
	void idGeneratorIncrement() {
		when(valueOps.increment("seq:order")).thenReturn(42L);
		when(valueOps.increment("seq:order", 100L)).thenReturn(142L);

		SpringRedisIdGenerator ids = new SpringRedisIdGenerator(template, "seq:order");
		assertThat(ids.nextId()).isEqualTo(42L);
		assertThat(ids.nextId(100)).isEqualTo(142L);
	}

	@Test
	void idGeneratorRejectsNonPositiveStep() {
		SpringRedisIdGenerator ids = new SpringRedisIdGenerator(template, "seq:order");
		assertThatThrownBy(() -> ids.nextId(0)).isInstanceOf(IllegalArgumentException.class);
	}

}

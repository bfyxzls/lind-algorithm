package com.lind.data.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.resps.Tuple;

/**
 * Redis 场景封装单元测试（mock {@link RedisCommands}，无需本机 Redis）。
 */
@ExtendWith(MockitoExtension.class)
class RedisScenarioTest {

	@Mock
	private RedisCommands redis;

	@Test
	void cacheSetWithTtlAndGet() {
		when(redis.get("k")).thenReturn("v");
		RedisCache cache = new RedisCache(redis);
		cache.set("k", "v", Duration.ofSeconds(30));
		verify(redis).set(eq("k"), eq("v"), any(SetParams.class));
		assertEquals("v", cache.get("k").orElseThrow());
	}

	@Test
	void lockTryAndUnlock() {
		when(redis.set(eq("lock:1"), anyString(), any(SetParams.class))).thenReturn("OK");
		when(redis.eval(anyString(), any(), any())).thenReturn(1L);

		RedisLock lock = new RedisLock(redis, "lock:1", Duration.ofSeconds(5));
		assertTrue(lock.tryLock());
		assertTrue(lock.unlock());
	}

	@Test
	void rateLimiterRejectsWhenOverLimit() {
		when(redis.zCard("rl")).thenReturn(10L);
		RedisRateLimiter limiter = new RedisRateLimiter(redis, "rl", 10, Duration.ofSeconds(1));
		assertFalse(limiter.tryAcquire());
	}

	@Test
	void rateLimiterAllowsWhenUnderLimit() {
		when(redis.zCard("rl")).thenReturn(1L);
		when(redis.zAdd(eq("rl"), anyDouble(), anyString())).thenReturn(1L);
		RedisRateLimiter limiter = new RedisRateLimiter(redis, "rl", 10, Duration.ofSeconds(1));
		assertTrue(limiter.tryAcquire());
	}

	@Test
	void bloomPutAndMightContain() {
		when(redis.getBit(eq("bf"), anyLong())).thenReturn(true);
		RedisBloomFilter bloom = new RedisBloomFilter(redis, "bf", 1000, 0.01);
		bloom.put("apple");
		verify(redis, org.mockito.Mockito.atLeastOnce()).setBit(eq("bf"), anyLong(), eq(true));
		assertTrue(bloom.mightContain("apple"));
	}

	@Test
	void rankingTop() {
		when(redis.zRevRangeWithScores("rank", 0, 1))
				.thenReturn(List.of(new Tuple("alice", 100D), new Tuple("bob", 80D)));
		RedisRanking ranking = new RedisRanking(redis, "rank");
		List<RedisRanking.Entry> top = ranking.top(2);
		assertEquals("alice", top.get(0).member());
		assertEquals(100D, top.get(0).score());
	}

	@Test
	void listQueueAndDelayQueue() {
		when(redis.lPush("q", "m1")).thenReturn(1L);
		when(redis.rPop("q")).thenReturn("m1");
		RedisListQueue queue = new RedisListQueue(redis, "q");
		assertEquals(1L, queue.offer("m1"));
		assertEquals("m1", queue.poll().orElseThrow());

		when(redis.zRangeByScoreWithScores(eq("dq"), eq(0D), anyDouble())).thenReturn(List.of(new Tuple("job-1", 1D)));
		RedisDelayQueue delay = new RedisDelayQueue(redis, "dq");
		delay.schedule("job-1", Instant.ofEpochMilli(1));
		assertEquals(List.of("job-1"), delay.pollDue());
		verify(redis).zRem("dq", "job-1");
	}

	@Test
	void bitmapHyperLogLogIdAndSocial() {
		when(redis.setBit("bm", 3, true)).thenReturn(false);
		when(redis.getBit("bm", 3)).thenReturn(true);
		when(redis.bitCount("bm")).thenReturn(1L);
		RedisBitmap bitmap = new RedisBitmap(redis, "bm");
		bitmap.set(3, true);
		assertTrue(bitmap.get(3));
		assertEquals(1L, bitmap.cardinality());

		when(redis.pfAdd("uv", "u1")).thenReturn(1L);
		when(redis.pfCount("uv")).thenReturn(1L);
		RedisHyperLogLog hll = new RedisHyperLogLog(redis, "uv");
		assertTrue(hll.add("u1"));
		assertEquals(1L, hll.count());

		when(redis.incr("seq")).thenReturn(100L);
		assertEquals(100L, new RedisIdGenerator(redis, "seq").nextId());

		RedisSocialGraph social = new RedisSocialGraph(redis, "s");
		social.follow("a", "b");
		verify(redis).sAdd("s:following:a", "b");
		verify(redis).sAdd("s:followers:b", "a");
		when(redis.sInter("s:following:a", "s:following:c")).thenReturn(Set.of("b"));
		assertEquals(Set.of("b"), social.commonFollowing("a", "c"));
	}

	@Test
	void lindRedisFacadeCreatesComponents() {
		LindRedis lind = new LindRedis(redis);
		assertEquals(redis, lind.commands());
		assertFalse(lind.cache() == null);
		assertFalse(lind.lock("k", Duration.ofSeconds(1)) == null);
		assertFalse(lind.rateLimiter("k", 1, Duration.ofSeconds(1)) == null);
		assertFalse(lind.bloom("k", 100, 0.01) == null);
		assertFalse(lind.geo("g") == null);
		assertFalse(lind.ranking("r") == null);
		assertFalse(lind.listQueue("q") == null);
		assertFalse(lind.delayQueue("d") == null);
		assertFalse(lind.pubSub() == null);
		assertFalse(lind.bitmap("b") == null);
		assertFalse(lind.hyperLogLog("h") == null);
		assertFalse(lind.social() == null);
		assertFalse(lind.idGenerator("i") == null);
	}

	@Test
	void pubSubPublish() {
		when(redis.publish("ch", "hi")).thenReturn(2L);
		assertEquals(2L, new RedisPubSub(redis).publish("ch", "hi"));
	}

}

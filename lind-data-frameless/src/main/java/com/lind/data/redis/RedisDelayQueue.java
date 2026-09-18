package com.lind.data.redis;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import redis.clients.jedis.resps.Tuple;

/**
 * 延迟队列：ZSET score=执行时间戳（毫秒），到期后取出。
 */
public final class RedisDelayQueue {

	private final RedisCommands redis;

	private final String key;

	public RedisDelayQueue(RedisCommands redis, String key) {
		this.redis = Objects.requireNonNull(redis, "redis");
		this.key = Objects.requireNonNull(key, "key");
	}

	public void schedule(String message, Instant executeAt) {
		Objects.requireNonNull(message, "message");
		Objects.requireNonNull(executeAt, "executeAt");
		redis.zAdd(key, executeAt.toEpochMilli(), message);
	}

	public void scheduleAfterMillis(String message, long delayMillis) {
		schedule(message, Instant.ofEpochMilli(System.currentTimeMillis() + delayMillis));
	}

	/**
	 * 取出所有已到期任务（score &lt;= now），并移除。
	 */
	public List<String> pollDue() {
		long now = System.currentTimeMillis();
		List<Tuple> due = redis.zRangeByScoreWithScores(key, 0, now);
		if (due.isEmpty()) {
			return List.of();
		}
		List<String> messages = new ArrayList<>(due.size());
		for (Tuple t : due) {
			messages.add(t.getElement());
			redis.zRem(key, t.getElement());
		}
		return Collections.unmodifiableList(messages);
	}

	public long size() {
		return redis.zCard(key);
	}

}

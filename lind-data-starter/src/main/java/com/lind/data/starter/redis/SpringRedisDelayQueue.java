package com.lind.data.starter.redis;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

/**
 * 延迟队列：ZSET score=执行时间戳（毫秒）。
 */
public class SpringRedisDelayQueue {

	private final StringRedisTemplate redis;

	private final String key;

	public SpringRedisDelayQueue(StringRedisTemplate redis, String key) {
		this.redis = Objects.requireNonNull(redis, "redis");
		this.key = Objects.requireNonNull(key, "key");
	}

	public void schedule(String message, Instant executeAt) {
		redis.opsForZSet().add(key, Objects.requireNonNull(message, "message"),
				Objects.requireNonNull(executeAt, "executeAt").toEpochMilli());
	}

	public void scheduleAfterMillis(String message, long delayMillis) {
		schedule(message, Instant.ofEpochMilli(System.currentTimeMillis() + delayMillis));
	}

	public List<String> pollDue() {
		long now = System.currentTimeMillis();
		Set<ZSetOperations.TypedTuple<String>> due = redis.opsForZSet().rangeByScoreWithScores(key, 0, now);
		if (due == null || due.isEmpty()) {
			return List.of();
		}
		List<String> messages = new ArrayList<>(due.size());
		for (ZSetOperations.TypedTuple<String> t : due) {
			if (t.getValue() != null) {
				messages.add(t.getValue());
				redis.opsForZSet().remove(key, t.getValue());
			}
		}
		return Collections.unmodifiableList(messages);
	}

}

package com.lind.data.redis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import redis.clients.jedis.resps.Tuple;

/**
 * 排行榜 Top-N：ZSET score 为分数。
 */
public final class RedisRanking {

	private final RedisCommands redis;

	private final String key;

	public RedisRanking(RedisCommands redis, String key) {
		this.redis = Objects.requireNonNull(redis, "redis");
		this.key = Objects.requireNonNull(key, "key");
	}

	public void addScore(String member, double score) {
		redis.zAdd(key, score, Objects.requireNonNull(member, "member"));
	}

	public void increaseScore(String member, double delta) {
		Objects.requireNonNull(member, "member");
		Double current = redis.zScore(key, member);
		redis.zAdd(key, (current == null ? 0D : current) + delta, member);
	}

	public List<Entry> top(int n) {
		if (n <= 0) {
			throw new IllegalArgumentException("n must be > 0");
		}
		List<Tuple> tuples = redis.zRevRangeWithScores(key, 0, n - 1L);
		List<Entry> result = new ArrayList<>(tuples.size());
		for (Tuple t : tuples) {
			result.add(new Entry(t.getElement(), t.getScore()));
		}
		return Collections.unmodifiableList(result);
	}

	public long rankDesc(String member) {
		return redis.zRevRank(key, Objects.requireNonNull(member, "member"));
	}

	public record Entry(String member, double score) {
	}

}

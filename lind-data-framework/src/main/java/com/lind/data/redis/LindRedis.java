package com.lind.data.redis;

import java.time.Duration;
import java.util.Objects;

/**
 * Redis 场景门面：基于 Jedis，按业务场景创建封装组件。
 *
 * <pre>
 * try (LindRedis redis = LindRedis.connect("127.0.0.1", 6379)) {
 *     redis.cache().set("k", "v", Duration.ofMinutes(5));
 * }
 * </pre>
 */
public final class LindRedis implements AutoCloseable {

	private final RedisCommands commands;

	public LindRedis(RedisCommands commands) {
		this.commands = Objects.requireNonNull(commands, "commands");
	}

	public static LindRedis connect(String host, int port) {
		return new LindRedis(JedisRedisCommands.connect(host, port));
	}

	public static LindRedis connect(String host, int port, String password) {
		return new LindRedis(JedisRedisCommands.connect(host, port, password));
	}

	public RedisCommands commands() {
		return commands;
	}

	public RedisCache cache() {
		return new RedisCache(commands);
	}

	public RedisLock lock(String key, Duration lease) {
		return new RedisLock(commands, key, lease);
	}

	public RedisRateLimiter rateLimiter(String key, int limit, Duration window) {
		return new RedisRateLimiter(commands, key, limit, window);
	}

	public RedisBloomFilter bloom(String key, long expectedInsertions, double falsePositiveRate) {
		return new RedisBloomFilter(commands, key, expectedInsertions, falsePositiveRate);
	}

	public RedisGeo geo(String key) {
		return new RedisGeo(commands, key);
	}

	public RedisRanking ranking(String key) {
		return new RedisRanking(commands, key);
	}

	public RedisListQueue listQueue(String key) {
		return new RedisListQueue(commands, key);
	}

	public RedisDelayQueue delayQueue(String key) {
		return new RedisDelayQueue(commands, key);
	}

	public RedisPubSub pubSub() {
		return new RedisPubSub(commands);
	}

	public RedisBitmap bitmap(String key) {
		return new RedisBitmap(commands, key);
	}

	public RedisHyperLogLog hyperLogLog(String key) {
		return new RedisHyperLogLog(commands, key);
	}

	public RedisSocialGraph social() {
		return new RedisSocialGraph(commands);
	}

	public RedisSocialGraph social(String keyPrefix) {
		return new RedisSocialGraph(commands, keyPrefix);
	}

	public RedisIdGenerator idGenerator(String key) {
		return new RedisIdGenerator(commands, key);
	}

	@Override
	public void close() {
		commands.close();
	}

}

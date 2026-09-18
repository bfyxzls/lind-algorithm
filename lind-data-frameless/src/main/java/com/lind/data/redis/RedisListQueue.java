package com.lind.data.redis;

import java.util.Objects;
import java.util.Optional;

/**
 * 简易消息队列：List（LPUSH 生产 / BRPOPLPUSH 或 RPOP 消费）。
 */
public final class RedisListQueue {

	private final RedisCommands redis;

	private final String key;

	public RedisListQueue(RedisCommands redis, String key) {
		this.redis = Objects.requireNonNull(redis, "redis");
		this.key = Objects.requireNonNull(key, "key");
	}

	public long offer(String message) {
		return redis.lPush(key, Objects.requireNonNull(message, "message"));
	}

	public Optional<String> poll() {
		return Optional.ofNullable(redis.rPop(key));
	}

	/**
	 * 可靠消费：阻塞弹出并放入 processing 列表。
	 */
	public Optional<String> blockingReliablePoll(String processingKey, int timeoutSeconds) {
		Objects.requireNonNull(processingKey, "processingKey");
		return Optional.ofNullable(redis.bRPopLPush(key, processingKey, timeoutSeconds));
	}

	public long size() {
		return redis.lLen(key);
	}

}

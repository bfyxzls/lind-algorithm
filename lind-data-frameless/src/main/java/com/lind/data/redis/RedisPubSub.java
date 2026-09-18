package com.lind.data.redis;

import java.util.Objects;

/**
 * 发布订阅。
 * <p>
 * 注意：{@link #subscribe} 会阻塞当前线程。
 * </p>
 */
public final class RedisPubSub {

	private final RedisCommands redis;

	public RedisPubSub(RedisCommands redis) {
		this.redis = Objects.requireNonNull(redis, "redis");
	}

	public long publish(String channel, String message) {
		return redis.publish(Objects.requireNonNull(channel, "channel"), Objects.requireNonNull(message, "message"));
	}

	public void subscribe(RedisMessageListener listener, String... channels) {
		if (channels == null || channels.length == 0) {
			throw new IllegalArgumentException("channels must not be empty");
		}
		redis.subscribe(listener, channels);
	}

}

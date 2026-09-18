package com.lind.data.redis;

/**
 * Pub/Sub 消息监听。
 */
@FunctionalInterface
public interface RedisMessageListener {

	void onMessage(String channel, String message);

}

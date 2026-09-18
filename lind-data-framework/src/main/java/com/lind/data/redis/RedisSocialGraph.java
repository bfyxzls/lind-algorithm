package com.lind.data.redis;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * 社交关系：Set 实现关注/粉丝、共同关注、可能认识的人（推荐雏形）。
 */
public final class RedisSocialGraph {

	private final RedisCommands redis;

	private final String keyPrefix;

	public RedisSocialGraph(RedisCommands redis) {
		this(redis, "social");
	}

	public RedisSocialGraph(RedisCommands redis, String keyPrefix) {
		this.redis = Objects.requireNonNull(redis, "redis");
		this.keyPrefix = Objects.requireNonNull(keyPrefix, "keyPrefix");
	}

	public void follow(String userId, String targetUserId) {
		redis.sAdd(followingKey(userId), requireId(targetUserId));
		redis.sAdd(followersKey(targetUserId), requireId(userId));
	}

	public void unfollow(String userId, String targetUserId) {
		redis.sRem(followingKey(userId), requireId(targetUserId));
		redis.sRem(followersKey(targetUserId), requireId(userId));
	}

	public boolean isFollowing(String userId, String targetUserId) {
		return redis.sIsMember(followingKey(userId), requireId(targetUserId));
	}

	public Set<String> following(String userId) {
		return Collections.unmodifiableSet(redis.sMembers(followingKey(userId)));
	}

	public Set<String> followers(String userId) {
		return Collections.unmodifiableSet(redis.sMembers(followersKey(userId)));
	}

	/** 共同关注。 */
	public Set<String> commonFollowing(String userA, String userB) {
		return Collections.unmodifiableSet(redis.sInter(followingKey(userA), followingKey(userB)));
	}

	/** 共同粉丝。 */
	public Set<String> commonFollowers(String userA, String userB) {
		return Collections.unmodifiableSet(redis.sInter(followersKey(userA), followersKey(userB)));
	}

	/**
	 * 简易推荐：我关注的人关注了、但我还没关注的人（二度人脉差集雏形，需业务侧再筛）。
	 */
	public Set<String> recommendByFollowing(String userId, String friendId) {
		return Collections.unmodifiableSet(redis.sDiff(followingKey(friendId), followingKey(userId)));
	}

	private String followingKey(String userId) {
		return keyPrefix + ":following:" + requireId(userId);
	}

	private String followersKey(String userId) {
		return keyPrefix + ":followers:" + requireId(userId);
	}

	private static String requireId(String id) {
		Objects.requireNonNull(id, "id");
		if (id.isBlank()) {
			throw new IllegalArgumentException("id must not be blank");
		}
		return id;
	}

}

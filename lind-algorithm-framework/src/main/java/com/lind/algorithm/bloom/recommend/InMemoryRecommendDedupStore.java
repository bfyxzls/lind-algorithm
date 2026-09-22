package com.lind.algorithm.bloom.recommend;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程内推荐去重存储。生产可换成 Redis / 曝光日志库。
 */
public final class InMemoryRecommendDedupStore implements RecommendDedupStore {

	private final Set<String> keys = ConcurrentHashMap.newKeySet();

	@Override
	public void add(String key) {
		keys.add(requireKey(key));
	}

	@Override
	public boolean contains(String key) {
		return keys.contains(requireKey(key));
	}

	@Override
	public int size() {
		return keys.size();
	}

	@Override
	public Collection<String> keys() {
		return List.copyOf(keys);
	}

	private static String requireKey(String key) {
		Objects.requireNonNull(key, "key");
		if (key.isEmpty()) {
			throw new IllegalArgumentException("key must not be empty");
		}
		return key;
	}

}

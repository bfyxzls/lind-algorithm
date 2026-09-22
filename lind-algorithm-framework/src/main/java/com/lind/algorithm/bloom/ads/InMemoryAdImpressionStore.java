package com.lind.algorithm.bloom.ads;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程内广告曝光记录。生产可换成 Redis 计数器 / 实时频控服务。
 */
public final class InMemoryAdImpressionStore implements AdImpressionStore {

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

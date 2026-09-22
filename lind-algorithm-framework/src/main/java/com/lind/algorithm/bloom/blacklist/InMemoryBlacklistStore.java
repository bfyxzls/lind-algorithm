package com.lind.algorithm.bloom.blacklist;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程内黑名单真相源。生产环境换成数据库或 Redis Set，接口不变。
 */
public final class InMemoryBlacklistStore implements BlacklistStore {

	private final Set<String> keys = ConcurrentHashMap.newKeySet();

	@Override
	public void add(String key) {
		keys.add(requireKey(key));
	}

	@Override
	public boolean remove(String key) {
		return keys.remove(requireKey(key));
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

package com.lind.algorithm.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * LRU 缓存：基于 access-order {@link LinkedHashMap}，容量满时淘汰最久未访问条目。
 *
 * @param <K> 键
 * @param <V> 值
 */
public final class LruCache<K, V> {

	private final int capacity;

	private final Map<K, V> map;

	public LruCache(int capacity) {
		if (capacity <= 0) {
			throw new IllegalArgumentException("capacity must be > 0");
		}
		this.capacity = capacity;
		this.map = new LinkedHashMap<>(16, 0.75f, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
				return size() > LruCache.this.capacity;
			}
		};
	}

	public synchronized void put(K key, V value) {
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(value, "value");
		map.put(key, value);
	}

	public synchronized Optional<V> get(K key) {
		Objects.requireNonNull(key, "key");
		return Optional.ofNullable(map.get(key));
	}

	public synchronized boolean containsKey(K key) {
		return map.containsKey(key);
	}

	public synchronized int size() {
		return map.size();
	}

	public int capacity() {
		return capacity;
	}

	public synchronized void clear() {
		map.clear();
	}

}

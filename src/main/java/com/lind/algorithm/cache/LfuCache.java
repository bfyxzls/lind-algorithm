package com.lind.algorithm.cache;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * LFU 缓存：按访问频次淘汰；同频次按插入/访问顺序淘汰最旧（Tiny 近似）。
 *
 * @param <K> 键
 * @param <V> 值
 */
public final class LfuCache<K, V> {

	private final int capacity;

	private final Map<K, Node<K, V>> values = new HashMap<>();

	private final TreeMap<Integer, LinkedHashSet<K>> frequencies = new TreeMap<>();

	public LfuCache(int capacity) {
		if (capacity <= 0) {
			throw new IllegalArgumentException("capacity must be > 0");
		}
		this.capacity = capacity;
	}

	public synchronized void put(K key, V value) {
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(value, "value");
		if (values.containsKey(key)) {
			Node<K, V> node = values.get(key);
			node.value = value;
			touch(node);
			return;
		}
		if (values.size() >= capacity) {
			evict();
		}
		Node<K, V> node = new Node<>(key, value, 1);
		values.put(key, node);
		frequencies.computeIfAbsent(1, f -> new LinkedHashSet<>()).add(key);
	}

	public synchronized Optional<V> get(K key) {
		Objects.requireNonNull(key, "key");
		Node<K, V> node = values.get(key);
		if (node == null) {
			return Optional.empty();
		}
		touch(node);
		return Optional.of(node.value);
	}

	public synchronized int size() {
		return values.size();
	}

	public int capacity() {
		return capacity;
	}

	private void touch(Node<K, V> node) {
		int oldFreq = node.freq;
		LinkedHashSet<K> set = frequencies.get(oldFreq);
		set.remove(node.key);
		if (set.isEmpty()) {
			frequencies.remove(oldFreq);
		}
		node.freq++;
		frequencies.computeIfAbsent(node.freq, f -> new LinkedHashSet<>()).add(node.key);
	}

	private void evict() {
		Map.Entry<Integer, LinkedHashSet<K>> entry = frequencies.firstEntry();
		Iterator<K> it = entry.getValue().iterator();
		K evictKey = it.next();
		it.remove();
		if (entry.getValue().isEmpty()) {
			frequencies.remove(entry.getKey());
		}
		values.remove(evictKey);
	}

	private static final class Node<K, V> {

		private final K key;

		private V value;

		private int freq;

		private Node(K key, V value, int freq) {
			this.key = key;
			this.value = value;
			this.freq = freq;
		}

	}

}

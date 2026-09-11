package com.lind.algorithm.skiplist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 跳表：多层链表实现的有序 KV，期望查找 / 插入 O(log n)，对齐 Redis ZSET 底层思想。
 *
 * @param <K> 可比较键
 * @param <V> 值
 */
public final class SkipListMap<K extends Comparable<K>, V> {

	private static final int MAX_LEVEL = 32;

	private static final double P = 0.25;

	private final Node<K, V> head = new Node<>(null, null, MAX_LEVEL);

	private int level = 1;

	private int size;

	public int size() {
		return size;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public Optional<V> get(K key) {
		Objects.requireNonNull(key, "key");
		Node<K, V> node = findNode(key);
		return node == null ? Optional.empty() : Optional.ofNullable(node.value);
	}

	public boolean containsKey(K key) {
		return get(key).isPresent();
	}

	public void put(K key, V value) {
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(value, "value");
		@SuppressWarnings("unchecked")
		Node<K, V>[] update = (Node<K, V>[]) new Node<?, ?>[MAX_LEVEL];
		Node<K, V> current = head;
		for (int i = level - 1; i >= 0; i--) {
			while (current.next[i] != null && current.next[i].key.compareTo(key) < 0) {
				current = current.next[i];
			}
			update[i] = current;
		}
		current = current.next[0];
		if (current != null && current.key.compareTo(key) == 0) {
			current.value = value;
			return;
		}
		int newLevel = randomLevel();
		if (newLevel > level) {
			for (int i = level; i < newLevel; i++) {
				update[i] = head;
			}
			level = newLevel;
		}
		Node<K, V> node = new Node<>(key, value, newLevel);
		for (int i = 0; i < newLevel; i++) {
			node.next[i] = update[i].next[i];
			update[i].next[i] = node;
		}
		size++;
	}

	public boolean remove(K key) {
		Objects.requireNonNull(key, "key");
		@SuppressWarnings("unchecked")
		Node<K, V>[] update = (Node<K, V>[]) new Node<?, ?>[MAX_LEVEL];
		Node<K, V> current = head;
		for (int i = level - 1; i >= 0; i--) {
			while (current.next[i] != null && current.next[i].key.compareTo(key) < 0) {
				current = current.next[i];
			}
			update[i] = current;
		}
		current = current.next[0];
		if (current == null || current.key.compareTo(key) != 0) {
			return false;
		}
		for (int i = 0; i < level; i++) {
			if (update[i].next[i] != current) {
				break;
			}
			update[i].next[i] = current.next[i];
		}
		while (level > 1 && head.next[level - 1] == null) {
			level--;
		}
		size--;
		return true;
	}

	public List<K> keys() {
		List<K> keys = new ArrayList<>(size);
		Node<K, V> node = head.next[0];
		while (node != null) {
			keys.add(node.key);
			node = node.next[0];
		}
		return Collections.unmodifiableList(keys);
	}

	private Node<K, V> findNode(K key) {
		Node<K, V> current = head;
		for (int i = level - 1; i >= 0; i--) {
			while (current.next[i] != null && current.next[i].key.compareTo(key) < 0) {
				current = current.next[i];
			}
		}
		current = current.next[0];
		if (current != null && current.key.compareTo(key) == 0) {
			return current;
		}
		return null;
	}

	private static int randomLevel() {
		int lvl = 1;
		ThreadLocalRandom random = ThreadLocalRandom.current();
		while (lvl < MAX_LEVEL && random.nextDouble() < P) {
			lvl++;
		}
		return lvl;
	}

	private static final class Node<K, V> {

		private final K key;

		private V value;

		private final Node<K, V>[] next;

		@SuppressWarnings("unchecked")
		private Node(K key, V value, int level) {
			this.key = key;
			this.value = value;
			this.next = (Node<K, V>[]) new Node<?, ?>[level];
		}

	}

}

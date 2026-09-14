package com.lind.algorithm.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 内存 B+ 树：所有数据在叶子节点，内部节点仅存分隔键；叶子按键有序串联，支持点查与范围扫描。
 * <p>
 * {@code order} 表示单个节点最多键数（须 ≥ 3）。内部节点最多 {@code order + 1} 个子节点。
 * </p>
 *
 * @param <K> 可比较键
 * @param <V> 值
 */
public class BPlusTree<K extends Comparable<K>, V> {

	private final int order;

	private Node<K, V> root;

	private int size;

	public BPlusTree() {
		this(4);
	}

	public BPlusTree(int order) {
		if (order < 3) {
			throw new IllegalArgumentException("order must be >= 3");
		}
		this.order = order;
		this.root = new LeafNode<>();
	}

	public int order() {
		return order;
	}

	public int size() {
		return size;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * 插入或覆盖。
	 */
	public void put(K key, V value) {
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(value, "value");
		SplitResult<K, V> split = root.insert(key, value, order);
		if (split.replaced) {
			return;
		}
		if (!split.grew) {
			size++;
		}
		if (split.newNode != null) {
			InternalNode<K, V> newRoot = new InternalNode<>();
			newRoot.keys.add(split.separator);
			newRoot.children.add(root);
			newRoot.children.add(split.newNode);
			root = newRoot;
		}
	}

	public Optional<V> get(K key) {
		Objects.requireNonNull(key, "key");
		return root.find(key);
	}

	public boolean containsKey(K key) {
		return get(key).isPresent();
	}

	/**
	 * 闭区间 [from, to] 范围查询（两端均可为 null 表示开放）。
	 */
	public List<Entry<K, V>> range(K from, K to) {
		if (from != null && to != null && from.compareTo(to) > 0) {
			throw new IllegalArgumentException("from must be <= to");
		}
		List<Entry<K, V>> result = new ArrayList<>();
		LeafNode<K, V> leaf = root.leftmostLeaf();
		while (leaf != null) {
			for (int i = 0; i < leaf.keys.size(); i++) {
				K key = leaf.keys.get(i);
				if (from != null && key.compareTo(from) < 0) {
					continue;
				}
				if (to != null && key.compareTo(to) > 0) {
					return Collections.unmodifiableList(result);
				}
				result.add(new Entry<>(key, leaf.values.get(i)));
			}
			leaf = leaf.next;
		}
		return Collections.unmodifiableList(result);
	}

	public List<K> keys() {
		List<K> keys = new ArrayList<>(size);
		for (Entry<K, V> e : range(null, null)) {
			keys.add(e.key());
		}
		return Collections.unmodifiableList(keys);
	}

	public record Entry<K, V>(K key, V value) {
	}

	private static final class SplitResult<K extends Comparable<K>, V> {

		private final K separator;

		private final Node<K, V> newNode;

		private final boolean replaced;

		private final boolean grew;

		private SplitResult(K separator, Node<K, V> newNode, boolean replaced, boolean grew) {
			this.separator = separator;
			this.newNode = newNode;
			this.replaced = replaced;
			this.grew = grew;
		}

		static <K extends Comparable<K>, V> SplitResult<K, V> none(boolean replaced, boolean grew) {
			return new SplitResult<>(null, null, replaced, grew);
		}

		static <K extends Comparable<K>, V> SplitResult<K, V> split(K separator, Node<K, V> right, boolean grew) {
			return new SplitResult<>(separator, right, false, grew);
		}

	}

	private abstract static class Node<K extends Comparable<K>, V> {

		final List<K> keys = new ArrayList<>();

		abstract Optional<V> find(K key);

		abstract SplitResult<K, V> insert(K key, V value, int order);

		abstract LeafNode<K, V> leftmostLeaf();

	}

	private static final class InternalNode<K extends Comparable<K>, V> extends Node<K, V> {

		private final List<Node<K, V>> children = new ArrayList<>();

		@Override
		Optional<V> find(K key) {
			return children.get(childIndex(key)).find(key);
		}

		@Override
		SplitResult<K, V> insert(K key, V value, int order) {
			int idx = childIndex(key);
			SplitResult<K, V> childSplit = children.get(idx).insert(key, value, order);
			if (childSplit.newNode == null) {
				return childSplit;
			}
			keys.add(idx, childSplit.separator);
			children.add(idx + 1, childSplit.newNode);
			if (keys.size() <= order) {
				return SplitResult.none(false, childSplit.grew);
			}
			return splitInternal(order, childSplit.grew);
		}

		private SplitResult<K, V> splitInternal(int order, boolean grew) {
			int mid = keys.size() / 2;
			K separator = keys.get(mid);
			InternalNode<K, V> right = new InternalNode<>();
			right.keys.addAll(keys.subList(mid + 1, keys.size()));
			right.children.addAll(children.subList(mid + 1, children.size()));
			keys.subList(mid, keys.size()).clear();
			children.subList(mid + 1, children.size()).clear();
			return SplitResult.split(separator, right, grew);
		}

		private int childIndex(K key) {
			int i = 0;
			while (i < keys.size() && key.compareTo(keys.get(i)) >= 0) {
				i++;
			}
			return i;
		}

		@Override
		LeafNode<K, V> leftmostLeaf() {
			return children.get(0).leftmostLeaf();
		}

	}

	private static final class LeafNode<K extends Comparable<K>, V> extends Node<K, V> {

		private final List<V> values = new ArrayList<>();

		private LeafNode<K, V> next;

		@Override
		Optional<V> find(K key) {
			int idx = Collections.binarySearch(keys, key);
			if (idx >= 0) {
				return Optional.of(values.get(idx));
			}
			return Optional.empty();
		}

		@Override
		SplitResult<K, V> insert(K key, V value, int order) {
			int idx = Collections.binarySearch(keys, key);
			if (idx >= 0) {
				values.set(idx, value);
				return SplitResult.none(true, false);
			}
			int insertAt = -idx - 1;
			keys.add(insertAt, key);
			values.add(insertAt, value);
			if (keys.size() <= order) {
				return SplitResult.none(false, false);
			}
			return splitLeaf(order);
		}

		private SplitResult<K, V> splitLeaf(int order) {
			int mid = (keys.size() + 1) / 2;
			LeafNode<K, V> right = new LeafNode<>();
			right.keys.addAll(keys.subList(mid, keys.size()));
			right.values.addAll(values.subList(mid, values.size()));
			keys.subList(mid, keys.size()).clear();
			values.subList(mid, values.size()).clear();
			right.next = this.next;
			this.next = right;
			return SplitResult.split(right.keys.get(0), right, false);
		}

		@Override
		LeafNode<K, V> leftmostLeaf() {
			return this;
		}

	}

}

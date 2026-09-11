package com.lind.algorithm.tree;

/**
 * 红黑树：自平衡二叉搜索树，插入后保持近似平衡，查找约为 O(log n)。
 *
 * @param <T> 可比较元素类型
 */
public class RedBlackTree<T extends Comparable<T>> {

	private static final boolean RED = false;

	private static final boolean BLACK = true;

	private Node<T> root;

	private int size;

	/**
	 * 插入元素；null 不允许。相等元素按右侧挂接（允许多次插入同值）。
	 */
	public void insert(T value) {
		if (value == null) {
			throw new IllegalArgumentException("value must not be null");
		}
		Node<T> node = new Node<>(value);
		if (root == null) {
			root = node;
			root.color = BLACK;
			size++;
			return;
		}
		Node<T> parent = null;
		Node<T> current = root;
		while (current != null) {
			parent = current;
			if (value.compareTo(current.value) < 0) {
				current = current.left;
			}
			else {
				current = current.right;
			}
		}
		node.parent = parent;
		if (value.compareTo(parent.value) < 0) {
			parent.left = node;
		}
		else {
			parent.right = node;
		}
		insertFixUp(node);
		size++;
	}

	/**
	 * 是否包含该元素。
	 */
	public boolean contains(T value) {
		return find(value) != null;
	}

	public int size() {
		return size;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	private Node<T> find(T value) {
		if (value == null) {
			return null;
		}
		Node<T> node = root;
		while (node != null) {
			int cmp = value.compareTo(node.value);
			if (cmp == 0) {
				return node;
			}
			node = cmp < 0 ? node.left : node.right;
		}
		return null;
	}

	private void insertFixUp(Node<T> node) {
		while (node.parent != null && node.parent.color == RED) {
			if (node.parent.parent == null) {
				break;
			}
			if (node.parent == node.parent.parent.left) {
				Node<T> uncle = node.parent.parent.right;
				if (uncle != null && uncle.color == RED) {
					node.parent.color = BLACK;
					uncle.color = BLACK;
					node.parent.parent.color = RED;
					node = node.parent.parent;
				}
				else {
					if (node == node.parent.right) {
						node = node.parent;
						leftRotate(node);
					}
					node.parent.color = BLACK;
					node.parent.parent.color = RED;
					rightRotate(node.parent.parent);
				}
			}
			else {
				Node<T> uncle = node.parent.parent.left;
				if (uncle != null && uncle.color == RED) {
					node.parent.color = BLACK;
					uncle.color = BLACK;
					node.parent.parent.color = RED;
					node = node.parent.parent;
				}
				else {
					if (node == node.parent.left) {
						node = node.parent;
						rightRotate(node);
					}
					node.parent.color = BLACK;
					node.parent.parent.color = RED;
					leftRotate(node.parent.parent);
				}
			}
		}
		root.color = BLACK;
	}

	private void leftRotate(Node<T> node) {
		Node<T> right = node.right;
		if (right == null) {
			return;
		}
		node.right = right.left;
		if (right.left != null) {
			right.left.parent = node;
		}
		right.parent = node.parent;
		if (node.parent == null) {
			root = right;
		}
		else if (node == node.parent.left) {
			node.parent.left = right;
		}
		else {
			node.parent.right = right;
		}
		right.left = node;
		node.parent = right;
	}

	private void rightRotate(Node<T> node) {
		Node<T> left = node.left;
		if (left == null) {
			return;
		}
		node.left = left.right;
		if (left.right != null) {
			left.right.parent = node;
		}
		left.parent = node.parent;
		if (node.parent == null) {
			root = left;
		}
		else if (node == node.parent.right) {
			node.parent.right = left;
		}
		else {
			node.parent.left = left;
		}
		left.right = node;
		node.parent = left;
	}

	private static final class Node<T> {

		private final T value;

		private Node<T> left;

		private Node<T> right;

		private Node<T> parent;

		private boolean color;

		private Node(T value) {
			this.value = value;
			this.color = RED;
		}

	}

}

package com.lind.algorithm.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 二叉树节点，提供前序 / 中序 / 后序遍历（返回节点值列表，不依赖控制台输出）。
 */
public class BinaryTreeNode {

	private int val;

	private BinaryTreeNode left;

	private BinaryTreeNode right;

	public BinaryTreeNode(int val) {
		this.val = val;
	}

	public int getVal() {
		return val;
	}

	public void setVal(int val) {
		this.val = val;
	}

	public BinaryTreeNode getLeft() {
		return left;
	}

	public void setLeft(BinaryTreeNode left) {
		this.left = left;
	}

	public BinaryTreeNode getRight() {
		return right;
	}

	public void setRight(BinaryTreeNode right) {
		this.right = right;
	}

	/**
	 * 前序遍历：根 -&gt; 左 -&gt; 右
	 */
	public static List<Integer> preOrder(BinaryTreeNode root) {
		List<Integer> result = new ArrayList<>();
		preOrder(root, result);
		return Collections.unmodifiableList(result);
	}

	/**
	 * 中序遍历：左 -&gt; 根 -&gt; 右
	 */
	public static List<Integer> inOrder(BinaryTreeNode root) {
		List<Integer> result = new ArrayList<>();
		inOrder(root, result);
		return Collections.unmodifiableList(result);
	}

	/**
	 * 后序遍历：左 -&gt; 右 -&gt; 根
	 */
	public static List<Integer> postOrder(BinaryTreeNode root) {
		List<Integer> result = new ArrayList<>();
		postOrder(root, result);
		return Collections.unmodifiableList(result);
	}

	private static void preOrder(BinaryTreeNode node, List<Integer> result) {
		if (node == null) {
			return;
		}
		result.add(node.val);
		preOrder(node.left, result);
		preOrder(node.right, result);
	}

	private static void inOrder(BinaryTreeNode node, List<Integer> result) {
		if (node == null) {
			return;
		}
		inOrder(node.left, result);
		result.add(node.val);
		inOrder(node.right, result);
	}

	private static void postOrder(BinaryTreeNode node, List<Integer> result) {
		if (node == null) {
			return;
		}
		postOrder(node.left, result);
		postOrder(node.right, result);
		result.add(node.val);
	}

	@Override
	public String toString() {
		return "BinaryTreeNode{val=" + val + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof BinaryTreeNode)) {
			return false;
		}
		BinaryTreeNode that = (BinaryTreeNode) o;
		return val == that.val && Objects.equals(left, that.left) && Objects.equals(right, that.right);
	}

	@Override
	public int hashCode() {
		return Objects.hash(val, left, right);
	}

}

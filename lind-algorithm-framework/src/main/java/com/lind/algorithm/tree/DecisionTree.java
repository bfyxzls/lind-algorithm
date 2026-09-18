package com.lind.algorithm.tree;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 决策树：按特征分支直到叶子标签，适用于规则引擎、风控分流、简单分类推断。
 * <p>
 * 本实现为可手工组装的分类树（离散特征等值分支）；不内置样本训练，便于业务规则可视化落地。
 * </p>
 */
public class DecisionTree {

	private final Node root;

	public DecisionTree(Node root) {
		this.root = Objects.requireNonNull(root, "root");
	}

	public Node root() {
		return root;
	}

	/**
	 * 根据特征映射预测叶子标签。
	 * @param features 特征名 → 特征值
	 */
	public String predict(Map<String, String> features) {
		Objects.requireNonNull(features, "features");
		Node current = root;
		while (!current.isLeaf()) {
			String value = features.get(current.feature());
			if (value == null) {
				throw new IllegalArgumentException("missing feature: " + current.feature());
			}
			Node next = current.branches().get(value);
			if (next == null) {
				if (current.defaultBranch() != null) {
					next = current.defaultBranch();
				}
				else {
					throw new IllegalArgumentException("no branch for feature " + current.feature() + " = " + value);
				}
			}
			current = next;
		}
		return current.label();
	}

	/**
	 * 决策树节点：内部节点按特征分支，叶子节点持有分类标签。
	 */
	public static final class Node {

		private final String feature;

		private final Map<String, Node> branches;

		private final Node defaultBranch;

		private final String label;

		private Node(String feature, Map<String, Node> branches, Node defaultBranch, String label) {
			this.feature = feature;
			this.branches = branches == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(branches));
			this.defaultBranch = defaultBranch;
			this.label = label;
		}

		public static Node leaf(String label) {
			Objects.requireNonNull(label, "label");
			return new Node(null, null, null, label);
		}

		public static Builder branch(String feature) {
			return new Builder(feature);
		}

		public boolean isLeaf() {
			return label != null;
		}

		public String feature() {
			return feature;
		}

		public Map<String, Node> branches() {
			return branches;
		}

		public Node defaultBranch() {
			return defaultBranch;
		}

		public String label() {
			return label;
		}

		public static final class Builder {

			private final String feature;

			private final Map<String, Node> branches = new LinkedHashMap<>();

			private Node defaultBranch;

			private Builder(String feature) {
				this.feature = Objects.requireNonNull(feature, "feature");
			}

			public Builder when(String value, Node child) {
				Objects.requireNonNull(value, "value");
				Objects.requireNonNull(child, "child");
				branches.put(value, child);
				return this;
			}

			public Builder otherwise(Node child) {
				this.defaultBranch = Objects.requireNonNull(child, "child");
				return this;
			}

			public Node build() {
				if (branches.isEmpty() && defaultBranch == null) {
					throw new IllegalStateException("branch node requires at least one branch");
				}
				return new Node(feature, branches, defaultBranch, null);
			}

		}

	}

}

package com.lind.algorithm.tree;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 多叉树（业务组织架构）：任意个子节点的树，面向部门 / 员工汇报线等纯业务层级。
 * <p>
 * 与 {@link Tree}（菜单契约 + type 过滤）互补：本类是通用 N 叉树容器，提供扁平组装、路径、遍历与查找。
 * </p>
 *
 * @param <T> 节点业务载荷
 */
public class MultiWayTree<T> {

	private final MultiWayNode<T> root;

	public MultiWayTree(MultiWayNode<T> root) {
		this.root = Objects.requireNonNull(root, "root");
	}

	public MultiWayNode<T> root() {
		return root;
	}

	/**
	 * 由扁平列表组装多叉树。根节点的 {@code parentId} 为 null 或空。
	 * @param nodes 扁平节点（须含唯一 id）
	 */
	public static <T> MultiWayTree<T> fromFlat(List<FlatNode<T>> nodes) {
		Objects.requireNonNull(nodes, "nodes");
		if (nodes.isEmpty()) {
			throw new IllegalArgumentException("nodes must not be empty");
		}
		Map<String, MultiWayNode<T>> index = new LinkedHashMap<>();
		for (FlatNode<T> flat : nodes) {
			Objects.requireNonNull(flat, "flat node");
			if (index.containsKey(flat.id())) {
				throw new IllegalArgumentException("duplicate id: " + flat.id());
			}
			index.put(flat.id(), new MultiWayNode<>(flat.id(), flat.payload()));
		}
		MultiWayNode<T> root = null;
		for (FlatNode<T> flat : nodes) {
			MultiWayNode<T> node = index.get(flat.id());
			String parentId = flat.parentId();
			if (parentId == null || parentId.isBlank()) {
				if (root != null) {
					throw new IllegalArgumentException("multiple roots: " + root.id() + " and " + node.id());
				}
				root = node;
			}
			else {
				MultiWayNode<T> parent = index.get(parentId);
				if (parent == null) {
					throw new IllegalArgumentException("missing parent: " + parentId + " for " + flat.id());
				}
				parent.addChild(node);
			}
		}
		if (root == null) {
			throw new IllegalArgumentException("no root node (parentId null/blank) found");
		}
		return new MultiWayTree<>(root);
	}

	public Optional<MultiWayNode<T>> findById(String id) {
		Objects.requireNonNull(id, "id");
		return find(root, id);
	}

	/**
	 * 从根到目标节点的路径（含两端）；不存在则 empty。
	 */
	public Optional<List<MultiWayNode<T>>> pathTo(String id) {
		Objects.requireNonNull(id, "id");
		List<MultiWayNode<T>> path = new ArrayList<>();
		if (dfsPath(root, id, path)) {
			return Optional.of(Collections.unmodifiableList(path));
		}
		return Optional.empty();
	}

	public int height() {
		return height(root);
	}

	public int size() {
		int[] count = { 0 };
		dfs(n -> count[0]++);
		return count[0];
	}

	public void dfs(Consumer<MultiWayNode<T>> visitor) {
		Objects.requireNonNull(visitor, "visitor");
		dfsVisit(root, visitor);
	}

	public void bfs(Consumer<MultiWayNode<T>> visitor) {
		Objects.requireNonNull(visitor, "visitor");
		Deque<MultiWayNode<T>> queue = new ArrayDeque<>();
		queue.add(root);
		while (!queue.isEmpty()) {
			MultiWayNode<T> n = queue.removeFirst();
			visitor.accept(n);
			queue.addAll(n.children());
		}
	}

	public List<T> dfsPayloads() {
		List<T> list = new ArrayList<>();
		dfs(n -> list.add(n.payload()));
		return Collections.unmodifiableList(list);
	}

	private static <T> Optional<MultiWayNode<T>> find(MultiWayNode<T> node, String id) {
		if (node.id().equals(id)) {
			return Optional.of(node);
		}
		for (MultiWayNode<T> child : node.children()) {
			Optional<MultiWayNode<T>> found = find(child, id);
			if (found.isPresent()) {
				return found;
			}
		}
		return Optional.empty();
	}

	private static <T> boolean dfsPath(MultiWayNode<T> node, String id, List<MultiWayNode<T>> path) {
		path.add(node);
		if (node.id().equals(id)) {
			return true;
		}
		for (MultiWayNode<T> child : node.children()) {
			if (dfsPath(child, id, path)) {
				return true;
			}
		}
		path.remove(path.size() - 1);
		return false;
	}

	private static <T> void dfsVisit(MultiWayNode<T> node, Consumer<MultiWayNode<T>> visitor) {
		visitor.accept(node);
		for (MultiWayNode<T> child : node.children()) {
			dfsVisit(child, visitor);
		}
	}

	private static <T> int height(MultiWayNode<T> node) {
		if (node.children().isEmpty()) {
			return 1;
		}
		int max = 0;
		for (MultiWayNode<T> child : node.children()) {
			max = Math.max(max, height(child));
		}
		return max + 1;
	}

	/**
	 * 扁平输入节点。
	 */
	public record FlatNode<T>(String id, String parentId, T payload) {

		public FlatNode {
			Objects.requireNonNull(id, "id");
		}

		public static <T> FlatNode<T> root(String id, T payload) {
			return new FlatNode<>(id, null, payload);
		}

		public static <T> FlatNode<T> of(String id, String parentId, T payload) {
			return new FlatNode<>(id, parentId, payload);
		}

	}

	/**
	 * 多叉树节点。
	 */
	public static final class MultiWayNode<T> {

		private final String id;

		private final T payload;

		private final List<MultiWayNode<T>> children = new ArrayList<>();

		public MultiWayNode(String id, T payload) {
			this.id = Objects.requireNonNull(id, "id");
			this.payload = payload;
		}

		public String id() {
			return id;
		}

		public T payload() {
			return payload;
		}

		public List<MultiWayNode<T>> children() {
			return Collections.unmodifiableList(children);
		}

		public void addChild(MultiWayNode<T> child) {
			children.add(Objects.requireNonNull(child, "child"));
		}

		public boolean isLeaf() {
			return children.isEmpty();
		}

	}

}

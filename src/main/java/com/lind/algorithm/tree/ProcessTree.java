package com.lind.algorithm.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 流程树：以树形编排业务流程节点（开始 / 任务 / 网关 / 结束），适合审批流、状态推进与编排演示。
 * <p>
 * XOR 网关按条件选择唯一分支；AND 网关按定义顺序依次执行全部子分支（简化语义，非并行引擎）。
 * </p>
 */
public class ProcessTree {

	private final ProcessNode root;

	public ProcessTree(ProcessNode root) {
		this.root = Objects.requireNonNull(root, "root");
		if (root.type() != NodeType.START) {
			throw new IllegalArgumentException("root must be START");
		}
	}

	public ProcessNode root() {
		return root;
	}

	/**
	 * 按树路径执行，返回经过的节点 id 列表。
	 */
	public List<String> execute(Map<String, Object> context) {
		Objects.requireNonNull(context, "context");
		List<String> trace = new ArrayList<>();
		executeNode(root, context, trace);
		return Collections.unmodifiableList(trace);
	}

	private void executeNode(ProcessNode node, Map<String, Object> context, List<String> trace) {
		trace.add(node.id());
		if (node.action() != null) {
			node.action().run(context);
		}
		switch (node.type()) {
			case START, TASK -> {
				if (!node.children().isEmpty()) {
					executeNode(node.children().get(0), context, trace);
				}
			}
			case XOR -> {
				for (ProcessNode child : node.children()) {
					if (child.condition() == null || child.condition().test(context)) {
						executeNode(child, context, trace);
						return;
					}
				}
				throw new IllegalStateException("no XOR branch matched at " + node.id());
			}
			case AND -> {
				for (ProcessNode child : node.children()) {
					executeNode(child, context, trace);
				}
			}
			case END -> {
				// terminal
			}
		}
	}

	public enum NodeType {

		START, TASK, XOR, AND, END

	}

	@FunctionalInterface
	public interface NodeAction {

		void run(Map<String, Object> context);

	}

	/**
	 * 流程节点。
	 */
	public static final class ProcessNode {

		private final String id;

		private final String name;

		private final NodeType type;

		private final Predicate<Map<String, Object>> condition;

		private final NodeAction action;

		private final List<ProcessNode> children;

		private ProcessNode(String id, String name, NodeType type, Predicate<Map<String, Object>> condition,
				NodeAction action, List<ProcessNode> children) {
			this.id = Objects.requireNonNull(id, "id");
			this.name = name == null ? id : name;
			this.type = Objects.requireNonNull(type, "type");
			this.condition = condition;
			this.action = action;
			this.children = List.copyOf(children == null ? List.of() : children);
		}

		public String id() {
			return id;
		}

		public String name() {
			return name;
		}

		public NodeType type() {
			return type;
		}

		public Predicate<Map<String, Object>> condition() {
			return condition;
		}

		public NodeAction action() {
			return action;
		}

		public List<ProcessNode> children() {
			return children;
		}

		public static Builder start(String id) {
			return new Builder(id, NodeType.START);
		}

		public static Builder task(String id) {
			return new Builder(id, NodeType.TASK);
		}

		public static Builder xor(String id) {
			return new Builder(id, NodeType.XOR);
		}

		public static Builder and(String id) {
			return new Builder(id, NodeType.AND);
		}

		public static Builder end(String id) {
			return new Builder(id, NodeType.END);
		}

		public static final class Builder {

			private final String id;

			private final NodeType type;

			private String name;

			private Predicate<Map<String, Object>> condition;

			private NodeAction action;

			private final List<ProcessNode> children = new ArrayList<>();

			private Builder(String id, NodeType type) {
				this.id = id;
				this.type = type;
			}

			public Builder name(String name) {
				this.name = name;
				return this;
			}

			public Builder when(Predicate<Map<String, Object>> condition) {
				this.condition = condition;
				return this;
			}

			public Builder action(NodeAction action) {
				this.action = action;
				return this;
			}

			public Builder child(ProcessNode child) {
				children.add(Objects.requireNonNull(child, "child"));
				return this;
			}

			public Builder children(ProcessNode... nodes) {
				for (ProcessNode n : nodes) {
					child(n);
				}
				return this;
			}

			public ProcessNode build() {
				validate();
				return new ProcessNode(id, name, type, condition, action, children);
			}

			private void validate() {
				Set<NodeType> terminal = EnumSet.of(NodeType.END);
				if (terminal.contains(type) && !children.isEmpty()) {
					throw new IllegalStateException("END node cannot have children");
				}
				if ((type == NodeType.START || type == NodeType.TASK) && children.size() > 1) {
					throw new IllegalStateException(type + " allows at most one child; use XOR/AND for branching");
				}
			}

		}

	}

	/**
	 * 可变执行上下文的便捷工厂。
	 */
	public static Map<String, Object> context(Object... keyValues) {
		if (keyValues.length % 2 != 0) {
			throw new IllegalArgumentException("keyValues must be even");
		}
		Map<String, Object> map = new LinkedHashMap<>();
		for (int i = 0; i < keyValues.length; i += 2) {
			map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
		}
		return map;
	}

}

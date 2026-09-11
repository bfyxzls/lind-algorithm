package com.lind.algorithm.stringmatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

/**
 * AC 自动机：多模式串匹配，适合敏感词、关键词一次扫描命中全部模式。
 */
public final class AhoCorasick {

	private final Node root = new Node();

	private boolean built;

	public void addKeyword(String keyword) {
		Objects.requireNonNull(keyword, "keyword");
		if (keyword.isEmpty()) {
			throw new IllegalArgumentException("keyword must not be empty");
		}
		if (built) {
			throw new IllegalStateException("cannot add keyword after build()");
		}
		Node node = root;
		for (int i = 0; i < keyword.length(); i++) {
			char ch = keyword.charAt(i);
			node = node.children.computeIfAbsent(ch, c -> new Node());
		}
		node.outputs.add(keyword);
	}

	public void addKeywords(Iterable<String> keywords) {
		for (String keyword : keywords) {
			addKeyword(keyword);
		}
	}

	public void build() {
		Queue<Node> queue = new LinkedList<>();
		for (Node child : root.children.values()) {
			child.fail = root;
			queue.add(child);
		}
		while (!queue.isEmpty()) {
			Node current = queue.remove();
			for (Map.Entry<Character, Node> entry : current.children.entrySet()) {
				char ch = entry.getKey();
				Node child = entry.getValue();
				Node fail = current.fail;
				while (fail != null && !fail.children.containsKey(ch)) {
					fail = fail.fail;
				}
				child.fail = (fail == null) ? root : fail.children.getOrDefault(ch, root);
				child.outputs.addAll(child.fail.outputs);
				queue.add(child);
			}
		}
		built = true;
	}

	/**
	 * 在文本中查找全部模式，返回命中列表（可重叠）。
	 */
	public List<Match> findAll(String text) {
		Objects.requireNonNull(text, "text");
		ensureBuilt();
		List<Match> matches = new ArrayList<>();
		Node node = root;
		for (int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);
			while (node != root && !node.children.containsKey(ch)) {
				node = node.fail;
			}
			node = node.children.getOrDefault(ch, root);
			for (String word : node.outputs) {
				int start = i - word.length() + 1;
				matches.add(new Match(word, start, i + 1));
			}
		}
		return Collections.unmodifiableList(matches);
	}

	public boolean containsAny(String text) {
		return !findAll(text).isEmpty();
	}

	private void ensureBuilt() {
		if (!built) {
			build();
		}
	}

	public record Match(String keyword, int start, int end) {
	}

	private static final class Node {

		private final Map<Character, Node> children = new HashMap<>();

		private Node fail;

		private final List<String> outputs = new ArrayList<>();

	}

}

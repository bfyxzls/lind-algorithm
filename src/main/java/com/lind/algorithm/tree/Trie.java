package com.lind.algorithm.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用字典树 / 前缀树（Trie）。
 * <p>
 * 子节点使用 {@link HashMap}，支持 Unicode（含中文）。提供：
 * <ul>
 * <li>整词插入 / 查询 / 前缀判断 / 词频</li>
 * <li>多关键词在文本中的词频扫描（敏感词、实体词统计等场景）</li>
 * <li>英文空白分词、基于词典的中文正向最大匹配分词</li>
 * </ul>
 * 文本扫描与中文分词均非 AC 自动机；词典规模很大或要更高召回时可接 HanLP / AC。
 * </p>
 */
public class Trie {

	private final Node root = new Node();

	/**
	 * 插入词条；重复插入时词频 +1。
	 */
	public void insert(String word) {
		String value = requireNonBlank(word);
		Node current = root;
		for (int i = 0; i < value.length(); i++) {
			char ch = value.charAt(i);
			current = current.children.computeIfAbsent(ch, c -> new Node());
		}
		current.endOfWord = true;
		current.freq++;
	}

	/**
	 * 批量插入词条（常用于加载中文分词词典）。
	 */
	public void insertAll(Iterable<String> words) {
		if (words == null) {
			return;
		}
		for (String word : words) {
			if (word != null && !word.isEmpty()) {
				insert(word);
			}
		}
	}

	/**
	 * 是否存在完整词条（须到达词尾标记）。
	 */
	public boolean search(String word) {
		Node node = findNode(word);
		return node != null && node.endOfWord;
	}

	/**
	 * 是否存在以该前缀开头的路径。
	 */
	public boolean startsWith(String prefix) {
		return findNode(prefix) != null;
	}

	/**
	 * 词条插入次数；未作为完整词插入过则返回 0。
	 */
	public int getFreq(String word) {
		Node node = findNode(word);
		if (node == null || !node.endOfWord) {
			return 0;
		}
		return node.freq;
	}

	/**
	 * 在文档中扫描本 Trie 已插入的关键词，统计出现次数（可命中嵌套词）。
	 * @param document 待扫描文本
	 * @return 关键词 -&gt; 次数；无命中返回空 Map
	 */
	public Map<String, Integer> matchInText(String document) {
		if (document == null || document.isEmpty() || root.children.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<String, Integer> result = new HashMap<>();
		Node cur = root;
		int start = 0;
		int end = 0;
		int len = document.length();
		while (start < len) {
			if (end < len && cur.children.containsKey(document.charAt(end))) {
				cur = cur.children.get(document.charAt(end));
				end++;
				if (cur.endOfWord) {
					result.merge(document.substring(start, end), 1, Integer::sum);
				}
			}
			else {
				end = ++start;
				cur = root;
			}
		}
		return result;
	}

	/**
	 * 英文文本简单分词：小写、去常见标点后按空白切分（不依赖词典）。
	 */
	public static String[] tokenizeWords(String text) {
		if (text == null || text.isEmpty()) {
			return new String[0];
		}
		String normalized = text.toLowerCase().replaceAll("[,?!.:;\"']", " ").trim();
		if (normalized.isEmpty()) {
			return new String[0];
		}
		return normalized.split("\\s+");
	}

	/**
	 * 中文分词：基于本 Trie 词典的正向最大匹配（Forward Maximum Matching）。
	 * <p>
	 * 使用前需 {@link #insert}/{@link #insertAll} 加载词库。从左到右取能匹配到的最长词；
	 * 若当前位置无法成词，则按单字切出；空白与标点跳过不输出。
	 * </p>
	 * @param text 待分词文本
	 * @return 词序列；text 为空时返回空列表
	 */
	public List<String> segmentChinese(String text) {
		if (text == null || text.isEmpty()) {
			return Collections.emptyList();
		}
		List<String> tokens = new ArrayList<>();
		int i = 0;
		int len = text.length();
		while (i < len) {
			char ch = text.charAt(i);
			if (isSkippable(ch)) {
				i++;
				continue;
			}
			int matchLen = longestMatchLength(text, i);
			if (matchLen > 0) {
				tokens.add(text.substring(i, i + matchLen));
				i += matchLen;
			}
			else {
				// 未登录词：单字切分，保证覆盖整句
				tokens.add(String.valueOf(ch));
				i++;
			}
		}
		return tokens;
	}

	/**
	 * 从 {@code start} 起，在词典中能匹配到的最长词长度；无匹配返回 0。
	 */
	private int longestMatchLength(String text, int start) {
		Node cur = root;
		int longest = 0;
		for (int i = start; i < text.length(); i++) {
			cur = cur.children.get(text.charAt(i));
			if (cur == null) {
				break;
			}
			if (cur.endOfWord) {
				longest = i - start + 1;
			}
		}
		return longest;
	}

	private static boolean isSkippable(char ch) {
		return Character.isWhitespace(ch) || isPunctuation(ch);
	}

	private static boolean isPunctuation(char ch) {
		return ",.?!:;\"'，。？！：；、（）()【】[]《》<>—…·".indexOf(ch) >= 0;
	}

	private Node findNode(String word) {
		String value = requireNonBlank(word);
		Node current = root;
		for (int i = 0; i < value.length(); i++) {
			current = current.children.get(value.charAt(i));
			if (current == null) {
				return null;
			}
		}
		return current;
	}

	private static String requireNonBlank(String word) {
		if (word == null || word.isEmpty()) {
			throw new IllegalArgumentException("word must not be null or empty");
		}
		return word;
	}

	private static final class Node {

		private final Map<Character, Node> children = new HashMap<>();

		private boolean endOfWord;

		private int freq;

	}

}

package com.lind.algorithm.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * {@link Trie} 单元测试。
 */
public class TrieTest {

	@Test
	void insertSearchAndPrefix() {
		Trie trie = new Trie();
		trie.insert("apple");
		trie.insert("app");
		trie.insert("banana");
		trie.insert("中国");

		assertTrue(trie.search("apple"));
		assertTrue(trie.search("app"));
		assertTrue(trie.search("中国"));
		assertFalse(trie.search("appl"));
		assertTrue(trie.startsWith("app"));
		assertTrue(trie.startsWith("中"));
		assertFalse(trie.startsWith("pear"));
	}

	@Test
	void freqIncrementsOnInsertOnly() {
		Trie trie = new Trie();
		trie.insert("hello");
		trie.insert("hello");
		assertEquals(2, trie.getFreq("hello"));
		assertTrue(trie.search("hello"));
		assertEquals(2, trie.getFreq("hello"));
		assertEquals(0, trie.getFreq("hell"));
	}

	@Test
	void matchInTextCountsNestedKeywords() {
		Trie trie = new Trie();
		trie.insert("美国");
		trie.insert("美海军");
		trie.insert("美国海军");
		trie.insert("战斗机");

		Map<String, Integer> result = trie.matchInText("报道称，美国海军正面临严重的战机荒。美国总统表现出色");

		assertEquals(2, result.get("美国"));
		assertEquals(1, result.get("美国海军"));
		assertFalse(result.containsKey("战斗机"));
		assertFalse(result.containsKey("美海军"));
	}

	@Test
	void tokenizeWords() {
		String[] words = Trie.tokenizeWords("Hello, World! Nice day.");
		assertEquals(4, words.length);
		assertEquals("hello", words[0]);
		assertEquals("world", words[1]);
	}

	@Test
	void segmentChineseWithDictionary() {
		Trie trie = new Trie();
		trie.insertAll(Arrays.asList("中国", "人民", "银行", "中国人民银行"));

		List<String> tokens = trie.segmentChinese("中国人民银行成立了");
		assertEquals(Arrays.asList("中国人民银行", "成", "立", "了"), tokens);

		List<String> nested = trie.segmentChinese("中国人民很幸福");
		assertEquals(Arrays.asList("中国", "人民", "很", "幸", "福"), nested);
	}

	@Test
	void segmentChineseSkipsPunctuation() {
		Trie trie = new Trie();
		trie.insert("美国");
		trie.insert("海军");

		List<String> tokens = trie.segmentChinese("美国，海军！");
		assertEquals(Arrays.asList("美国", "海军"), tokens);
	}

	@Test
	void rejectBlankWord() {
		Trie trie = new Trie();
		assertThrows(IllegalArgumentException.class, () -> trie.insert(""));
		assertThrows(IllegalArgumentException.class, () -> trie.search(null));
	}

}

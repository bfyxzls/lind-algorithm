package com.lind.algorithm.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * {@link BPlusTree} 单元测试。
 */
public class BPlusTreeTest {

	@Test
	void putGetAndOverwrite() {
		BPlusTree<Integer, String> tree = new BPlusTree<>(4);
		tree.put(10, "a");
		tree.put(20, "b");
		tree.put(5, "c");

		assertEquals(3, tree.size());
		assertEquals("a", tree.get(10).orElseThrow());
		assertEquals("b", tree.get(20).orElseThrow());
		assertTrue(tree.containsKey(5));
		assertFalse(tree.containsKey(99));

		tree.put(10, "A");
		assertEquals(3, tree.size());
		assertEquals("A", tree.get(10).orElseThrow());
	}

	@Test
	void splitAndRangeQuery() {
		BPlusTree<Integer, String> tree = new BPlusTree<>(3);
		for (int i = 1; i <= 20; i++) {
			tree.put(i, "v" + i);
		}
		assertEquals(20, tree.size());
		assertEquals(IntStream.rangeClosed(1, 20).boxed().collect(Collectors.toList()), tree.keys());

		List<BPlusTree.Entry<Integer, String>> range = tree.range(5, 8);
		assertEquals(List.of(5, 6, 7, 8), range.stream().map(BPlusTree.Entry::key).collect(Collectors.toList()));
		assertEquals("v6", tree.get(6).orElseThrow());
	}

	@Test
	void rejectNullKeyOrValue() {
		BPlusTree<Integer, String> tree = new BPlusTree<>();
		assertThrows(NullPointerException.class, () -> tree.put(null, "x"));
		assertThrows(NullPointerException.class, () -> tree.put(1, null));
	}

}

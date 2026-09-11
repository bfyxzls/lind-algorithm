package com.lind.algorithm.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * {@link RedBlackTree} 单元测试。
 */
public class RedBlackTreeTest {

	@Test
	void insertAndContains() {
		RedBlackTree<Integer> tree = new RedBlackTree<>();
		assertTrue(tree.isEmpty());

		tree.insert(10);
		tree.insert(20);
		tree.insert(5);
		tree.insert(15);

		assertEquals(4, tree.size());
		assertTrue(tree.contains(10));
		assertTrue(tree.contains(20));
		assertTrue(tree.contains(5));
		assertTrue(tree.contains(15));
		assertFalse(tree.contains(99));
	}

	@Test
	void rejectNull() {
		RedBlackTree<Integer> tree = new RedBlackTree<>();
		assertThrows(IllegalArgumentException.class, () -> tree.insert(null));
	}

}

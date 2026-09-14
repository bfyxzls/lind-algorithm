package com.lind.algorithm.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@link RTree} 单元测试。
 */
public class RTreeTest {

	@Test
	void insertAndIntersectSearch() {
		RTree<String> tree = new RTree<>(3);
		tree.insert(new RTree.Rectangle(0, 0, 1, 1), "a");
		tree.insert(new RTree.Rectangle(5, 5, 6, 6), "b");
		tree.insert(RTree.Rectangle.ofPoint(0.5, 0.5), "c");
		tree.insert(new RTree.Rectangle(10, 10, 12, 12), "d");

		assertEquals(4, tree.size());

		List<String> hit = tree.search(new RTree.Rectangle(0, 0, 2, 2));
		assertEquals(Set.of("a", "c"), new HashSet<>(hit));

		List<String> far = tree.search(new RTree.Rectangle(100, 100, 101, 101));
		assertTrue(far.isEmpty());
	}

	@Test
	void splitStillQueryable() {
		RTree<Integer> tree = new RTree<>(2);
		for (int i = 0; i < 12; i++) {
			double x = i * 10.0;
			tree.insert(new RTree.Rectangle(x, x, x + 1, x + 1), i);
		}
		assertEquals(12, tree.size());
		List<Integer> mid = tree.search(new RTree.Rectangle(25, 25, 45, 45));
		assertTrue(mid.contains(3));
		assertTrue(mid.contains(4));
	}

}

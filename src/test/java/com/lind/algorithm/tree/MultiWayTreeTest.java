package com.lind.algorithm.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@link MultiWayTree} 组织架构多叉树单元测试。
 */
public class MultiWayTreeTest {

	@Test
	void fromFlatBuildsOrgChart() {
		MultiWayTree<String> org = MultiWayTree.fromFlat(List.of(
				MultiWayTree.FlatNode.root("ceo", "CEO"),
				MultiWayTree.FlatNode.of("eng", "ceo", "Engineering"),
				MultiWayTree.FlatNode.of("hr", "ceo", "HR"),
				MultiWayTree.FlatNode.of("be", "eng", "Backend"),
				MultiWayTree.FlatNode.of("fe", "eng", "Frontend")));

		assertEquals(5, org.size());
		assertEquals(3, org.height());
		assertEquals("Engineering", org.findById("eng").orElseThrow().payload());
		assertEquals(2, org.findById("eng").orElseThrow().children().size());

		List<String> path = org.pathTo("be").orElseThrow().stream().map(MultiWayTree.MultiWayNode::id).toList();
		assertEquals(List.of("ceo", "eng", "be"), path);

		assertEquals(List.of("CEO", "Engineering", "Backend", "Frontend", "HR"), org.dfsPayloads());
	}

	@Test
	void bfsOrderIsLevelOrder() {
		MultiWayTree<String> org = MultiWayTree.fromFlat(List.of(
				MultiWayTree.FlatNode.root("1", "A"),
				MultiWayTree.FlatNode.of("2", "1", "B"),
				MultiWayTree.FlatNode.of("3", "1", "C")));
		List<String> ids = new java.util.ArrayList<>();
		org.bfs(n -> ids.add(n.id()));
		assertEquals(List.of("1", "2", "3"), ids);
	}

	@Test
	void rejectDuplicateOrMissingParent() {
		assertThrows(IllegalArgumentException.class, () -> MultiWayTree.fromFlat(List.of(
				MultiWayTree.FlatNode.root("1", "a"),
				MultiWayTree.FlatNode.of("1", "1", "dup"))));
		assertThrows(IllegalArgumentException.class, () -> MultiWayTree.fromFlat(List.of(
				MultiWayTree.FlatNode.root("1", "a"),
				MultiWayTree.FlatNode.of("2", "missing", "b"))));
		assertTrue(MultiWayTree.fromFlat(List.of(MultiWayTree.FlatNode.root("only", "x"))).root().isLeaf());
	}

}

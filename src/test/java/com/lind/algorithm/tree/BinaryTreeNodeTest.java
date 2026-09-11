package com.lind.algorithm.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@link BinaryTreeNode} 单元测试。
 */
public class BinaryTreeNodeTest {

	@Test
	void traversals() {
		BinaryTreeNode root = new BinaryTreeNode(1);
		root.setLeft(new BinaryTreeNode(2));
		root.setRight(new BinaryTreeNode(3));

		assertEquals(Arrays.asList(1, 2, 3), BinaryTreeNode.preOrder(root));
		assertEquals(Arrays.asList(2, 1, 3), BinaryTreeNode.inOrder(root));
		assertEquals(Arrays.asList(2, 3, 1), BinaryTreeNode.postOrder(root));
	}

	@Test
	void emptyRootReturnsEmptyList() {
		List<Integer> empty = BinaryTreeNode.preOrder(null);
		assertEquals(0, empty.size());
	}

}

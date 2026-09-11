package com.lind.algorithm.unionfind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * {@link UnionFind} 单元测试。
 */
public class UnionFindTest {

	@Test
	void unionAndConnected() {
		UnionFind<Integer> uf = new UnionFind<>();
		uf.makeSet(1);
		uf.makeSet(2);
		uf.makeSet(3);
		assertEquals(3, uf.setCount());
		assertTrue(uf.union(1, 2));
		assertTrue(uf.connected(1, 2));
		assertFalse(uf.connected(1, 3));
		assertFalse(uf.union(1, 2));
		uf.union(2, 3);
		assertTrue(uf.connected(1, 3));
		assertEquals(1, uf.setCount());
	}

}

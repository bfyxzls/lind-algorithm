package com.lind.algorithm.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@link DirectedGraph} 单元测试。
 */
public class DirectedGraphTest {

	@Test
	void dijkstraShortestPath() {
		DirectedGraph<String> g = new DirectedGraph<>();
		g.addEdge("A", "B", 1);
		g.addEdge("B", "C", 2);
		g.addEdge("A", "C", 10);
		assertEquals(3.0, g.shortestPath("A", "C").orElseThrow());
	}

	@Test
	void topologicalSortAndCycle() {
		DirectedGraph<String> g = new DirectedGraph<>();
		g.addEdge("build", "test");
		g.addEdge("test", "deploy");
		assertEquals(List.of("build", "test", "deploy"), g.topologicalSort().orElseThrow());
		assertFalse(g.hasCycle());

		g.addEdge("deploy", "build");
		assertTrue(g.hasCycle());
		assertTrue(g.topologicalSort().isEmpty());
	}

}

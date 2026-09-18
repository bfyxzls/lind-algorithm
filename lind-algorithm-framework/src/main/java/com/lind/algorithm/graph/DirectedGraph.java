package com.lind.algorithm.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

/**
 * 有向加权图：支持 Dijkstra 最短路径、拓扑排序与环检测。
 *
 * @param <V> 顶点类型
 */
public final class DirectedGraph<V> {

	private final Map<V, List<Edge<V>>> adjacency = new LinkedHashMap<>();

	public void addVertex(V vertex) {
		Objects.requireNonNull(vertex, "vertex");
		adjacency.computeIfAbsent(vertex, v -> new ArrayList<>());
	}

	public void addEdge(V from, V to, double weight) {
		Objects.requireNonNull(from, "from");
		Objects.requireNonNull(to, "to");
		if (weight < 0) {
			throw new IllegalArgumentException("Dijkstra requires non-negative weight");
		}
		addVertex(from);
		addVertex(to);
		adjacency.get(from).add(new Edge<>(to, weight));
	}

	public void addEdge(V from, V to) {
		addEdge(from, to, 1.0);
	}

	public Set<V> vertices() {
		return Collections.unmodifiableSet(adjacency.keySet());
	}

	public List<Edge<V>> edgesFrom(V vertex) {
		return Collections.unmodifiableList(adjacency.getOrDefault(vertex, List.of()));
	}

	/**
	 * Dijkstra：返回从 source 到各顶点的最短距离；不可达顶点不在结果中。
	 */
	public Map<V, Double> shortestPaths(V source) {
		Objects.requireNonNull(source, "source");
		if (!adjacency.containsKey(source)) {
			throw new IllegalArgumentException("unknown source: " + source);
		}
		Map<V, Double> dist = new HashMap<>();
		PriorityQueue<Map.Entry<V, Double>> pq = new PriorityQueue<>(Map.Entry.comparingByValue());
		dist.put(source, 0.0);
		pq.offer(Map.entry(source, 0.0));
		while (!pq.isEmpty()) {
			Map.Entry<V, Double> current = pq.poll();
			V u = current.getKey();
			double d = current.getValue();
			if (d > dist.getOrDefault(u, Double.POSITIVE_INFINITY)) {
				continue;
			}
			for (Edge<V> edge : adjacency.getOrDefault(u, List.of())) {
				double nd = d + edge.weight();
				if (nd < dist.getOrDefault(edge.to(), Double.POSITIVE_INFINITY)) {
					dist.put(edge.to(), nd);
					pq.offer(Map.entry(edge.to(), nd));
				}
			}
		}
		return Collections.unmodifiableMap(dist);
	}

	public Optional<Double> shortestPath(V source, V target) {
		return Optional.ofNullable(shortestPaths(source).get(target));
	}

	/**
	 * Kahn 拓扑排序；存在环时 empty。
	 */
	public Optional<List<V>> topologicalSort() {
		Map<V, Integer> indegree = new HashMap<>();
		for (V v : adjacency.keySet()) {
			indegree.putIfAbsent(v, 0);
			for (Edge<V> e : adjacency.get(v)) {
				indegree.merge(e.to(), 1, Integer::sum);
				indegree.putIfAbsent(e.to(), indegree.get(e.to()));
			}
		}
		Queue<V> queue = new LinkedList<>();
		for (Map.Entry<V, Integer> e : indegree.entrySet()) {
			if (e.getValue() == 0) {
				queue.add(e.getKey());
			}
		}
		List<V> order = new ArrayList<>();
		while (!queue.isEmpty()) {
			V u = queue.remove();
			order.add(u);
			for (Edge<V> edge : adjacency.getOrDefault(u, List.of())) {
				int d = indegree.merge(edge.to(), -1, Integer::sum);
				if (d == 0) {
					queue.add(edge.to());
				}
			}
		}
		if (order.size() != indegree.size()) {
			return Optional.empty();
		}
		return Optional.of(Collections.unmodifiableList(order));
	}

	public boolean hasCycle() {
		return topologicalSort().isEmpty();
	}

	public record Edge<V> (V to, double weight) {
	}

}

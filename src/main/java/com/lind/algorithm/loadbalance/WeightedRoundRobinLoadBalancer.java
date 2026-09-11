package com.lind.algorithm.loadbalance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 平滑加权轮询（Nginx Smooth Weighted Round-Robin）。
 * <p>
 * 相对简单「按权重重复节点再轮询」，该算法分布更均匀，且无短时突发偏斜。
 * </p>
 */
public final class WeightedRoundRobinLoadBalancer {

	private final ConcurrentMap<String, State> states = new ConcurrentHashMap<>();

	/**
	 * @param key 路由键（按 key 维护独立权重状态）
	 * @param nodes 带权节点，不可为空
	 */
	public String select(String key, Collection<WeightedNode> nodes) {
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(nodes, "nodes");
		if (nodes.isEmpty()) {
			throw new IllegalArgumentException("nodes must not be empty");
		}
		List<WeightedNode> list = new ArrayList<>(nodes.size());
		for (WeightedNode n : nodes) {
			Objects.requireNonNull(n, "node");
			list.add(n);
		}

		State state = states.computeIfAbsent(key, k -> new State());
		synchronized (state) {
			state.ensureSize(list.size());
			int total = 0;
			int best = -1;
			for (int i = 0; i < list.size(); i++) {
				int w = list.get(i).weight();
				state.current[i] += w;
				total += w;
				if (best == -1 || state.current[i] > state.current[best]) {
					best = i;
				}
			}
			state.current[best] -= total;
			return list.get(best).address();
		}
	}

	private static final class State {

		private int[] current = new int[0];

		void ensureSize(int size) {
			if (current.length != size) {
				current = new int[size];
			}
		}

	}

}

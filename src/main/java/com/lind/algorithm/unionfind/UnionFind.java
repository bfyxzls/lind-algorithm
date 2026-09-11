package com.lind.algorithm.unionfind;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 并查集（带路径压缩与按秩合并）：维护不相交集合的连通性。
 *
 * @param <T> 元素类型
 */
public final class UnionFind<T> {

	private final Map<T, T> parent = new HashMap<>();

	private final Map<T, Integer> rank = new HashMap<>();

	private int setCount;

	public void makeSet(T item) {
		Objects.requireNonNull(item, "item");
		if (parent.containsKey(item)) {
			return;
		}
		parent.put(item, item);
		rank.put(item, 0);
		setCount++;
	}

	public T find(T item) {
		Objects.requireNonNull(item, "item");
		if (!parent.containsKey(item)) {
			makeSet(item);
		}
		T p = parent.get(item);
		if (!p.equals(item)) {
			parent.put(item, find(p));
		}
		return parent.get(item);
	}

	public boolean union(T a, T b) {
		T rootA = find(a);
		T rootB = find(b);
		if (rootA.equals(rootB)) {
			return false;
		}
		int rankA = rank.get(rootA);
		int rankB = rank.get(rootB);
		if (rankA < rankB) {
			parent.put(rootA, rootB);
		}
		else if (rankA > rankB) {
			parent.put(rootB, rootA);
		}
		else {
			parent.put(rootB, rootA);
			rank.put(rootA, rankA + 1);
		}
		setCount--;
		return true;
	}

	public boolean connected(T a, T b) {
		return find(a).equals(find(b));
	}

	public int setCount() {
		return setCount;
	}

}

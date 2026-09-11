package com.lind.algorithm.topk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;

/**
 * Top-K 计数器：统计元素频次并返回出现次数最高的 K 个（小顶堆）。
 *
 * @param <T> 元素类型
 */
public final class TopKCounter<T> {

	private final int k;

	private final Map<T, Long> counts = new HashMap<>();

	public TopKCounter(int k) {
		if (k <= 0) {
			throw new IllegalArgumentException("k must be > 0");
		}
		this.k = k;
	}

	public void offer(T item) {
		Objects.requireNonNull(item, "item");
		counts.merge(item, 1L, Long::sum);
	}

	public void offer(T item, long times) {
		Objects.requireNonNull(item, "item");
		if (times <= 0) {
			throw new IllegalArgumentException("times must be > 0");
		}
		counts.merge(item, times, Long::sum);
	}

	public long countOf(T item) {
		return counts.getOrDefault(item, 0L);
	}

	public List<Entry<T>> topK() {
		PriorityQueue<Entry<T>> heap = new PriorityQueue<>(Comparator.comparingLong(Entry::count));
		for (Map.Entry<T, Long> e : counts.entrySet()) {
			heap.offer(new Entry<>(e.getKey(), e.getValue()));
			if (heap.size() > k) {
				heap.poll();
			}
		}
		List<Entry<T>> result = new ArrayList<>(heap);
		result.sort(Comparator.comparingLong(Entry<T>::count).reversed());
		return Collections.unmodifiableList(result);
	}

	public int k() {
		return k;
	}

	public record Entry<T>(T item, long count) {
	}

}

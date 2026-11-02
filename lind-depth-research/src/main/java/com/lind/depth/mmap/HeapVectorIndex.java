package com.lind.depth.mmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;

/**
 * <b>玩具做法</b>：全部向量以 {@code float[]} 驻留 JVM 堆，查询时暴力扫全库。
 *
 * <p>
 * 对应文档「全量 List&lt;float[]&gt; 进堆」——向量一多，堆直接爆，GC 成为瓶颈。 本类只适合小数据对照实验，用来对比 mmap / 量化路径。
 */
public final class HeapVectorIndex {

	private final List<float[]> vectors = new ArrayList<>();

	private final int dim;

	public HeapVectorIndex(int dim) {
		if (dim <= 0) {
			throw new IllegalArgumentException("dim 必须 > 0");
		}
		this.dim = dim;
	}

	public int dim() {
		return dim;
	}

	public int size() {
		return vectors.size();
	}

	/** 拷贝后存入堆内列表。 */
	public int add(float[] vector) {
		Objects.requireNonNull(vector, "vector");
		if (vector.length != dim) {
			throw new IllegalArgumentException("维度不一致");
		}
		int id = vectors.size();
		vectors.add(Vectors.copy(vector));
		return id;
	}

	/**
	 * 暴力 Top-K：对每一条算 L2 平方距离。
	 */
	public List<SearchHit> search(float[] query, int topK) {
		Objects.requireNonNull(query, "query");
		if (query.length != dim) {
			throw new IllegalArgumentException("维度不一致");
		}
		if (topK <= 0) {
			throw new IllegalArgumentException("topK 必须 > 0");
		}
		PriorityQueue<SearchHit> heap = new PriorityQueue<>(Collections.reverseOrder());
		for (int i = 0; i < vectors.size(); i++) {
			float dist = Vectors.l2Squared(query, vectors.get(i));
			SearchHit hit = new SearchHit(i, dist);
			if (heap.size() < topK) {
				heap.offer(hit);
			}
			else if (hit.distance() < heap.peek().distance()) {
				heap.poll();
				heap.offer(hit);
			}
		}
		List<SearchHit> out = new ArrayList<>(heap);
		Collections.sort(out);
		return out;
	}

	/** 粗略估计堆内占用：条数 × 维数 × 4 字节（不含对象头与 List 开销）。 */
	public long estimatedHeapBytes() {
		return (long) vectors.size() * dim * Float.BYTES;
	}

}

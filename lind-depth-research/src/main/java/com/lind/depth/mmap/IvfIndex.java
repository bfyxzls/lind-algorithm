package com.lind.depth.mmap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * <b>IVF（倒排文件）粗分桶</b>教学实现。
 *
 * <p>
 * 对应文档：先聚类把向量分到 {@code nlist} 个桶；查询时只扫最近 {@code nprobe} 个桶，避免全库比对。 桶内具体怎么算距离，由调用方决定（可用精确
 * float，也可用 PQ 近似）。
 */
public final class IvfIndex {

	private final float[][] centroids;

	private final List<List<Integer>> invertedLists;

	private IvfIndex(float[][] centroids, List<List<Integer>> invertedLists) {
		this.centroids = centroids;
		this.invertedLists = invertedLists;
	}

	public int nlist() {
		return centroids.length;
	}

	public List<Integer> list(int bucket) {
		return Collections.unmodifiableList(invertedLists.get(bucket));
	}

	/**
	 * 用训练集学粗中心，并把 {@code allVectors} 按最近中心挂进倒排列表。
	 */
	public static IvfIndex trainAndAssign(float[][] allVectors, int nlist, int iters, long seed) {
		Objects.requireNonNull(allVectors, "allVectors");
		if (allVectors.length < nlist) {
			throw new IllegalArgumentException("样本数不能小于 nlist");
		}
		int dim = allVectors[0].length;
		Random rnd = new Random(seed);
		float[][] centroids = new float[nlist][];
		boolean[] used = new boolean[allVectors.length];
		for (int i = 0; i < nlist; i++) {
			int idx;
			do {
				idx = rnd.nextInt(allVectors.length);
			}
			while (used[idx]);
			used[idx] = true;
			centroids[i] = Vectors.copy(allVectors[idx]);
		}
		int[] assign = new int[allVectors.length];
		for (int it = 0; it < iters; it++) {
			for (int i = 0; i < allVectors.length; i++) {
				assign[i] = nearestCentroid(centroids, allVectors[i]);
			}
			float[][] sum = new float[nlist][dim];
			int[] cnt = new int[nlist];
			for (int i = 0; i < allVectors.length; i++) {
				int a = assign[i];
				cnt[a]++;
				for (int d = 0; d < dim; d++) {
					sum[a][d] += allVectors[i][d];
				}
			}
			for (int c = 0; c < nlist; c++) {
				if (cnt[c] == 0) {
					centroids[c] = Vectors.copy(allVectors[rnd.nextInt(allVectors.length)]);
					continue;
				}
				for (int d = 0; d < dim; d++) {
					centroids[c][d] = sum[c][d] / cnt[c];
				}
			}
		}
		List<List<Integer>> lists = new ArrayList<>(nlist);
		for (int i = 0; i < nlist; i++) {
			lists.add(new ArrayList<>());
		}
		for (int i = 0; i < allVectors.length; i++) {
			int a = nearestCentroid(centroids, allVectors[i]);
			lists.get(a).add(i);
		}
		return new IvfIndex(centroids, lists);
	}

	/** 返回与查询最近的 {@code nprobe} 个桶号（近 → 远）。 */
	public int[] probe(float[] query, int nprobe) {
		Objects.requireNonNull(query, "query");
		int n = Math.min(nprobe, centroids.length);
		PriorityQueue<SearchHit> heap = new PriorityQueue<>(Collections.reverseOrder());
		for (int i = 0; i < centroids.length; i++) {
			float dist = Vectors.l2Squared(query, centroids[i]);
			SearchHit hit = new SearchHit(i, dist);
			if (heap.size() < n) {
				heap.offer(hit);
			}
			else if (hit.distance() < heap.peek().distance()) {
				heap.poll();
				heap.offer(hit);
			}
		}
		SearchHit[] arr = heap.toArray(SearchHit[]::new);
		Arrays.sort(arr);
		int[] ids = new int[arr.length];
		for (int i = 0; i < arr.length; i++) {
			ids[i] = arr[i].id();
		}
		return ids;
	}

	private static int nearestCentroid(float[][] centroids, float[] v) {
		int best = 0;
		float bestDist = Float.POSITIVE_INFINITY;
		for (int i = 0; i < centroids.length; i++) {
			float d = Vectors.l2Squared(v, centroids[i]);
			if (d < bestDist) {
				bestDist = d;
				best = i;
			}
		}
		return best;
	}

}

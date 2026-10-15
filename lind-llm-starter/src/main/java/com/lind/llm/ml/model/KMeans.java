package com.lind.llm.ml.model;

import com.lind.llm.ml.math.Vectors;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

/**
 * K-Means 聚类（无监督）：迭代分配簇 + 更新中心。
 */
public final class KMeans {

	private final int k;

	private final int maxIters;

	private final long seed;

	private double[][] centroids;

	public KMeans(int k, int maxIters, long seed) {
		if (k <= 0) {
			throw new IllegalArgumentException("k 必须 > 0");
		}
		if (maxIters <= 0) {
			throw new IllegalArgumentException("maxIters 必须 > 0");
		}
		this.k = k;
		this.maxIters = maxIters;
		this.seed = seed;
	}

	public void fit(double[][] points) {
		Objects.requireNonNull(points, "points");
		if (points.length < k) {
			throw new IllegalArgumentException("样本数不能小于 k");
		}
		int dim = points[0].length;
		for (double[] p : points) {
			if (p.length != dim) {
				throw new IllegalArgumentException("点的维度不一致");
			}
		}
		Random rnd = new Random(seed);
		centroids = new double[k][];
		boolean[] used = new boolean[points.length];
		for (int i = 0; i < k; i++) {
			int idx;
			do {
				idx = rnd.nextInt(points.length);
			}
			while (used[idx]);
			used[idx] = true;
			centroids[i] = Vectors.copy(points[idx]);
		}

		int[] assign = new int[points.length];
		for (int iter = 0; iter < maxIters; iter++) {
			boolean changed = false;
			for (int i = 0; i < points.length; i++) {
				int nearest = nearestCentroid(points[i]);
				if (assign[i] != nearest) {
					assign[i] = nearest;
					changed = true;
				}
			}
			double[][] sums = new double[k][dim];
			int[] counts = new int[k];
			for (int i = 0; i < points.length; i++) {
				int c = assign[i];
				counts[c]++;
				for (int d = 0; d < dim; d++) {
					sums[c][d] += points[i][d];
				}
			}
			for (int c = 0; c < k; c++) {
				if (counts[c] == 0) {
					centroids[c] = Vectors.copy(points[rnd.nextInt(points.length)]);
					continue;
				}
				for (int d = 0; d < dim; d++) {
					centroids[c][d] = sums[c][d] / counts[c];
				}
			}
			if (!changed && iter > 0) {
				break;
			}
		}
	}

	public int predict(double[] point) {
		if (centroids == null) {
			throw new IllegalStateException("请先调用 fit");
		}
		return nearestCentroid(point);
	}

	public double[][] centroids() {
		if (centroids == null) {
			throw new IllegalStateException("请先调用 fit");
		}
		double[][] copy = new double[centroids.length][];
		for (int i = 0; i < centroids.length; i++) {
			copy[i] = Arrays.copyOf(centroids[i], centroids[i].length);
		}
		return copy;
	}

	private int nearestCentroid(double[] point) {
		int best = 0;
		double bestDist = Double.POSITIVE_INFINITY;
		for (int i = 0; i < centroids.length; i++) {
			double dist = squaredDistance(point, centroids[i]);
			if (dist < bestDist) {
				bestDist = dist;
				best = i;
			}
		}
		return best;
	}

	private static double squaredDistance(double[] a, double[] b) {
		Vectors.requireSameLength(a, b);
		double s = 0;
		for (int i = 0; i < a.length; i++) {
			double d = a[i] - b[i];
			s += d * d;
		}
		return s;
	}

}

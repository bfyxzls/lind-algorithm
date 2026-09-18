package com.lind.algorithm.topk;

import java.util.Objects;
import java.util.Random;

/**
 * Count-Min Sketch：概率型频次估计，用较少空间近似计数，适合海量流式 Top/热点粗估。
 */
public final class CountMinSketch {

	private final int depth;

	private final int width;

	private final long[][] table;

	private final int[] coefficients;

	public CountMinSketch(int depth, int width) {
		if (depth <= 0 || width <= 0) {
			throw new IllegalArgumentException("depth/width must be > 0");
		}
		this.depth = depth;
		this.width = width;
		this.table = new long[depth][width];
		this.coefficients = new int[depth * 2];
		Random random = new Random(0xC0FFEE);
		for (int i = 0; i < coefficients.length; i++) {
			coefficients[i] = random.nextInt(Integer.MAX_VALUE - 1) + 1;
		}
	}

	public void add(String key) {
		add(key, 1);
	}

	public void add(String key, long count) {
		Objects.requireNonNull(key, "key");
		if (count <= 0) {
			throw new IllegalArgumentException("count must be > 0");
		}
		for (int i = 0; i < depth; i++) {
			table[i][index(key, i)] += count;
		}
	}

	public long estimate(String key) {
		Objects.requireNonNull(key, "key");
		long min = Long.MAX_VALUE;
		for (int i = 0; i < depth; i++) {
			min = Math.min(min, table[i][index(key, i)]);
		}
		return min;
	}

	private int index(String key, int row) {
		int a = coefficients[row * 2];
		int b = coefficients[row * 2 + 1];
		int hash = key.hashCode();
		long mixed = (long) a * hash + b;
		return Math.floorMod(mixed, width);
	}

}

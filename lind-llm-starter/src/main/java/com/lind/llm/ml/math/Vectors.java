package com.lind.llm.ml.math;

import java.util.Arrays;
import java.util.Objects;

/**
 * 轻量向量运算（教学用，非高性能 BLAS）。
 */
public final class Vectors {

	private Vectors() {
	}

	public static double dot(double[] a, double[] b) {
		requireSameLength(a, b);
		double s = 0;
		for (int i = 0; i < a.length; i++) {
			s += a[i] * b[i];
		}
		return s;
	}

	public static double[] add(double[] a, double[] b) {
		requireSameLength(a, b);
		double[] out = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			out[i] = a[i] + b[i];
		}
		return out;
	}

	public static double[] scale(double[] a, double factor) {
		Objects.requireNonNull(a, "a");
		double[] out = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			out[i] = a[i] * factor;
		}
		return out;
	}

	public static double l2(double[] a) {
		Objects.requireNonNull(a, "a");
		double s = 0;
		for (double v : a) {
			s += v * v;
		}
		return Math.sqrt(s);
	}

	public static double[] copy(double[] a) {
		return Arrays.copyOf(Objects.requireNonNull(a, "a"), a.length);
	}

	public static void requireSameLength(double[] a, double[] b) {
		Objects.requireNonNull(a, "a");
		Objects.requireNonNull(b, "b");
		if (a.length != b.length) {
			throw new IllegalArgumentException("向量维度不一致: " + a.length + " vs " + b.length);
		}
	}

}

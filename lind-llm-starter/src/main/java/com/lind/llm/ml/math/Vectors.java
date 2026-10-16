package com.lind.llm.ml.math;

import java.util.Arrays;
import java.util.Objects;

/**
 * 轻量向量运算（教学用，非高性能 BLAS）。
 */
public final class Vectors {

	private Vectors() {
	}

	/**
	 * 计算两个向量的点积（内积）。
	 *
	 * @param a 第一个向量，非空
	 * @param b 第二个向量，非空且长度与 a 相同
	 * @return 点积值（标量）
	 */
	public static double dot(double[] a, double[] b) {
		requireSameLength(a, b);
		double s = 0;
		for (int i = 0; i < a.length; i++) {
			s += a[i] * b[i];
		}
		return s;
	}

	/**
	 * 计算两个向量的和（逐元素相加）。
	 *
	 * @param a 第一个向量，非空
	 * @param b 第二个向量，非空且长度与 a 相同
	 * @return 新的向量，长度与输入相同；不会修改输入数组
	 */
	public static double[] add(double[] a, double[] b) {
		requireSameLength(a, b);
		double[] out = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			out[i] = a[i] + b[i];
		}
		return out;
	}
	
	/**
	 * 对向量按给定因子逐元素缩放。
	 *
	 * @param a 向量，非空
	 * @param factor 缩放因子
	 * @return 缩放后的新向量，不修改输入数组
	 */
	public static double[] scale(double[] a, double factor) {
		Objects.requireNonNull(a, "a");
		double[] out = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			out[i] = a[i] * factor;
		}
		return out;
	}

	/**
	 * 计算向量的 L2（Euclidean）范数，即 sqrt(sum_i a[i]^2)。
	 *
	 * @param a 输入向量，非空
	 * @return 向量的 L2 范数（非负实数）
	 */
	public static double l2(double[] a) {
		Objects.requireNonNull(a, "a");
		double s = 0;
		for (double v : a) {
			s += v * v;
		}
		return Math.sqrt(s);
	}

	/**
	 * 返回向量的浅拷贝（数组复制）。
	 *
	 * @param a 要复制的向量，非空
	 * @return 包含相同元素的新数组
	 */
	public static double[] copy(double[] a) {
		return Arrays.copyOf(Objects.requireNonNull(a, "a"), a.length);
	}

	/**
	 * 验证两个向量长度是否相同并且非空；否则抛出异常。
	 *
	 * @param a 第一个向量，非空
	 * @param b 第二个向量，非空
	 * @throws IllegalArgumentException 如果两者长度不相等
	 */
	public static void requireSameLength(double[] a, double[] b) {
		Objects.requireNonNull(a, "a");
		Objects.requireNonNull(b, "b");
		if (a.length != b.length) {
			throw new IllegalArgumentException("向量维度不一致: " + a.length + " vs " + b.length);
		}
	}

}

package com.lind.llm.ml.metrics;

import java.util.Objects;

/**
 * 回归评估指标。
 */
public final class RegressionMetrics {

	private RegressionMetrics() {
	}

	public static double mse(double[] yTrue, double[] yPred) {
		requireAligned(yTrue, yPred);
		double s = 0;
		for (int i = 0; i < yTrue.length; i++) {
			double d = yPred[i] - yTrue[i];
			s += d * d;
		}
		return s / yTrue.length;
	}

	public static double mae(double[] yTrue, double[] yPred) {
		requireAligned(yTrue, yPred);
		double s = 0;
		for (int i = 0; i < yTrue.length; i++) {
			s += Math.abs(yPred[i] - yTrue[i]);
		}
		return s / yTrue.length;
	}

	/** 决定系数 R²，越接近 1 越好。 */
	public static double r2(double[] yTrue, double[] yPred) {
		requireAligned(yTrue, yPred);
		double mean = 0;
		for (double y : yTrue) {
			mean += y;
		}
		mean /= yTrue.length;
		double ssTot = 0;
		double ssRes = 0;
		for (int i = 0; i < yTrue.length; i++) {
			ssTot += (yTrue[i] - mean) * (yTrue[i] - mean);
			ssRes += (yTrue[i] - yPred[i]) * (yTrue[i] - yPred[i]);
		}
		if (ssTot < 1e-12) {
			return 1.0;
		}
		return 1.0 - ssRes / ssTot;
	}

	private static void requireAligned(double[] a, double[] b) {
		Objects.requireNonNull(a, "yTrue");
		Objects.requireNonNull(b, "yPred");
		if (a.length == 0 || a.length != b.length) {
			throw new IllegalArgumentException("yTrue / yPred 长度须相同且非空");
		}
	}

}

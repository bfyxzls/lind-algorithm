package com.lind.llm.ml.math;

import java.util.Objects;

/**
 * 常用激活函数。
 */
public final class Activations {

	private Activations() {
	}

	/** Sigmoid：把实数压到 (0,1)，逻辑回归输出概率。 */
	public static double sigmoid(double z) {
		if (z >= 0) {
			double e = Math.exp(-z);
			return 1.0 / (1.0 + e);
		}
		double e = Math.exp(z);
		return e / (1.0 + e);
	}

	/** Softmax：多分类概率分布（数值稳定版）。 */
	public static double[] softmax(double[] logits) {
		Objects.requireNonNull(logits, "logits");
		double max = Double.NEGATIVE_INFINITY;
		for (double v : logits) {
			if (v > max) {
				max = v;
			}
		}
		double[] out = new double[logits.length];
		double sum = 0;
		for (int i = 0; i < logits.length; i++) {
			out[i] = Math.exp(logits[i] - max);
			sum += out[i];
		}
		for (int i = 0; i < out.length; i++) {
			out[i] /= sum;
		}
		return out;
	}

}

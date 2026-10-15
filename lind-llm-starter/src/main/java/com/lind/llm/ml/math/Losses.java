package com.lind.llm.ml.math;

/**
 * 常用损失函数（标量形式，便于教学）。
 */
public final class Losses {

	private Losses() {
	}

	/** 均方误差的一半：½(ŷ − y)²，梯度恰为 (ŷ − y)。 */
	public static double mseHalf(double yHat, double y) {
		double d = yHat - y;
		return 0.5 * d * d;
	}

	/** 二分类交叉熵；y ∈ {0,1}，p ∈ (0,1)。 */
	public static double binaryCrossEntropy(double p, double y) {
		double eps = 1e-12;
		double pp = Math.min(1 - eps, Math.max(eps, p));
		return -(y * Math.log(pp) + (1 - y) * Math.log(1 - pp));
	}

}

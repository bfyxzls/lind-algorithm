package com.lind.llm.ml.metrics;

import java.util.Objects;

/**
 * 二分类评估（标签 0/1）。
 */
public final class ClassificationMetrics {

	private ClassificationMetrics() {
	}

	public record Report(double accuracy, double precision, double recall, double f1) {
	}

	public static Report evaluate(int[] yTrue, int[] yPred) {
		Objects.requireNonNull(yTrue, "yTrue");
		Objects.requireNonNull(yPred, "yPred");
		if (yTrue.length == 0 || yTrue.length != yPred.length) {
			throw new IllegalArgumentException("标签长度须相同且非空");
		}
		int tp = 0;
		int fp = 0;
		int tn = 0;
		int fn = 0;
		for (int i = 0; i < yTrue.length; i++) {
			int t = yTrue[i];
			int p = yPred[i];
			if (t != 0 && t != 1 || p != 0 && p != 1) {
				throw new IllegalArgumentException("仅支持 0/1 标签");
			}
			if (t == 1 && p == 1) {
				tp++;
			}
			else if (t == 0 && p == 1) {
				fp++;
			}
			else if (t == 0 && p == 0) {
				tn++;
			}
			else {
				fn++;
			}
		}
		double accuracy = (tp + tn) / (double) yTrue.length;
		double precision = tp + fp == 0 ? 0 : tp / (double) (tp + fp);
		double recall = tp + fn == 0 ? 0 : tp / (double) (tp + fn);
		double f1 = precision + recall == 0 ? 0 : 2 * precision * recall / (precision + recall);
		return new Report(accuracy, precision, recall, f1);
	}

}

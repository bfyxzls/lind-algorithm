package com.lind.llm.ml.model;

import com.lind.llm.ml.data.LabeledPoint;
import com.lind.llm.ml.math.Activations;
import com.lind.llm.ml.math.Losses;
import com.lind.llm.ml.math.Vectors;
import com.lind.llm.ml.train.Trainable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 二分类逻辑回归：p = σ(w·x + b)，标签 y ∈ {0,1}。
 */
public final class LogisticRegression implements Trainable {

	private final double[] weights;

	private double bias;

	public LogisticRegression(int inputDim) {
		if (inputDim <= 0) {
			throw new IllegalArgumentException("inputDim 必须 > 0");
		}
		this.weights = new double[inputDim];
		this.bias = 0;
	}

	@Override
	public int inputDim() {
		return weights.length;
	}

	/** 输出正类概率。 */
	@Override
	public double predict(double[] features) {
		Vectors.requireSameLength(features, weights);
		return Activations.sigmoid(Vectors.dot(weights, features) + bias);
	}

	public int predictClass(double[] features) {
		return predict(features) >= 0.5 ? 1 : 0;
	}

	@Override
	public double trainStep(List<LabeledPoint> batch, double learningRate) {
		Objects.requireNonNull(batch, "batch");
		if (batch.isEmpty()) {
			return 0;
		}
		double[] gradW = new double[weights.length];
		double gradB = 0;
		double lossSum = 0;
		for (LabeledPoint p : batch) {
			double y = p.label();
			if (y != 0.0 && y != 1.0) {
				throw new IllegalArgumentException("逻辑回归标签须为 0 或 1，实际: " + y);
			}
			double pHat = predict(p.features());
			lossSum += Losses.binaryCrossEntropy(pHat, y);
			double error = pHat - y;
			for (int i = 0; i < weights.length; i++) {
				gradW[i] += error * p.features()[i];
			}
			gradB += error;
		}
		double n = batch.size();
		for (int i = 0; i < weights.length; i++) {
			weights[i] -= learningRate * (gradW[i] / n);
		}
		bias -= learningRate * (gradB / n);
		return lossSum / n;
	}

	public double[] weights() {
		return Arrays.copyOf(weights, weights.length);
	}

	public double bias() {
		return bias;
	}

}

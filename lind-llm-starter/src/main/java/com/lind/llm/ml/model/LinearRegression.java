package com.lind.llm.ml.model;

import com.lind.llm.ml.data.LabeledPoint;
import com.lind.llm.ml.math.Losses;
import com.lind.llm.ml.math.Vectors;
import com.lind.llm.ml.train.Trainable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 多元线性回归：ŷ = w·x + b，全量 batch SGD。
 */
public final class LinearRegression implements Trainable {

	private final double[] weights;

	private double bias;

	public LinearRegression(int inputDim) {
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

	@Override
	public double predict(double[] features) {
		Vectors.requireSameLength(features, weights);
		return Vectors.dot(weights, features) + bias;
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
			double yHat = predict(p.features());
			double error = yHat - p.label();
			lossSum += Losses.mseHalf(yHat, p.label());
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

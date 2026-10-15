package com.lind.llm.ml.data;

import com.lind.llm.ml.math.Vectors;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Z-Score 标准化：x' = (x − μ) / σ。特征尺度差异大时，对 SGD 收敛至关重要。
 */
public final class StandardScaler {

	private final double[] mean;

	private final double[] std;

	private StandardScaler(double[] mean, double[] std) {
		this.mean = mean;
		this.std = std;
	}

	public static StandardScaler fit(List<LabeledPoint> data) {
		Objects.requireNonNull(data, "data");
		if (data.isEmpty()) {
			throw new IllegalArgumentException("数据为空");
		}
		int dim = data.get(0).dimension();
		double[] mean = new double[dim];
		for (LabeledPoint p : data) {
			if (p.dimension() != dim) {
				throw new IllegalArgumentException("特征维度不一致");
			}
			for (int i = 0; i < dim; i++) {
				mean[i] += p.features()[i];
			}
		}
		for (int i = 0; i < dim; i++) {
			mean[i] /= data.size();
		}
		double[] var = new double[dim];
		for (LabeledPoint p : data) {
			for (int i = 0; i < dim; i++) {
				double d = p.features()[i] - mean[i];
				var[i] += d * d;
			}
		}
		double[] std = new double[dim];
		for (int i = 0; i < dim; i++) {
			std[i] = Math.sqrt(var[i] / data.size());
			if (std[i] < 1e-12) {
				std[i] = 1.0; // 常数特征避免除零
			}
		}
		return new StandardScaler(mean, std);
	}

	public double[] transformFeatures(double[] features) {
		Vectors.requireSameLength(features, mean);
		double[] out = new double[features.length];
		for (int i = 0; i < features.length; i++) {
			out[i] = (features[i] - mean[i]) / std[i];
		}
		return out;
	}

	public List<LabeledPoint> transform(List<LabeledPoint> data) {
		List<LabeledPoint> out = new ArrayList<>(data.size());
		for (LabeledPoint p : data) {
			out.add(new LabeledPoint(transformFeatures(p.features()), p.label()));
		}
		return out;
	}

	public double[] mean() {
		return Vectors.copy(mean);
	}

	public double[] std() {
		return Vectors.copy(std);
	}

}

package com.lind.llm.ml.data;

import com.lind.llm.ml.math.Vectors;
import java.util.Objects;

/**
 * 带标签的样本：特征 x、标签 y（回归为连续值；二分类常用 0/1）。
 */
public record LabeledPoint(double[] features, double label) {

	public LabeledPoint {
		Objects.requireNonNull(features, "features");
		features = Vectors.copy(features);
	}

	public int dimension() {
		return features.length;
	}

}

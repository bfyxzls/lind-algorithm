package com.lind.llm.ml.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * 数据集工具：划分训练 / 测试集。
 */
public final class Datasets {

	private Datasets() {
	}

	public record Split(List<LabeledPoint> train, List<LabeledPoint> test) {
	}

	/**
	 * @param testRatio 测试集比例，建议 (0, 0.5]
	 */
	public static Split trainTestSplit(List<LabeledPoint> data, double testRatio, long seed) {
		Objects.requireNonNull(data, "data");
		if (data.isEmpty()) {
			throw new IllegalArgumentException("数据集为空");
		}
		if (testRatio <= 0 || testRatio >= 1) {
			throw new IllegalArgumentException("testRatio 须在 (0,1) 内");
		}
		List<LabeledPoint> shuffled = new ArrayList<>(data);
		Collections.shuffle(shuffled, new Random(seed));
		int testSize = Math.max(1, (int) Math.round(shuffled.size() * testRatio));
		if (testSize >= shuffled.size()) {
			testSize = shuffled.size() - 1;
		}
		List<LabeledPoint> test = List.copyOf(shuffled.subList(0, testSize));
		List<LabeledPoint> train = List.copyOf(shuffled.subList(testSize, shuffled.size()));
		return new Split(train, test);
	}

}

package com.lind.llm.ml.train;

import java.util.List;

/**
 * 一次训练的摘要。
 */
public record TrainResult(int epochsRun, List<Double> trainLossHistory, List<Double> validLossHistory,
		boolean earlyStopped) {
}

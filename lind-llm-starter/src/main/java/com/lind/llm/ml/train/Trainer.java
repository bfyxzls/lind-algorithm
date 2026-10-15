package com.lind.llm.ml.train;

import com.lind.llm.ml.data.LabeledPoint;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 通用训练循环：支持超参热更新与早停。
 */
public final class Trainer {

	private Trainer() {
	}

	public static TrainResult fit(Trainable model, List<LabeledPoint> train, HyperParameters hp) {
		return fit(model, train, List.of(), hp, null);
	}

	/**
	 * @param onEpochEnd 每个 epoch 结束后回调（可在此动态改 learningRate）
	 */
	public static TrainResult fit(Trainable model, List<LabeledPoint> train, List<LabeledPoint> valid,
			HyperParameters hp, Consumer<EpochContext> onEpochEnd) {
		Objects.requireNonNull(model, "model");
		Objects.requireNonNull(train, "train");
		Objects.requireNonNull(hp, "hp");
		if (train.isEmpty()) {
			throw new IllegalArgumentException("训练集为空");
		}
		List<Double> trainHist = new ArrayList<>();
		List<Double> validHist = new ArrayList<>();
		double bestValid = Double.POSITIVE_INFINITY;
		int badEpochs = 0;
		boolean earlyStopped = false;
		int ran = 0;

		for (int epoch = 1; epoch <= hp.epochs(); epoch++) {
			double lr = hp.learningRate();
			double trainLoss = model.trainStep(train, lr);
			trainHist.add(trainLoss);

			double validLoss = Double.NaN;
			if (valid != null && !valid.isEmpty()) {
				validLoss = averagePredictLoss(model, valid);
				validHist.add(validLoss);
				if (hp.earlyStoppingPatience() > 0) {
					if (validLoss + hp.minDelta() < bestValid) {
						bestValid = validLoss;
						badEpochs = 0;
					}
					else {
						badEpochs++;
						if (badEpochs >= hp.earlyStoppingPatience()) {
							earlyStopped = true;
							ran = epoch;
							if (onEpochEnd != null) {
								onEpochEnd.accept(new EpochContext(epoch, trainLoss, validLoss, hp));
							}
							break;
						}
					}
				}
			}

			ran = epoch;
			if (onEpochEnd != null) {
				onEpochEnd.accept(new EpochContext(epoch, trainLoss, validLoss, hp));
			}
		}
		return new TrainResult(ran, List.copyOf(trainHist), List.copyOf(validHist), earlyStopped);
	}

	/** 用当前模型在数据集上估算平均 |ŷ−y|（通用监控，不替代专用 Metrics）。 */
	public static double averagePredictLoss(Trainable model, List<LabeledPoint> data) {
		double sum = 0;
		for (LabeledPoint p : data) {
			double err = model.predict(p.features()) - p.label();
			sum += err * err;
		}
		return sum / (2.0 * data.size());
	}

	public record EpochContext(int epoch, double trainLoss, double validLoss, HyperParameters hyperParameters) {
	}

}

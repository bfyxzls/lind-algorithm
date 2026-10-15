package com.lind.llm.ml.train;

import com.lind.llm.ml.data.LabeledPoint;
import java.util.List;

/**
 * 可训练模型：一个 batch / 全量数据上做一步参数更新，并返回平均损失。
 */
public interface Trainable {

	/** 特征维度。 */
	int inputDim();

	/**
	 * 使用当前学习率对一批样本做一次梯度更新。
	 * @return 本批平均损失（训练监控用）
	 */
	double trainStep(List<LabeledPoint> batch, double learningRate);

	/** 对单个样本预测（回归输出实数；分类可输出概率）。 */
	double predict(double[] features);

}

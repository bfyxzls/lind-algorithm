package com.lind.llm.ml.train;

/**
 * 可变超参数。训练循环每个 epoch 读取最新值，支持中途动态调整（如学习率衰减）。
 */
public final class HyperParameters {

	private double learningRate = 0.05;

	private int epochs = 200;

	/** 验证损失连续多少个 epoch 无改善则早停；0 表示关闭。 */
	private int earlyStoppingPatience = 0;

	/** 验证损失至少改善多少才算「更好」。 */
	private double minDelta = 1e-6;

	public static HyperParameters defaults() {
		return new HyperParameters();
	}

	public double learningRate() {
		return learningRate;
	}

	public HyperParameters learningRate(double learningRate) {
		if (learningRate <= 0) {
			throw new IllegalArgumentException("learningRate 必须 > 0");
		}
		this.learningRate = learningRate;
		return this;
	}

	public int epochs() {
		return epochs;
	}

	public HyperParameters epochs(int epochs) {
		if (epochs <= 0) {
			throw new IllegalArgumentException("epochs 必须 > 0");
		}
		this.epochs = epochs;
		return this;
	}

	public int earlyStoppingPatience() {
		return earlyStoppingPatience;
	}

	public HyperParameters earlyStoppingPatience(int earlyStoppingPatience) {
		if (earlyStoppingPatience < 0) {
			throw new IllegalArgumentException("earlyStoppingPatience 不能为负");
		}
		this.earlyStoppingPatience = earlyStoppingPatience;
		return this;
	}

	public double minDelta() {
		return minDelta;
	}

	public HyperParameters minDelta(double minDelta) {
		if (minDelta < 0) {
			throw new IllegalArgumentException("minDelta 不能为负");
		}
		this.minDelta = minDelta;
		return this;
	}

}

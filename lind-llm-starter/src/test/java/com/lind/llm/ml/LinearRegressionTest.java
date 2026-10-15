package com.lind.llm.ml;

import static org.assertj.core.api.Assertions.assertThat;

import com.lind.llm.ml.data.LabeledPoint;
import com.lind.llm.ml.data.StandardScaler;
import com.lind.llm.ml.metrics.RegressionMetrics;
import com.lind.llm.ml.model.LinearRegression;
import com.lind.llm.ml.train.HyperParameters;
import com.lind.llm.ml.train.TrainResult;
import com.lind.llm.ml.train.Trainer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 线性回归入门测试。
 *
 * <p>
 * <b>回归</b>要解决的问题：根据输入 x，预测一个<strong>连续数值</strong> y。 例如：用房屋面积预测房价。
 *
 * <p>
 * 本测试构造最简单的关系：大约 $y = 2x$（带一点点噪声），看模型能否「学」出这条直线。
 */
class LinearRegressionTest {

	/**
	 * 场景：人造数据接近 $y = 2x$，训练后损失应下降，拟合优度 R² 应很高，且能外推预测。
	 */
	@Test
	void learnsApproximateLineYEqualsTwoX() {
		// ---------- 1. 准备带标签的样本 ----------
		// LabeledPoint = (特征 features, 标签 label)
		// 这里特征只有 1 维：x；标签是 y ≈ 2x + 0.1
		// 「+0.1」是故意加的一点点偏差，模拟真实世界里不会完美落在直线上的噪声。
		List<LabeledPoint> raw = new ArrayList<>();
		for (int i = 1; i <= 40; i++) {
			double x = i;
			raw.add(new LabeledPoint(new double[] { x }, 2 * x + 0.1));
		}

		// ---------- 2. 特征标准化（强烈建议初学者记住这一步）----------
		// 不同特征如果数值差几个数量级（如面积 vs 房间数），梯度下降会很难训。
		// StandardScaler 把特征变成大致「均值 0、方差 1」的尺度：
		// x' = (x - 均值) / 标准差
		// fit：在训练集上统计均值和标准差；transform：用同一套统计量变换数据。
		StandardScaler scaler = StandardScaler.fit(raw);
		List<LabeledPoint> data = scaler.transform(raw);

		// ---------- 3. 建模型 + 设超参数 ----------
		// inputDim=1：特征是 1 维，对应 ŷ = w * x + b
		LinearRegression model = new LinearRegression(1);

		// 超参数不是「学」出来的，是你事先定的：
		// - learningRate（学习率 η）：每一步沿梯度走多大。太大易发散，太小学得慢。
		// - epochs：把全部训练数据完整过几轮。
		HyperParameters hp = HyperParameters.defaults().learningRate(0.1).epochs(800);

		// ---------- 4. 训练 ----------
		// Trainer 会反复：预测 → 算损失 → 算梯度 → 更新 w、b
		TrainResult result = Trainer.fit(model, data, hp);

		// ---------- 5. 检查「训练有没有在变好」----------
		// 损失（loss）衡量预测错得多离谱。正常训练时，最后一轮损失应小于第一轮。
		assertThat(result.trainLossHistory().get(result.trainLossHistory().size() - 1))
				.isLessThan(result.trainLossHistory().get(0));

		// ---------- 6. 用 R² 看拟合得好不好 ----------
		// R²（决定系数）接近 1 表示模型几乎解释了标签的变化；接近 0 或负数表示很差。
		double[] yTrue = new double[data.size()];
		double[] yPred = new double[data.size()];
		for (int i = 0; i < data.size(); i++) {
			yTrue[i] = data.get(i).label(); // 真实 y
			yPred[i] = model.predict(data.get(i).features()); // 模型预测 ŷ
		}
		assertThat(RegressionMetrics.r2(yTrue, yPred)).isGreaterThan(0.95);

		// ---------- 7. 对新样本做预测（注意：也要先走同一套 scaler）----------
		// 训练时用的是标准化后的 x，预测时必须用同一套均值/方差变换，否则尺度对不上。
		// 原始 x=10 时，真值大约是 2*10+0.1=20.1，所以期望落在 18~22.5 这个宽松区间。
		double pred = model.predict(scaler.transformFeatures(new double[] { 10 }));
		assertThat(pred).isBetween(18.0, 22.5);
	}

	/**
	 * 演示「超参数可以在训练过程中动态改」——为后续 LLM 在线调温/调学习率打样。
	 *
	 * <p>
	 * 常见做法：前期用大学习率快速靠近最优解，后期减小学习率做精细调整（学习率衰减）。
	 */
	@Test
	void learningRateCanBeDecayedDuringTraining() {
		// 三个点刚好在直线 y = x 上，任务很简单，重点不在精度，而在「中途改 lr」。
		List<LabeledPoint> data = List.of(new LabeledPoint(new double[] { 0 }, 0),
				new LabeledPoint(new double[] { 1 }, 1), new LabeledPoint(new double[] { 2 }, 2));

		LinearRegression model = new LinearRegression(1);
		HyperParameters hp = HyperParameters.defaults().learningRate(0.5).epochs(40);

		// onEpochEnd 回调：每个 epoch 结束后执行。
		// 第 10 轮时把学习率从 0.5 降到 0.05；Trainer 下一轮会读到新的 learningRate()。
		TrainResult result = Trainer.fit(model, data, List.of(), hp, ctx -> {
			if (ctx.epoch() == 10) {
				hp.learningRate(0.05);
			}
		});

		assertThat(result.epochsRun()).isEqualTo(40);
		assertThat(hp.learningRate()).isEqualTo(0.05);
	}

}

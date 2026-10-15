package com.lind.llm.ml;

import static org.assertj.core.api.Assertions.assertThat;

import com.lind.llm.ml.data.LabeledPoint;
import com.lind.llm.ml.metrics.ClassificationMetrics;
import com.lind.llm.ml.model.LogisticRegression;
import com.lind.llm.ml.train.HyperParameters;
import com.lind.llm.ml.train.Trainer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 逻辑回归（二分类）入门测试。
 *
 * <p>
 * <b>分类</b>与回归不同：标签不是连续房价，而是<strong>类别</strong>。 二分类最常见：垃圾邮件/正常、通过/不通过，标签用 0 和 1 表示。
 *
 * <p>
 * 逻辑回归先算线性得分 $z = w·x + b$，再经 Sigmoid 变成概率 $p∈(0,1)$：
 * <ul>
 * <li>$p ≥ 0.5$ → 预测为类别 1</li>
 * <li>$p &lt; 0.5$ → 预测为类别 0</li>
 * </ul>
 */
class LogisticRegressionTest {

	/**
	 * 场景：数轴上左右两团点——左边标 0、右边标 1。模型应学会「靠左是 0、靠右是 1」。
	 */
	@Test
	void separatesTwoClustersOnXAxis() {
		// ---------- 1. 构造「容易分开」的两类数据 ----------
		// 左簇：x 在 -2 附近，标签 0
		// 右簇：x 在 +2 附近，标签 1
		// 两类之间有空隙，对初学者最友好：若算法正确，准确率应接近 100%。
		List<LabeledPoint> data = new ArrayList<>();
		for (int i = 0; i < 30; i++) {
			data.add(new LabeledPoint(new double[] { -2.0 - i * 0.01 }, 0));
			data.add(new LabeledPoint(new double[] { 2.0 + i * 0.01 }, 1));
		}

		// ---------- 2. 训练 ----------
		// inputDim=1：只用一个特征 x
		LogisticRegression model = new LogisticRegression(1);
		// 学习率略大、轮数够多，让它在这个简单任务上充分收敛
		HyperParameters hp = HyperParameters.defaults().learningRate(0.5).epochs(400);
		Trainer.fit(model, data, hp);

		// ---------- 3. 在训练集上算分类指标 ----------
		// Accuracy（准确率）= 预测对的样本数 / 总样本数
		// 另外还有 Precision / Recall / F1（见 ClassificationMetrics），这里先看准确率即可。
		int[] yTrue = new int[data.size()];
		int[] yPred = new int[data.size()];
		for (int i = 0; i < data.size(); i++) {
			yTrue[i] = (int) data.get(i).label();
			yPred[i] = model.predictClass(data.get(i).features()); // 概率阈值后的 0/1
		}
		ClassificationMetrics.Report report = ClassificationMetrics.evaluate(yTrue, yPred);
		assertThat(report.accuracy()).isGreaterThan(0.9);

		// ---------- 4. 抽两个「从没见过的」点做常识检查 ----------
		// x=-3 更靠左 → 应为 0；x=3 更靠右 → 应为 1
		assertThat(model.predictClass(new double[] { -3 })).isEqualTo(0);
		assertThat(model.predictClass(new double[] { 3 })).isEqualTo(1);
	}

}

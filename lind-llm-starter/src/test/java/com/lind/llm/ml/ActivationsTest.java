package com.lind.llm.ml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.lind.llm.ml.math.Activations;
import org.junit.jupiter.api.Test;

/**
 * 激活函数入门测试。
 *
 * <p>
 * 神经网络 / 逻辑回归里，「激活」就是给线性计算结果加一个非线性变换。 没有非线性，多层网络叠起来仍等价于一层线性，表达能力很弱。
 *
 * <p>
 * 本测试覆盖两个最常见的：
 * <ul>
 * <li><b>Sigmoid</b>：把任意实数压到 (0,1)，常当作「概率」</li>
 * <li><b>Softmax</b>：把一组分数变成「加起来为 1 的概率分布」，用于多分类</li>
 * </ul>
 */
class ActivationsTest {

	@Test
	void sigmoidAndSoftmaxBasics() {
		// ---------- Sigmoid：σ(z) = 1 / (1 + e^{-z}) ----------
		// z=0 时正好在中间 → 0.5
		assertThat(Activations.sigmoid(0)).isCloseTo(0.5, within(1e-9));
		// z 很大（正）→ 接近 1（「非常像正类」）
		assertThat(Activations.sigmoid(20)).isGreaterThan(0.99);
		// z 很小（负）→ 接近 0（「非常不像正类」）
		assertThat(Activations.sigmoid(-20)).isLessThan(0.01);

		// ---------- Softmax：把 logits [1,2,3] 变成概率 ----------
		// 哪个原始分数最大，对应概率也应最大；三者之和必须 ≈ 1。
		double[] p = Activations.softmax(new double[] { 1, 2, 3 });
		assertThat(p).hasSize(3);
		assertThat(p[0] + p[1] + p[2]).isCloseTo(1.0, within(1e-9));
		// 3 > 2 > 1，所以概率也应 p[2] > p[1] > p[0]
		assertThat(p[2]).isGreaterThan(p[1]).isGreaterThan(p[0]);
	}

}

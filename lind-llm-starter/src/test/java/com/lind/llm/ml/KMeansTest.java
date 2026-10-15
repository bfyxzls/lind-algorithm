package com.lind.llm.ml;

import static org.assertj.core.api.Assertions.assertThat;

import com.lind.llm.ml.model.KMeans;
import org.junit.jupiter.api.Test;

/**
 * K-Means 聚类入门测试。
 *
 * <p>
 * <b>无监督学习</b>：没有「正确答案」标签 y。算法只看点长什么样，把相似的点分到同一组（簇）。
 *
 * <p>
 * K-Means 的直觉：
 * <ol>
 * <li>先随便选 k 个「中心」（centroid）</li>
 * <li>每个点归到离它最近的中心</li>
 * <li>每个中心挪到该簇所有点的平均值</li>
 * <li>重复 2~3，直到几乎不再变化</li>
 * </ol>
 *
 * 你需要事先指定 k（簇的个数）。本例 k=2。
 */
class KMeansTest {

	/**
	 * 场景：二维平面上故意放两团点——一团在原点附近，一团在 (5,5) 附近。 拟合后，这两团应被分到不同簇。
	 */
	@Test
	void clustersTwoWellSeparatedGroups() {
		// 前 3 个点：靠近 (0,0)；后 3 个点：靠近 (5,5)
		double[][] points = { { 0, 0 }, { 0.1, -0.1 }, { -0.1, 0.05 }, { 5, 5 }, { 5.1, 4.9 }, { 4.9, 5.2 } };

		// k=2：希望得到 2 个簇
		// maxIters=50：最多迭代 50 轮（通常更早就稳定）
		// seed=42：随机初始化中心用的种子；固定种子让测试结果可复现
		KMeans kMeans = new KMeans(2, 50, 42L);
		kMeans.fit(points);

		// predict：问「这个点属于哪一簇？」，返回簇编号（0 或 1，编号本身无业务含义）
		int clusterNearOrigin = kMeans.predict(new double[] { 0, 0 });
		int clusterNearFive = kMeans.predict(new double[] { 5, 5 });

		// 两团相距很远，不应分到同一簇
		assertThat(clusterNearOrigin).isNotEqualTo(clusterNearFive);

		// centroids()：两个中心，每个中心是 2 维坐标 → 形状 [2][2]
		assertThat(kMeans.centroids()).hasDimensions(2, 2);
	}

}

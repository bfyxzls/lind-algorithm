package com.lind.algorithm.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * {@link DecisionTree} 单元测试。
 */
public class DecisionTreeTest {

	@Test
	void predictLoanRisk() {
		DecisionTree tree = new DecisionTree(
				DecisionTree.Node.branch("income").when("high", DecisionTree.Node.leaf("approve"))
						.when("medium",
								DecisionTree.Node.branch("credit").when("good", DecisionTree.Node.leaf("approve"))
										.when("bad", DecisionTree.Node.leaf("reject")).build())
						.when("low", DecisionTree.Node.leaf("reject"))
						.otherwise(DecisionTree.Node.leaf("manual_review")).build());

		assertEquals("approve", tree.predict(Map.of("income", "high")));
		assertEquals("approve", tree.predict(Map.of("income", "medium", "credit", "good")));
		assertEquals("reject", tree.predict(Map.of("income", "medium", "credit", "bad")));
		assertEquals("manual_review", tree.predict(Map.of("income", "unknown")));
	}

	@Test
	void missingFeatureFails() {
		DecisionTree tree = new DecisionTree(
				DecisionTree.Node.branch("color").when("red", DecisionTree.Node.leaf("a")).build());
		assertThrows(IllegalArgumentException.class, () -> tree.predict(Map.of()));
	}

}

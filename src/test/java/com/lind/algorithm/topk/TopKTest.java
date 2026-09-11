package com.lind.algorithm.topk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * TopK / CMS 单元测试。
 */
public class TopKTest {

	@Test
	void topKByFrequency() {
		TopKCounter<String> counter = new TopKCounter<>(2);
		counter.offer("a", 5);
		counter.offer("b", 3);
		counter.offer("c", 10);
		List<TopKCounter.Entry<String>> top = counter.topK();
		assertEquals(2, top.size());
		assertEquals("c", top.get(0).item());
		assertEquals("a", top.get(1).item());
	}

	@Test
	void countMinSketchEstimatesAtLeastTrueCount() {
		CountMinSketch cms = new CountMinSketch(4, 64);
		cms.add("hot", 7);
		cms.add("cold", 1);
		assertTrue(cms.estimate("hot") >= 7);
		assertTrue(cms.estimate("cold") >= 1);
	}

}

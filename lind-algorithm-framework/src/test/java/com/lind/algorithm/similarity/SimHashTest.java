package com.lind.algorithm.similarity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * {@link SimHash} 单元测试。
 */
public class SimHashTest {

	@Test
	void identicalTextsHaveZeroDistance() {
		long a = SimHash.hash64("hello world algorithm");
		long b = SimHash.hash64("hello world algorithm");
		assertEquals(0, SimHash.hammingDistance(a, b));
		assertTrue(SimHash.similar("hello world algorithm", "hello world algorithm", 0));
	}

	@Test
	void similarTextsAreClose() {
		assertTrue(SimHash.similar("今天天气很好适合出门", "今天天气很好适合外出", 10));
	}

	@Test
	void unrelatedTextsAreFar() {
		long a = SimHash.hash64("quantum physics research paper");
		long b = SimHash.hash64("chocolate cake baking recipe");
		assertTrue(SimHash.hammingDistance(a, b) > 5);
		assertFalse(SimHash.similar("quantum physics research paper", "chocolate cake baking recipe", 3));
	}

}

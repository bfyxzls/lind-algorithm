package com.lind.algorithm.bloom;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * {@link BloomFilter} 单元测试。
 */
public class BloomFilterTest {

	@Test
	void putAndMightContain() {
		BloomFilter filter = new BloomFilter(1000, 0.01);
		filter.put("apple");
		filter.put("banana");
		assertTrue(filter.mightContain("apple"));
		assertTrue(filter.mightContain("banana"));
		assertFalse(filter.mightContain("not-inserted-zzz"));
	}

}

package com.lind.algorithm.cuckoo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * {@link CuckooFilter} 单元测试。
 */
public class CuckooFilterTest {

	@Test
	void putMightContainAndDelete() {
		CuckooFilter filter = new CuckooFilter(1000, 0.01);
		assertTrue(filter.put("apple"));
		assertTrue(filter.mightContain("apple"));
		assertFalse(filter.mightContain("not-there-zzz"));
		assertTrue(filter.delete("apple"));
		assertFalse(filter.mightContain("apple"));
		assertFalse(filter.delete("apple"));
	}

	@Test
	void manyInsertsStayFindable() {
		CuckooFilter filter = new CuckooFilter(5000, 0.01);
		for (int i = 0; i < 2000; i++) {
			assertTrue(filter.put("k-" + i));
		}
		for (int i = 0; i < 2000; i++) {
			assertTrue(filter.mightContain("k-" + i));
		}
	}

}

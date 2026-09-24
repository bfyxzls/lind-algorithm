package com.lind.algorithm.hll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * {@link HyperLogLog} 单元测试。
 */
public class HyperLogLogTest {

	@Test
	void emptyIsZero() {
		assertEquals(0, new HyperLogLog(10).cardinality());
	}

	@Test
	void estimatesDistinctWithinTolerance() {
		HyperLogLog hll = new HyperLogLog(14);
		int n = 10_000;
		for (int i = 0; i < n; i++) {
			hll.add("id-" + i);
		}
		long estimate = hll.cardinality();
		double error = Math.abs(estimate - n) / (double) n;
		assertTrue(error < 0.05, "error=" + error + " estimate=" + estimate);
	}

	@Test
	void duplicatesDoNotInflateMuch() {
		HyperLogLog hll = new HyperLogLog(12);
		for (int i = 0; i < 1000; i++) {
			hll.add("same");
		}
		assertTrue(hll.cardinality() <= 3);
	}

	@Test
	void mergeCombinesRegisters() {
		HyperLogLog a = new HyperLogLog(10);
		HyperLogLog b = new HyperLogLog(10);
		for (int i = 0; i < 500; i++) {
			a.add("a-" + i);
			b.add("b-" + i);
		}
		a.merge(b);
		long estimate = a.cardinality();
		assertTrue(estimate > 800 && estimate < 1200, "estimate=" + estimate);
	}

	@Test
	void rejectsBadPrecisionAndMergeMismatch() {
		assertThrows(IllegalArgumentException.class, () -> new HyperLogLog(3));
		HyperLogLog a = new HyperLogLog(10);
		assertThrows(IllegalArgumentException.class, () -> a.merge(new HyperLogLog(12)));
	}

}

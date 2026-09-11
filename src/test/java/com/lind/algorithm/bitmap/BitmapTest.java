package com.lind.algorithm.bitmap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * {@link Bitmap} 单元测试。
 */
public class BitmapTest {

	@Test
	void setAndSetOps() {
		Bitmap a = new Bitmap();
		Bitmap b = new Bitmap();
		a.set(1);
		a.set(3);
		b.set(3);
		b.set(5);
		assertTrue(a.get(1));
		assertFalse(a.get(2));
		assertEquals(1, a.and(b).cardinality());
		assertEquals(3, a.or(b).cardinality());
		assertEquals(2, a.xor(b).cardinality());
	}

}

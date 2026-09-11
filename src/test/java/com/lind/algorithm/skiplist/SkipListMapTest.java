package com.lind.algorithm.skiplist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@link SkipListMap} 单元测试。
 */
public class SkipListMapTest {

	@Test
	void putGetRemoveKeepOrder() {
		SkipListMap<Integer, String> map = new SkipListMap<>();
		map.put(3, "c");
		map.put(1, "a");
		map.put(2, "b");
		map.put(2, "B");
		assertEquals(3, map.size());
		assertEquals("B", map.get(2).orElseThrow());
		assertEquals(List.of(1, 2, 3), map.keys());
		assertTrue(map.remove(2));
		assertFalse(map.containsKey(2));
		assertEquals(List.of(1, 3), map.keys());
	}

}

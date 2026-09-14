package com.lind.algorithm.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * 缓存单元测试。
 */
public class CacheTest {

	@Test
	void lruEvictsEldest() {
		LruCache<Integer, String> cache = new LruCache<>(2);
		cache.put(1, "a");
		cache.put(2, "b");
		cache.get(1);
		cache.put(3, "c");
		assertTrue(cache.get(1).isPresent());
		assertTrue(cache.get(2).isEmpty());
		assertEquals("c", cache.get(3).orElseThrow());
	}

	@Test
	void lfuEvictsLeastFrequent() {
		LfuCache<Integer, String> cache = new LfuCache<>(2);
		cache.put(1, "a");
		cache.put(2, "b");
		cache.get(1);
		cache.get(1);
		cache.put(3, "c");
		assertTrue(cache.get(1).isPresent());
		assertTrue(cache.get(2).isEmpty());
		assertEquals("c", cache.get(3).orElseThrow());
	}

}

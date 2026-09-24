package com.lind.algorithm.geo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * {@link Geohash} 单元测试。
 */
public class GeohashTest {

	@Test
	void encodeDecodeRoundTripBeijing() {
		String hash = Geohash.encode(39.9042, 116.4074, 6);
		LatLon point = Geohash.decode(hash);
		assertTrue(Math.abs(point.latitude() - 39.9042) < 0.05);
		assertTrue(Math.abs(point.longitude() - 116.4074) < 0.05);
	}

	@Test
	void knownHash() {
		// wx4g0e is a common sample near Beijing for precision 6
		String hash = Geohash.encode(39.9289, 116.3883, 6);
		assertEquals(6, hash.length());
		LatLon again = Geohash.decode(hash);
		assertEquals(hash, Geohash.encode(again.latitude(), again.longitude(), 6));
	}

	@Test
	void neighborsAreEightDistinct() {
		String hash = Geohash.encode(31.2304, 121.4737, 5);
		List<String> neighbors = Geohash.neighbors(hash);
		assertEquals(8, neighbors.size());
		assertTrue(neighbors.stream().distinct().count() == 8);
		assertTrue(neighbors.stream().noneMatch(hash::equals));
	}

}

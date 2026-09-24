package com.lind.algorithm.geo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Geohash：经纬度与 base32 字符串互转，并计算相邻格子。
 */
public final class Geohash {

	private static final String BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";

	private static final String[][] NEIGHBORS = {
			{ "bc01fg45238967deuvhjyznpkmstqrwx", "238967debc01fg45kmstqrwxuvhjyznp",
					"p0r21436x8zb9dcf5h7kjnmqesgutwvy", "14365h7k9dcfesgujnmqp0r2twvyx8zb" },
			{ "p0r21436x8zb9dcf5h7kjnmqesgutwvy", "14365h7k9dcfesgujnmqp0r2twvyx8zb",
					"bc01fg45238967deuvhjyznpkmstqrwx", "238967debc01fg45kmstqrwxuvhjyznp" } };

	private static final String[][] BORDERS = { { "bcfguvyz", "0145hjnp", "prxz", "028b" },
			{ "prxz", "028b", "bcfguvyz", "0145hjnp" } };

	private Geohash() {
	}

	/**
	 * @param precision 字符长度 1..12
	 */
	public static String encode(double latitude, double longitude, int precision) {
		if (latitude < -90 || latitude > 90) {
			throw new IllegalArgumentException("latitude out of range");
		}
		if (longitude < -180 || longitude > 180) {
			throw new IllegalArgumentException("longitude out of range");
		}
		if (precision < 1 || precision > 12) {
			throw new IllegalArgumentException("precision must be in [1,12]");
		}
		double[] latInterval = { -90.0, 90.0 };
		double[] lonInterval = { -180.0, 180.0 };
		StringBuilder hash = new StringBuilder(precision);
		boolean isEven = true;
		int bit = 0;
		int ch = 0;
		while (hash.length() < precision) {
			if (isEven) {
				double mid = (lonInterval[0] + lonInterval[1]) / 2;
				if (longitude >= mid) {
					ch |= 1 << (4 - bit);
					lonInterval[0] = mid;
				}
				else {
					lonInterval[1] = mid;
				}
			}
			else {
				double mid = (latInterval[0] + latInterval[1]) / 2;
				if (latitude >= mid) {
					ch |= 1 << (4 - bit);
					latInterval[0] = mid;
				}
				else {
					latInterval[1] = mid;
				}
			}
			isEven = !isEven;
			if (bit < 4) {
				bit++;
			}
			else {
				hash.append(BASE32.charAt(ch));
				bit = 0;
				ch = 0;
			}
		}
		return hash.toString();
	}

	public static LatLon decode(String hash) {
		Objects.requireNonNull(hash, "hash");
		if (hash.isEmpty() || hash.length() > 12) {
			throw new IllegalArgumentException("invalid geohash length");
		}
		double[] latInterval = { -90.0, 90.0 };
		double[] lonInterval = { -180.0, 180.0 };
		boolean isEven = true;
		for (int i = 0; i < hash.length(); i++) {
			int cd = BASE32.indexOf(Character.toLowerCase(hash.charAt(i)));
			if (cd < 0) {
				throw new IllegalArgumentException("invalid geohash char: " + hash.charAt(i));
			}
			for (int mask = 16; mask > 0; mask >>= 1) {
				if (isEven) {
					refine(lonInterval, (cd & mask) != 0);
				}
				else {
					refine(latInterval, (cd & mask) != 0);
				}
				isEven = !isEven;
			}
		}
		return new LatLon((latInterval[0] + latInterval[1]) / 2, (lonInterval[0] + lonInterval[1]) / 2);
	}

	/**
	 * 返回 8 个相邻格子（不含自身）：N, NE, E, SE, S, SW, W, NW。
	 */
	public static List<String> neighbors(String hash) {
		Objects.requireNonNull(hash, "hash");
		String n = adjacent(hash, 2);
		String s = adjacent(hash, 3);
		String e = adjacent(hash, 0);
		String w = adjacent(hash, 1);
		List<String> result = new ArrayList<>(8);
		result.add(n);
		result.add(adjacent(n, 0));
		result.add(e);
		result.add(adjacent(s, 0));
		result.add(s);
		result.add(adjacent(s, 1));
		result.add(w);
		result.add(adjacent(n, 1));
		return result;
	}

	/** direction: 0=right/east, 1=left/west, 2=top/north, 3=bottom/south */
	static String adjacent(String hash, int direction) {
		hash = hash.toLowerCase();
		int last = hash.length() - 1;
		char lastChar = hash.charAt(last);
		int type = hash.length() % 2;
		String base = hash.substring(0, last);
		if (BORDERS[type][direction].indexOf(lastChar) >= 0 && !base.isEmpty()) {
			base = adjacent(base, direction);
		}
		int idx = NEIGHBORS[type][direction].indexOf(lastChar);
		return base + BASE32.charAt(idx);
	}

	private static void refine(double[] interval, boolean bitSet) {
		double mid = (interval[0] + interval[1]) / 2;
		if (bitSet) {
			interval[0] = mid;
		}
		else {
			interval[1] = mid;
		}
	}

}

package com.lind.algorithm.bitmap;

import java.util.BitSet;
import java.util.Objects;

/**
 * 位图：紧凑存储布尔集合，适合签到、去重标记、人群包交集等。
 */
public final class Bitmap {

	private final BitSet bits;

	public Bitmap() {
		this.bits = new BitSet();
	}

	public Bitmap(int nbits) {
		if (nbits < 0) {
			throw new IllegalArgumentException("nbits must be >= 0");
		}
		this.bits = new BitSet(nbits);
	}

	public void set(int index) {
		checkIndex(index);
		bits.set(index);
	}

	public void set(int index, boolean value) {
		checkIndex(index);
		bits.set(index, value);
	}

	public void clear(int index) {
		checkIndex(index);
		bits.clear(index);
	}

	public boolean get(int index) {
		checkIndex(index);
		return bits.get(index);
	}

	public int cardinality() {
		return bits.cardinality();
	}

	public Bitmap and(Bitmap other) {
		Objects.requireNonNull(other, "other");
		Bitmap result = new Bitmap();
		result.bits.or(this.bits);
		result.bits.and(other.bits);
		return result;
	}

	public Bitmap or(Bitmap other) {
		Objects.requireNonNull(other, "other");
		Bitmap result = new Bitmap();
		result.bits.or(this.bits);
		result.bits.or(other.bits);
		return result;
	}

	public Bitmap xor(Bitmap other) {
		Objects.requireNonNull(other, "other");
		Bitmap result = new Bitmap();
		result.bits.or(this.bits);
		result.bits.xor(other.bits);
		return result;
	}

	private static void checkIndex(int index) {
		if (index < 0) {
			throw new IllegalArgumentException("index must be >= 0");
		}
	}

}

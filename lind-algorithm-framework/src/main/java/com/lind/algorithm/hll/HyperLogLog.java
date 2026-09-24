package com.lind.algorithm.hll;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * HyperLogLog：概率型基数估计，用固定内存近似 UV / 去重计数。
 */
public final class HyperLogLog {

	private static final double[] ALPHA = { 0.673, 0.697, 0.709 };

	private final int p;

	private final int m;

	private final byte[] registers;

	/**
	 * @param p 精度位，取值 4..16，寄存器数 m = 2^p
	 */
	public HyperLogLog(int p) {
		if (p < 4 || p > 16) {
			throw new IllegalArgumentException("p must be in [4,16]");
		}
		this.p = p;
		this.m = 1 << p;
		this.registers = new byte[m];
	}

	public void add(String value) {
		Objects.requireNonNull(value, "value");
		add(value.getBytes(StandardCharsets.UTF_8));
	}

	public void add(byte[] value) {
		Objects.requireNonNull(value, "value");
		long hash = murmurHash64(value);
		int index = (int) (hash >>> (64 - p));
		long w = hash << p;
		int rho = Long.numberOfLeadingZeros(w) + 1;
		if (rho > Byte.MAX_VALUE) {
			rho = Byte.MAX_VALUE;
		}
		if (rho > registers[index]) {
			registers[index] = (byte) rho;
		}
	}

	/**
	 * 估计不重复元素个数。
	 */
	public long cardinality() {
		double sum = 0;
		int zeros = 0;
		for (byte register : registers) {
			sum += Math.pow(2, -register);
			if (register == 0) {
				zeros++;
			}
		}
		double estimate = alpha() * m * m / sum;
		if (estimate <= 2.5 * m) {
			if (zeros > 0) {
				return Math.round(m * Math.log((double) m / zeros));
			}
			return Math.round(estimate);
		}
		if (estimate <= (1L << 32) / 30.0) {
			return Math.round(estimate);
		}
		return Math.round(-1L * (1L << 32) * Math.log(1.0 - estimate / (1L << 32)));
	}

	/**
	 * 合并另一个同精度 HLL（寄存器取 max）。
	 */
	public void merge(HyperLogLog other) {
		Objects.requireNonNull(other, "other");
		if (other.p != this.p) {
			throw new IllegalArgumentException("precision mismatch: " + other.p + " != " + this.p);
		}
		for (int i = 0; i < m; i++) {
			if (other.registers[i] > registers[i]) {
				registers[i] = other.registers[i];
			}
		}
	}

	public int precision() {
		return p;
	}

	public int registerCount() {
		return m;
	}

	private double alpha() {
		if (m == 16) {
			return ALPHA[0];
		}
		if (m == 32) {
			return ALPHA[1];
		}
		if (m == 64) {
			return ALPHA[2];
		}
		return 0.7213 / (1.0 + 1.079 / m);
	}

	static long murmurHash64(byte[] data) {
		final long seed = 0x9747b28cL;
		final long mul = 0xc6a4a7935bd1e995L;
		final int r = 47;
		long h = seed ^ (data.length * mul);
		int length8 = data.length / 8;
		for (int i = 0; i < length8; i++) {
			int i8 = i * 8;
			long k = ((long) data[i8] & 0xff) + (((long) data[i8 + 1] & 0xff) << 8)
					+ (((long) data[i8 + 2] & 0xff) << 16) + (((long) data[i8 + 3] & 0xff) << 24)
					+ (((long) data[i8 + 4] & 0xff) << 32) + (((long) data[i8 + 5] & 0xff) << 40)
					+ (((long) data[i8 + 6] & 0xff) << 48) + (((long) data[i8 + 7] & 0xff) << 56);
			k *= mul;
			k ^= k >>> r;
			k *= mul;
			h ^= k;
			h *= mul;
		}
		int remaining = data.length % 8;
		int offset = length8 * 8;
		switch (remaining) {
			case 7:
				h ^= (long) (data[offset + 6] & 0xff) << 48;
			case 6:
				h ^= (long) (data[offset + 5] & 0xff) << 40;
			case 5:
				h ^= (long) (data[offset + 4] & 0xff) << 32;
			case 4:
				h ^= (long) (data[offset + 3] & 0xff) << 24;
			case 3:
				h ^= (long) (data[offset + 2] & 0xff) << 16;
			case 2:
				h ^= (long) (data[offset + 1] & 0xff) << 8;
			case 1:
				h ^= (long) (data[offset] & 0xff);
				h *= mul;
			default:
				break;
		}
		h ^= h >>> r;
		h *= mul;
		h ^= h >>> r;
		return h;
	}

	@Override
	public String toString() {
		return "HyperLogLog{p=" + p + ", m=" + m + ", registers="
				+ Arrays.toString(Arrays.copyOf(registers, Math.min(8, m))) + "...}";
	}

}

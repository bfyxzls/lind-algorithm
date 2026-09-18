package com.lind.data.redis;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 基于 Redis BIT 的 Bloom 过滤器（无需 RedisBloom 模块）。
 */
public final class RedisBloomFilter {

	private final RedisCommands redis;

	private final String key;

	private final int bitSize;

	private final int hashCount;

	public RedisBloomFilter(RedisCommands redis, String key, long expectedInsertions, double falsePositiveRate) {
		this.redis = Objects.requireNonNull(redis, "redis");
		this.key = Objects.requireNonNull(key, "key");
		if (key.isBlank()) {
			throw new IllegalArgumentException("key must not be blank");
		}
		if (expectedInsertions <= 0) {
			throw new IllegalArgumentException("expectedInsertions must be > 0");
		}
		if (falsePositiveRate <= 0 || falsePositiveRate >= 1) {
			throw new IllegalArgumentException("falsePositiveRate must be in (0,1)");
		}
		this.bitSize = optimalNumOfBits(expectedInsertions, falsePositiveRate);
		this.hashCount = optimalNumOfHashFunctions(expectedInsertions, bitSize);
	}

	public void put(String value) {
		Objects.requireNonNull(value, "value");
		for (int index : indexes(value)) {
			redis.setBit(key, index, true);
		}
	}

	public boolean mightContain(String value) {
		Objects.requireNonNull(value, "value");
		for (int index : indexes(value)) {
			if (!redis.getBit(key, index)) {
				return false;
			}
		}
		return true;
	}

	public int bitSize() {
		return bitSize;
	}

	public int hashCount() {
		return hashCount;
	}

	private int[] indexes(String value) {
		long hash64 = murmurHash64(value.getBytes(StandardCharsets.UTF_8));
		int hash1 = (int) hash64;
		int hash2 = (int) (hash64 >>> 32);
		int[] result = new int[hashCount];
		for (int i = 0; i < hashCount; i++) {
			result[i] = Math.floorMod(hash1 + i * hash2, bitSize);
		}
		return result;
	}

	static int optimalNumOfBits(long n, double p) {
		return (int) Math.min(Integer.MAX_VALUE - 8, Math.ceil(-n * Math.log(p) / (Math.log(2) * Math.log(2))));
	}

	static int optimalNumOfHashFunctions(long n, long m) {
		return Math.max(1, (int) Math.round((double) m / n * Math.log(2)));
	}

	static long murmurHash64(byte[] data) {
		final long seed = 0x9747b28cL;
		final long m = 0xc6a4a7935bd1e995L;
		final int r = 47;
		long h = seed ^ (data.length * m);
		int length8 = data.length / 8;
		for (int i = 0; i < length8; i++) {
			int i8 = i * 8;
			long k = ((long) data[i8] & 0xff) + (((long) data[i8 + 1] & 0xff) << 8)
					+ (((long) data[i8 + 2] & 0xff) << 16) + (((long) data[i8 + 3] & 0xff) << 24)
					+ (((long) data[i8 + 4] & 0xff) << 32) + (((long) data[i8 + 5] & 0xff) << 40)
					+ (((long) data[i8 + 6] & 0xff) << 48) + (((long) data[i8 + 7] & 0xff) << 56);
			k *= m;
			k ^= k >>> r;
			k *= m;
			h ^= k;
			h *= m;
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
				h *= m;
			default:
				break;
		}
		h ^= h >>> r;
		h *= m;
		h ^= h >>> r;
		return h;
	}

}

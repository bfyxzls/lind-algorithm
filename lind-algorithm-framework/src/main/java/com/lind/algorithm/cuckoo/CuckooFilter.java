package com.lind.algorithm.cuckoo;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Cuckoo Filter：支持删除的概率型集合，可能误判存在，不会漏判「一定不存在」。
 */
public final class CuckooFilter {

	private static final int BUCKET_SIZE = 4;

	private static final int MAX_KICKS = 500;

	private final int bucketCount;

	private final int fingerprintBits;

	private final int fingerprintMask;

	private final short[][] buckets;

	private long size;

	/**
	 * @param capacity 期望元素量
	 * @param fpp 目标假阳性率 (0,1)
	 */
	public CuckooFilter(long capacity, double fpp) {
		if (capacity <= 0) {
			throw new IllegalArgumentException("capacity must be > 0");
		}
		if (fpp <= 0 || fpp >= 1) {
			throw new IllegalArgumentException("fpp must be in (0,1)");
		}
		this.fingerprintBits = Math.max(4, (int) Math.ceil(Math.log(1.0 / fpp) / Math.log(2)) + 3);
		if (fingerprintBits > 16) {
			throw new IllegalArgumentException("fpp too small for 16-bit fingerprint");
		}
		this.fingerprintMask = (1 << fingerprintBits) - 1;
		int bucketsNeeded = (int) Math.max(2, Math.ceil(capacity * 1.05 / BUCKET_SIZE));
		this.bucketCount = nextPowerOfTwo(bucketsNeeded);
		this.buckets = new short[bucketCount][BUCKET_SIZE];
	}

	public boolean put(String value) {
		Objects.requireNonNull(value, "value");
		return put(value.getBytes(StandardCharsets.UTF_8));
	}

	public boolean put(byte[] value) {
		Objects.requireNonNull(value, "value");
		long hash = murmurHash64(value);
		int fp = fingerprint(hash);
		int i1 = indexHash(hash);
		int i2 = altIndex(i1, fp);
		if (insertInto(i1, fp) || insertInto(i2, fp)) {
			size++;
			return true;
		}
		int i = (hash & 1) == 0 ? i1 : i2;
		for (int n = 0; n < MAX_KICKS; n++) {
			int slot = (int) (Math.floorMod(hash + n, BUCKET_SIZE));
			int old = buckets[i][slot] & 0xffff;
			buckets[i][slot] = (short) fp;
			fp = old;
			i = altIndex(i, fp);
			if (insertInto(i, fp)) {
				size++;
				return true;
			}
		}
		return false;
	}

	public boolean mightContain(String value) {
		Objects.requireNonNull(value, "value");
		return mightContain(value.getBytes(StandardCharsets.UTF_8));
	}

	public boolean mightContain(byte[] value) {
		Objects.requireNonNull(value, "value");
		long hash = murmurHash64(value);
		int fp = fingerprint(hash);
		int i1 = indexHash(hash);
		int i2 = altIndex(i1, fp);
		return containsIn(i1, fp) || containsIn(i2, fp);
	}

	/**
	 * @return 删除成功为 true；未找到为 false
	 */
	public boolean delete(String value) {
		Objects.requireNonNull(value, "value");
		return delete(value.getBytes(StandardCharsets.UTF_8));
	}

	public boolean delete(byte[] value) {
		Objects.requireNonNull(value, "value");
		long hash = murmurHash64(value);
		int fp = fingerprint(hash);
		int i1 = indexHash(hash);
		int i2 = altIndex(i1, fp);
		if (removeFrom(i1, fp) || removeFrom(i2, fp)) {
			size--;
			return true;
		}
		return false;
	}

	public long size() {
		return size;
	}

	public int bucketCount() {
		return bucketCount;
	}

	private boolean insertInto(int bucket, int fp) {
		for (int i = 0; i < BUCKET_SIZE; i++) {
			if (buckets[bucket][i] == 0) {
				buckets[bucket][i] = (short) fp;
				return true;
			}
		}
		return false;
	}

	private boolean containsIn(int bucket, int fp) {
		for (int i = 0; i < BUCKET_SIZE; i++) {
			if ((buckets[bucket][i] & 0xffff) == fp) {
				return true;
			}
		}
		return false;
	}

	private boolean removeFrom(int bucket, int fp) {
		for (int i = 0; i < BUCKET_SIZE; i++) {
			if ((buckets[bucket][i] & 0xffff) == fp) {
				buckets[bucket][i] = 0;
				return true;
			}
		}
		return false;
	}

	private int fingerprint(long hash) {
		int fp = (int) (hash & fingerprintMask);
		return fp == 0 ? 1 : fp;
	}

	private int indexHash(long hash) {
		return (int) ((hash >>> 32) & (bucketCount - 1));
	}

	private int altIndex(int index, int fingerprint) {
		return (index ^ (fingerprint * 0x5bd1e995)) & (bucketCount - 1);
	}

	private static int nextPowerOfTwo(int n) {
		int highest = Integer.highestOneBit(n);
		return highest == n ? n : highest << 1;
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

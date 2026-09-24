package com.lind.algorithm.similarity;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * SimHash：文本指纹与汉明距离，适合近重复内容粗判。
 * <p>
 * 分词：空白切分 + 字符 bigram（兼顾中文短文本）。可供 {@code bloom.recommend} 内容指纹使用。
 */
public final class SimHash {

	private SimHash() {
	}

	public static long hash64(CharSequence text) {
		Objects.requireNonNull(text, "text");
		int[] bits = new int[64];
		String s = text.toString().trim();
		if (s.isEmpty()) {
			return 0L;
		}
		for (String token : s.split("\\s+")) {
			if (!token.isEmpty()) {
				accumulate(bits, token);
			}
		}
		if (s.codePointCount(0, s.length()) >= 2) {
			s.codePoints().reduce((prev, curr) -> {
				String bigram = new String(Character.toChars(prev)) + new String(Character.toChars(curr));
				accumulate(bits, bigram);
				return curr;
			});
		}
		long fingerprint = 0L;
		for (int i = 0; i < 64; i++) {
			if (bits[i] > 0) {
				fingerprint |= 1L << i;
			}
		}
		return fingerprint;
	}

	public static int hammingDistance(long a, long b) {
		return Long.bitCount(a ^ b);
	}

	public static boolean similar(CharSequence a, CharSequence b, int maxDistance) {
		if (maxDistance < 0) {
			throw new IllegalArgumentException("maxDistance must be >= 0");
		}
		return hammingDistance(hash64(a), hash64(b)) <= maxDistance;
	}

	private static void accumulate(int[] bits, String token) {
		long h = murmurHash64(token.getBytes(StandardCharsets.UTF_8));
		for (int i = 0; i < 64; i++) {
			if (((h >>> i) & 1L) == 1L) {
				bits[i]++;
			}
			else {
				bits[i]--;
			}
		}
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

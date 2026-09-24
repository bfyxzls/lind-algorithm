package com.lind.algorithm.codec;

import java.util.Arrays;
import java.util.Objects;

/**
 * Base62 编解码：短链 ID、紧凑可读编码。字母表 {@code 0-9A-Za-z}。
 */
public final class Base62 {

	private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
			.toCharArray();

	private static final int[] DECODE = new int[128];

	static {
		Arrays.fill(DECODE, -1);
		for (int i = 0; i < ALPHABET.length; i++) {
			DECODE[ALPHABET[i]] = i;
		}
	}

	private Base62() {
	}

	public static String encode(long value) {
		if (value < 0) {
			throw new IllegalArgumentException("value must be >= 0");
		}
		if (value == 0) {
			return "0";
		}
		StringBuilder sb = new StringBuilder();
		long n = value;
		while (n > 0) {
			sb.append(ALPHABET[(int) (n % 62)]);
			n /= 62;
		}
		return sb.reverse().toString();
	}

	public static long decode(String encoded) {
		Objects.requireNonNull(encoded, "encoded");
		if (encoded.isEmpty()) {
			throw new IllegalArgumentException("encoded must not be empty");
		}
		long result = 0;
		for (int i = 0; i < encoded.length(); i++) {
			char c = encoded.charAt(i);
			if (c >= DECODE.length || DECODE[c] < 0) {
				throw new IllegalArgumentException("invalid base62 char: " + c);
			}
			result = Math.multiplyExact(result, 62);
			result = Math.addExact(result, DECODE[c]);
		}
		return result;
	}

	public static String encode(byte[] bytes) {
		Objects.requireNonNull(bytes, "bytes");
		if (bytes.length == 0) {
			return "";
		}
		// treat as big-endian unsigned big integer via repeated division
		int[] digits = new int[bytes.length];
		for (int i = 0; i < bytes.length; i++) {
			digits[i] = bytes[i] & 0xff;
		}
		StringBuilder sb = new StringBuilder();
		int[] current = digits;
		while (!isZero(current)) {
			int[] quotRem = divMod(current, 62);
			sb.append(ALPHABET[quotRem[quotRem.length - 1]]);
			current = Arrays.copyOf(quotRem, quotRem.length - 1);
		}
		// preserve leading zero bytes as leading '0'
		for (byte b : bytes) {
			if (b == 0) {
				sb.append('0');
			}
			else {
				break;
			}
		}
		return sb.reverse().toString();
	}

	public static byte[] decodeToBytes(String encoded) {
		Objects.requireNonNull(encoded, "encoded");
		if (encoded.isEmpty()) {
			return new byte[0];
		}
		int leadingZeros = 0;
		while (leadingZeros < encoded.length() && encoded.charAt(leadingZeros) == '0') {
			leadingZeros++;
		}
		int[] digits = new int[1];
		for (int i = 0; i < encoded.length(); i++) {
			char c = encoded.charAt(i);
			if (c >= DECODE.length || DECODE[c] < 0) {
				throw new IllegalArgumentException("invalid base62 char: " + c);
			}
			digits = mulAdd(digits, 62, DECODE[c]);
		}
		byte[] raw = new byte[digits.length];
		for (int i = 0; i < digits.length; i++) {
			raw[i] = (byte) digits[i];
		}
		// strip leading zero from big-endian unless all zero
		int start = 0;
		while (start < raw.length - 1 && raw[start] == 0) {
			start++;
		}
		byte[] withoutLeading = Arrays.copyOfRange(raw, start, raw.length);
		if (leadingZeros == 0) {
			return withoutLeading;
		}
		byte[] result = new byte[leadingZeros + withoutLeading.length];
		System.arraycopy(withoutLeading, 0, result, leadingZeros, withoutLeading.length);
		return result;
	}

	private static boolean isZero(int[] digits) {
		for (int d : digits) {
			if (d != 0) {
				return false;
			}
		}
		return true;
	}

	/** returns quotient digits + remainder as last element */
	private static int[] divMod(int[] digits, int divisor) {
		int[] quot = new int[digits.length];
		int rem = 0;
		for (int i = 0; i < digits.length; i++) {
			int cur = rem * 256 + digits[i];
			quot[i] = cur / divisor;
			rem = cur % divisor;
		}
		int start = 0;
		while (start < quot.length - 1 && quot[start] == 0) {
			start++;
		}
		int[] result = new int[quot.length - start + 1];
		System.arraycopy(quot, start, result, 0, quot.length - start);
		result[result.length - 1] = rem;
		return result;
	}

	private static int[] mulAdd(int[] digits, int mul, int add) {
		int[] result = new int[digits.length + 1];
		int carry = add;
		for (int i = digits.length - 1; i >= 0; i--) {
			int v = digits[i] * mul + carry;
			result[i + 1] = v & 0xff;
			carry = v >>> 8;
		}
		result[0] = carry;
		int start = 0;
		while (start < result.length - 1 && result[start] == 0) {
			start++;
		}
		return Arrays.copyOfRange(result, start, result.length);
	}

}

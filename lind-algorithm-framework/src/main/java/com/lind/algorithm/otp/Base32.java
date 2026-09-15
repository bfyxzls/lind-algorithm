package com.lind.algorithm.otp;

import java.util.Locale;
import java.util.Objects;

/**
 * Base32 编解码（RFC 4648），用于 OTP 密钥的常见文本表示。
 */
public final class Base32 {

	private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

	private Base32() {
	}

	public static String encode(byte[] data) {
		Objects.requireNonNull(data, "data");
		if (data.length == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder((data.length * 8 + 4) / 5);
		int buffer = 0;
		int bitsLeft = 0;
		for (byte b : data) {
			buffer = (buffer << 8) | (b & 0xFF);
			bitsLeft += 8;
			while (bitsLeft >= 5) {
				sb.append(ALPHABET.charAt((buffer >> (bitsLeft - 5)) & 0x1F));
				bitsLeft -= 5;
			}
		}
		if (bitsLeft > 0) {
			sb.append(ALPHABET.charAt((buffer << (5 - bitsLeft)) & 0x1F));
		}
		return sb.toString();
	}

	public static byte[] decode(String encoded) {
		Objects.requireNonNull(encoded, "encoded");
		String normalized = encoded.replace(" ", "").replace("-", "").toUpperCase(Locale.ROOT);
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException("Base32 secret must not be blank");
		}
		int end = normalized.length();
		while (end > 0 && normalized.charAt(end - 1) == '=') {
			end--;
		}
		int outLength = end * 5 / 8;
		byte[] result = new byte[outLength];
		int buffer = 0;
		int bitsLeft = 0;
		int index = 0;
		for (int i = 0; i < end; i++) {
			int val = ALPHABET.indexOf(normalized.charAt(i));
			if (val < 0) {
				throw new IllegalArgumentException("invalid Base32 character: " + normalized.charAt(i));
			}
			buffer = (buffer << 5) | val;
			bitsLeft += 5;
			if (bitsLeft >= 8) {
				result[index++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
				bitsLeft -= 8;
			}
		}
		return result;
	}

}

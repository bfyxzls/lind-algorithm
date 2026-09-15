package com.lind.algorithm.otp;

/**
 * OTP 使用的 HMAC 算法（RFC 4226 / RFC 6238）。
 */
public enum HmacAlgorithm {

	SHA1("HmacSHA1", 20),

	SHA256("HmacSHA256", 32),

	SHA512("HmacSHA512", 64);

	private final String macAlgorithm;

	private final int keyLengthBytes;

	HmacAlgorithm(String macAlgorithm, int keyLengthBytes) {
		this.macAlgorithm = macAlgorithm;
		this.keyLengthBytes = keyLengthBytes;
	}

	public String macAlgorithm() {
		return macAlgorithm;
	}

	public int keyLengthBytes() {
		return keyLengthBytes;
	}

}

package com.lind.algorithm.otp;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * HOTP（HMAC-Based One-Time Password，RFC 4226）：基于计数器的一次性密码。
 * <p>
 * 验证时支持滑动窗口：接受 {@code [counter - window, counter + window]} 内任一计数器生成的码。
 * </p>
 */
public final class Hotp {

	public static final int DEFAULT_DIGITS = 6;

	public static final int DEFAULT_WINDOW = 0;

	private final byte[] secret;

	private final int digits;

	private final HmacAlgorithm algorithm;

	private final int window;

	private final int modDivisor;

	public Hotp(byte[] secret) {
		this(secret, DEFAULT_DIGITS, HmacAlgorithm.SHA1, DEFAULT_WINDOW);
	}

	public Hotp(byte[] secret, int digits) {
		this(secret, digits, HmacAlgorithm.SHA1, DEFAULT_WINDOW);
	}

	public Hotp(byte[] secret, int digits, HmacAlgorithm algorithm, int window) {
		Objects.requireNonNull(secret, "secret");
		if (secret.length == 0) {
			throw new IllegalArgumentException("secret must not be empty");
		}
		if (digits < 6 || digits > 8) {
			throw new IllegalArgumentException("digits must be in [6, 8]");
		}
		Objects.requireNonNull(algorithm, "algorithm");
		if (window < 0) {
			throw new IllegalArgumentException("window must be >= 0");
		}
		this.secret = secret.clone();
		this.digits = digits;
		this.algorithm = algorithm;
		this.window = window;
		int mod = 1;
		for (int i = 0; i < digits; i++) {
			mod *= 10;
		}
		this.modDivisor = mod;
	}

	/**
	 * 从 Base32 编码密钥创建（Google Authenticator 常见格式，忽略空格与大小写）。
	 */
	public static Hotp fromBase32Secret(String base32Secret) {
		return fromBase32Secret(base32Secret, DEFAULT_DIGITS, HmacAlgorithm.SHA1, DEFAULT_WINDOW);
	}

	public static Hotp fromBase32Secret(String base32Secret, int digits, HmacAlgorithm algorithm, int window) {
		return new Hotp(Base32.decode(base32Secret), digits, algorithm, window);
	}

	public String generate(long counter) {
		if (counter < 0) {
			throw new IllegalArgumentException("counter must be >= 0");
		}
		return formatCode(truncate(hmacSha(counter)));
	}

	/**
	 * 在滑动窗口 {@code [expectedCounter - window, expectedCounter + window]} 内验证。
	 * @return 匹配到的计数器；未匹配返回 empty 语义用 -1
	 */
	public long verify(String code, long expectedCounter) {
		Objects.requireNonNull(code, "code");
		String normalized = code.trim();
		if (normalized.length() != digits || !normalized.chars().allMatch(Character::isDigit)) {
			return -1L;
		}
		long start = Math.max(0L, expectedCounter - window);
		long end = expectedCounter + window;
		for (long c = start; c <= end; c++) {
			if (constantTimeEquals(generate(c), normalized)) {
				return c;
			}
		}
		return -1L;
	}

	public boolean matches(String code, long expectedCounter) {
		return verify(code, expectedCounter) >= 0;
	}

	public int digits() {
		return digits;
	}

	public int window() {
		return window;
	}

	public HmacAlgorithm algorithm() {
		return algorithm;
	}

	private byte[] hmacSha(long counter) {
		byte[] message = ByteBuffer.allocate(8).putLong(counter).array();
		try {
			Mac mac = Mac.getInstance(algorithm.macAlgorithm());
			mac.init(new SecretKeySpec(secret, algorithm.macAlgorithm()));
			return mac.doFinal(message);
		}
		catch (NoSuchAlgorithmException | InvalidKeyException e) {
			throw new IllegalStateException("HMAC failure: " + algorithm.macAlgorithm(), e);
		}
	}

	private static int truncate(byte[] hash) {
		int offset = hash[hash.length - 1] & 0x0F;
		return ((hash[offset] & 0x7F) << 24) | ((hash[offset + 1] & 0xFF) << 16) | ((hash[offset + 2] & 0xFF) << 8)
				| (hash[offset + 3] & 0xFF);
	}

	private String formatCode(int binary) {
		int otp = binary % modDivisor;
		return String.format(Locale.ROOT, "%0" + digits + "d", otp);
	}

	static boolean constantTimeEquals(String a, String b) {
		if (a.length() != b.length()) {
			return false;
		}
		int result = 0;
		for (int i = 0; i < a.length(); i++) {
			result |= a.charAt(i) ^ b.charAt(i);
		}
		return result == 0;
	}

}

package com.lind.algorithm.otp;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * TOTP（Time-Based One-Time Password，RFC 6238）：将时间步作为 HOTP 计数器。
 * <p>
 * 验证采用<strong>滑动窗口</strong>：当前时间步为 {@code T} 时，接受
 * {@code [T - window, T + window]} 内任一时间步生成的验证码。这样用户在时钟略有偏差、
 * 或刚好跨过步长边界时，仍可在窗口内使用有效码。
 * </p>
 * 默认：30 秒步长、6 位数字、HMAC-SHA1、窗口 {@code 1}（即前后各 1 个时间步，共最多 3 个码有效）。
 */
public final class Totp {

	public static final int DEFAULT_DIGITS = 6;

	public static final long DEFAULT_PERIOD_SECONDS = 30L;

	/** 默认滑动窗口：当前步 ±1。 */
	public static final int DEFAULT_WINDOW = 1;

	private final Hotp hotp;

	private final long periodMillis;

	private final LongSupplier clockMillis;

	public Totp(byte[] secret) {
		this(secret, DEFAULT_DIGITS, DEFAULT_PERIOD_SECONDS, HmacAlgorithm.SHA1, DEFAULT_WINDOW, System::currentTimeMillis);
	}

	public Totp(byte[] secret, int digits, long periodSeconds, int window) {
		this(secret, digits, periodSeconds, HmacAlgorithm.SHA1, window, System::currentTimeMillis);
	}

	public Totp(byte[] secret, int digits, long periodSeconds, HmacAlgorithm algorithm, int window) {
		this(secret, digits, periodSeconds, algorithm, window, System::currentTimeMillis);
	}

	Totp(byte[] secret, int digits, long periodSeconds, HmacAlgorithm algorithm, int window, LongSupplier clockMillis) {
		if (periodSeconds <= 0) {
			throw new IllegalArgumentException("periodSeconds must be > 0");
		}
		Objects.requireNonNull(clockMillis, "clockMillis");
		this.hotp = new Hotp(secret, digits, algorithm, window);
		this.periodMillis = TimeUnit.SECONDS.toMillis(periodSeconds);
		this.clockMillis = clockMillis;
	}

	public static Totp fromBase32Secret(String base32Secret) {
		return fromBase32Secret(base32Secret, DEFAULT_DIGITS, DEFAULT_PERIOD_SECONDS, HmacAlgorithm.SHA1,
				DEFAULT_WINDOW);
	}

	public static Totp fromBase32Secret(String base32Secret, int digits, long periodSeconds, HmacAlgorithm algorithm,
			int window) {
		return new Totp(Base32.decode(base32Secret), digits, periodSeconds, algorithm, window);
	}

	/**
	 * 生成当前时间步对应的 TOTP 码。
	 */
	public String now() {
		return generateAt(clockMillis.getAsLong());
	}

	/**
	 * 生成指定毫秒时间戳对应的 TOTP 码。
	 */
	public String generateAt(long epochMillis) {
		return hotp.generate(timeStep(epochMillis));
	}

	/**
	 * 滑动窗口验证：码在 {@code [当前步 - window, 当前步 + window]} 内任一时间步匹配即有效。
	 */
	public boolean verify(String code) {
		return verifyAt(code, clockMillis.getAsLong()) >= 0;
	}

	/**
	 * 在指定时刻做滑动窗口验证。
	 * @return 匹配到的时间步；未匹配返回 -1
	 */
	public long verifyAt(String code, long epochMillis) {
		return hotp.verify(code, timeStep(epochMillis));
	}

	public boolean matches(String code) {
		return verify(code);
	}

	public long timeStep(long epochMillis) {
		if (epochMillis < 0) {
			throw new IllegalArgumentException("epochMillis must be >= 0");
		}
		return epochMillis / periodMillis;
	}

	/**
	 * 当前时间步剩余有效毫秒数（到下一步开始）。
	 */
	public long remainingMillis() {
		long now = clockMillis.getAsLong();
		return periodMillis - (now % periodMillis);
	}

	public int digits() {
		return hotp.digits();
	}

	public int window() {
		return hotp.window();
	}

	public long periodSeconds() {
		return TimeUnit.MILLISECONDS.toSeconds(periodMillis);
	}

	public HmacAlgorithm algorithm() {
		return hotp.algorithm();
	}

	/**
	 * 生成 otpauth URI（可给 Google Authenticator 等扫码）。
	 */
	public String otpAuthUri(String issuer, String account, String base32Secret) {
		Objects.requireNonNull(issuer, "issuer");
		Objects.requireNonNull(account, "account");
		Objects.requireNonNull(base32Secret, "base32Secret");
		String label = urlEncode(issuer) + ":" + urlEncode(account);
		return "otpauth://totp/" + label + "?secret=" + base32Secret.replace(" ", "").toUpperCase()
				+ "&issuer=" + urlEncode(issuer) + "&algorithm=" + algorithm().name() + "&digits=" + digits()
				+ "&period=" + periodSeconds();
	}

	private static String urlEncode(String value) {
		StringBuilder sb = new StringBuilder();
		for (char c : value.toCharArray()) {
			if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == '.'
					|| c == '_' || c == '~') {
				sb.append(c);
			}
			else {
				sb.append('%');
				sb.append(String.format("%02X", (int) c));
			}
		}
		return sb.toString();
	}

}

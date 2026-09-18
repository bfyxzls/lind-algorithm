package com.lind.algorithm.otp;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * 面向「服务 A → 服务 B」的 TOTP 鉴权器（服务端）。
 * <p>
 * 双方预共享密钥，但链路上只传短时有效的 TOTP，不传明文密钥。校验成功后记录已使用的时间步， 防止同一码在滑动窗口内被重放。
 * </p>
 */
public final class TotpServiceAuthenticator {

	private final Totp totp;

	private final LongSupplier clockMillis;

	/** clientId → 最近一次成功使用的时间步（防重放）。 */
	private final Map<String, Long> usedSteps = new ConcurrentHashMap<>();

	public TotpServiceAuthenticator(byte[] sharedSecret) {
		this(sharedSecret, Totp.DEFAULT_DIGITS, Totp.DEFAULT_PERIOD_SECONDS, HmacAlgorithm.SHA1, Totp.DEFAULT_WINDOW,
				System::currentTimeMillis);
	}

	public TotpServiceAuthenticator(byte[] sharedSecret, int digits, long periodSeconds, HmacAlgorithm algorithm,
			int window) {
		this(sharedSecret, digits, periodSeconds, algorithm, window, System::currentTimeMillis);
	}

	TotpServiceAuthenticator(byte[] sharedSecret, int digits, long periodSeconds, HmacAlgorithm algorithm, int window,
			LongSupplier clockMillis) {
		Objects.requireNonNull(clockMillis, "clockMillis");
		this.totp = new Totp(sharedSecret, digits, periodSeconds, algorithm, window, clockMillis);
		this.clockMillis = clockMillis;
	}

	/**
	 * 服务端校验来自客户端的 TOTP。
	 * @param clientId 调用方标识（用于防重放隔离）
	 * @param code 请求头中的一次性码
	 */
	public boolean authenticate(String clientId, String code) {
		Objects.requireNonNull(clientId, "clientId");
		long matchedStep = totp.verifyAt(code, clockMillis.getAsLong());
		if (matchedStep < 0) {
			return false;
		}
		Long previous = usedSteps.put(clientId, matchedStep);
		// 同一客户端、同一时间步的码只允许成功一次
		if (previous != null && previous == matchedStep) {
			return false;
		}
		// 不允许时间步回退重放更旧的码
		if (previous != null && matchedStep < previous) {
			usedSteps.put(clientId, previous);
			return false;
		}
		return true;
	}

	public Totp totp() {
		return totp;
	}

}

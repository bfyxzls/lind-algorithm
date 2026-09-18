package com.lind.algorithm.otp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 模拟服务 A 调用服务 B：用 TOTP 代替每次传递明文共享密钥。
 */
class TotpServerToServerTest {

	private static final byte[] SHARED_SECRET = "s2s-shared-secret-demo!!".getBytes(StandardCharsets.UTF_8);

	private AtomicLong clock;

	private Totp clientA;

	private ServiceB serviceB;

	@BeforeEach
	void setUp() {
		clock = new AtomicLong(1_700_000_000_000L);
		// A / B 使用同一密钥与时钟（生产中靠 NTP 对齐）
		clientA = new Totp(SHARED_SECRET, 8, 30, HmacAlgorithm.SHA256, 1, clock::get);
		serviceB = new ServiceB(
				new TotpServiceAuthenticator(SHARED_SECRET, 8, 30, HmacAlgorithm.SHA256, 1, clock::get));
	}

	@Test
	void serviceACallsBWithoutSendingSharedSecret() {
		// A：只把一次性码放进请求头，密钥留在本地
		Map<String, String> headers = new LinkedHashMap<>();
		headers.put("X-Client-Id", "service-a");
		headers.put("X-Totp", clientA.now());

		ServiceB.Response response = serviceB.handle("GET", "/api/orders", headers, null);

		assertEquals(200, response.status());
		assertEquals("ok", response.body());
		// 链路上不应出现共享密钥
		assertFalse(headers.containsValue(new String(SHARED_SECRET, StandardCharsets.UTF_8)));
		assertFalse(String.join(",", headers.values()).contains("s2s-shared-secret"));
	}

	@Test
	void expiredOrWrongCodeIsRejected() {
		String code = clientA.now();
		clock.addAndGet(120_000L); // 超出 window=1（约 ±30s）

		ServiceB.Response response = serviceB.handle("GET", "/api/orders",
				Map.of("X-Client-Id", "service-a", "X-Totp", code), null);

		assertEquals(401, response.status());
	}

	@Test
	void replaySameCodeInWindowIsRejected() {
		String code = clientA.now();
		Map<String, String> headers = Map.of("X-Client-Id", "service-a", "X-Totp", code);

		assertEquals(200, serviceB.handle("GET", "/api/orders", headers, null).status());
		// 中间人截获后重放同一码 → 防重放拒绝
		assertEquals(401, serviceB.handle("GET", "/api/orders", headers, null).status());
	}

	@Test
	void betterThanStaticApiKeyOnTheWire() {
		// 旧方式：每次把静态密钥放进 Header（泄露一次即可无限调用）
		String staticKey = new String(SHARED_SECRET, StandardCharsets.UTF_8);
		assertTrue(staticKey.equals(staticKey)); // 静态密钥可被反复使用

		// 新方式：同一时刻码有效，跨出窗口后旧码失效
		String totpNow = clientA.now();
		assertTrue(serviceB.authenticator().authenticate("service-a", totpNow));

		clock.addAndGet(120_000L);
		assertFalse(serviceB.authenticator().authenticate("service-a", totpNow));
	}

	@Test
	void missingHeadersRejected() {
		assertThrows(IllegalArgumentException.class, () -> serviceB.handle("GET", "/api/orders", Map.of(), null));
	}

	/**
	 * 模拟服务 B：校验 TOTP 后处理业务。
	 */
	static final class ServiceB {

		private final TotpServiceAuthenticator authenticator;

		ServiceB(TotpServiceAuthenticator authenticator) {
			this.authenticator = Objects.requireNonNull(authenticator);
		}

		TotpServiceAuthenticator authenticator() {
			return authenticator;
		}

		Response handle(String method, String path, Map<String, String> headers, String body) {
			Objects.requireNonNull(headers, "headers");
			String clientId = headers.get("X-Client-Id");
			String totp = headers.get("X-Totp");
			if (clientId == null || clientId.isBlank() || totp == null || totp.isBlank()) {
				throw new IllegalArgumentException("X-Client-Id and X-Totp are required");
			}
			if (!authenticator.authenticate(clientId, totp)) {
				return new Response(401, "unauthorized");
			}
			return new Response(200, "ok");
		}

		record Response(int status, String body) {
		}

	}

}

package com.lind.algorithm.otp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/**
 * HOTP / TOTP 单元测试（含 RFC 6238 测试向量）。
 */
class OtpTest {

	/**
	 * RFC 6238 Appendix B seed for SHA1.
	 */
	private static final byte[] SEED_SHA1 = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);

	/**
	 * RFC 6238 Appendix B seed for SHA256.
	 */
	private static final byte[] SEED_SHA256 = "12345678901234567890123456789012".getBytes(StandardCharsets.US_ASCII);

	/**
	 * RFC 6238 Appendix B seed for SHA512.
	 */
	private static final byte[] SEED_SHA512 = "1234567890123456789012345678901234567890123456789012345678901234"
			.getBytes(StandardCharsets.US_ASCII);

	@Test
	void hotpRfc4226AppendixD() {
		// RFC 4226 Appendix D (6 digits, SHA1)
		Hotp hotp = new Hotp(SEED_SHA1, 6);
		assertEquals("755224", hotp.generate(0));
		assertEquals("287082", hotp.generate(1));
		assertEquals("359152", hotp.generate(2));
		assertEquals("969429", hotp.generate(3));
		assertEquals("338314", hotp.generate(4));
		assertEquals("254676", hotp.generate(5));
		assertEquals("287922", hotp.generate(6));
		assertEquals("162583", hotp.generate(7));
		assertEquals("399871", hotp.generate(8));
		assertEquals("520489", hotp.generate(9));
	}

	@Test
	void hotpSlidingWindowAcceptsNeighborCounters() {
		Hotp hotp = new Hotp(SEED_SHA1, 6, HmacAlgorithm.SHA1, 1);
		String codeAt5 = hotp.generate(5);
		assertEquals(5, hotp.verify(codeAt5, 5));
		assertEquals(5, hotp.verify(codeAt5, 4)); // expected 4, window allows 5
		assertEquals(5, hotp.verify(codeAt5, 6));
		assertEquals(-1, hotp.verify(codeAt5, 7)); // outside window
	}

	@Test
	void totpRfc6238Sha1EightDigits() {
		Totp totp = new Totp(SEED_SHA1, 8, 30, HmacAlgorithm.SHA1, 0);
		assertEquals("94287082", totp.generateAt(59_000L));
		assertEquals("07081804", totp.generateAt(1_111_111_109_000L));
		assertEquals("14050471", totp.generateAt(1_111_111_111_000L));
		assertEquals("89005924", totp.generateAt(1_234_567_890_000L));
		assertEquals("69279037", totp.generateAt(2_000_000_000_000L));
		assertEquals("65353130", totp.generateAt(20_000_000_000_000L));
	}

	@Test
	void totpRfc6238Sha256AndSha512() {
		Totp sha256 = new Totp(SEED_SHA256, 8, 30, HmacAlgorithm.SHA256, 0);
		assertEquals("46119246", sha256.generateAt(59_000L));
		assertEquals("68084774", sha256.generateAt(1_111_111_109_000L));
		assertEquals("67062674", sha256.generateAt(1_111_111_111_000L));

		Totp sha512 = new Totp(SEED_SHA512, 8, 30, HmacAlgorithm.SHA512, 0);
		assertEquals("90693936", sha512.generateAt(59_000L));
		assertEquals("25091201", sha512.generateAt(1_111_111_109_000L));
		assertEquals("99943326", sha512.generateAt(1_111_111_111_000L));
	}

	@Test
	void totpSlidingWindowMakesPreviousStepCodeStillValid() {
		AtomicLong clock = new AtomicLong(0);
		// period=30s, window=1 → codes from steps T-1,T,T+1 are valid
		Totp totp = new Totp(SEED_SHA1, 6, 30, HmacAlgorithm.SHA1, 1, clock::get);

		clock.set(45_000L); // step 1
		String codeStep1 = totp.now();
		assertTrue(totp.verify(codeStep1));

		// move into step 2; previous step code still valid because window=1
		clock.set(65_000L); // step 2
		assertTrue(totp.verify(codeStep1));
		assertEquals(1, totp.verifyAt(codeStep1, 65_000L));

		// jump far ahead → outside window
		clock.set(200_000L); // step 6
		assertFalse(totp.verify(codeStep1));
	}

	@Test
	void totpWindowZeroOnlyAcceptsExactStep() {
		AtomicLong clock = new AtomicLong(45_000L);
		Totp totp = new Totp(SEED_SHA1, 6, 30, HmacAlgorithm.SHA1, 0, clock::get);
		String code = totp.now();
		assertTrue(totp.verify(code));
		clock.set(65_000L);
		assertFalse(totp.verify(code));

		clock.set(30_000L); // 45_000-15_000
		assertTrue(totp.verify(code));

		clock.set(59_999L); // //45_000+14_999
		assertTrue(totp.verify(code));
	}

	@Test
	void totpWindow1AcceptsAdjacentSteps() {
		AtomicLong clock = new AtomicLong(45_000L); // step 1
		Totp totp = new Totp(SEED_SHA1, 6, 30, HmacAlgorithm.SHA1, 1, clock::get);
		String codeStep1 = totp.now();

		assertTrue(totp.verify(codeStep1));

		clock.set(20_000L); // step 0，窗口内 <(45000)
		assertTrue(totp.verify(codeStep1));

		clock.set(70_000L); // step 2，窗口内 <(45000+45000)
		assertTrue(totp.verify(codeStep1));

		clock.set(89_000L); // step 2，窗口内 <(45000+45000)
		assertTrue(totp.verify(codeStep1));

		clock.set(90_000L); // step 3，超出 ±1 >=(45000+45000)
		assertFalse(totp.verify(codeStep1));
	}

	@Test
	void configurableDigits() {
		Totp six = new Totp(SEED_SHA1, 6, 30, 0);
		Totp eight = new Totp(SEED_SHA1, 8, 30, HmacAlgorithm.SHA1, 0);
		assertEquals(6, six.generateAt(59_000L).length());
		assertEquals(8, eight.generateAt(59_000L).length());
		assertEquals("94287082".substring(2), six.generateAt(59_000L)); // last 6 of
																		// 8-digit vector
	}

	@Test
	void base32RoundTripAndFactory() {
		byte[] raw = SEED_SHA1;
		String encoded = Base32.encode(raw);
		assertEquals(new String(raw, StandardCharsets.US_ASCII),
				new String(Base32.decode(encoded), StandardCharsets.US_ASCII));

		Totp totp = Totp.fromBase32Secret(encoded, 8, 30, HmacAlgorithm.SHA1, 0);
		assertEquals("94287082", totp.generateAt(59_000L));
	}

	@Test
	void rejectInvalidConfig() {
		assertThrows(IllegalArgumentException.class, () -> new Totp(SEED_SHA1, 5, 30, 1));
		assertThrows(IllegalArgumentException.class, () -> new Totp(SEED_SHA1, 6, 0, 1));
		assertThrows(IllegalArgumentException.class, () -> new Hotp(SEED_SHA1, 6, HmacAlgorithm.SHA1, -1));
		assertThrows(IllegalArgumentException.class, () -> Base32.decode("@@@"));
	}

	@Test
	void otpAuthUriContainsParams() {
		String secret = Base32.encode(SEED_SHA1);
		Totp totp = Totp.fromBase32Secret(secret);
		String uri = totp.otpAuthUri("Lind", "user@example.com", secret);
		assertTrue(uri.startsWith("otpauth://totp/"));
		assertTrue(uri.contains("digits=6"));
		assertTrue(uri.contains("period=30"));
		assertTrue(uri.contains("algorithm=SHA1"));
	}

}

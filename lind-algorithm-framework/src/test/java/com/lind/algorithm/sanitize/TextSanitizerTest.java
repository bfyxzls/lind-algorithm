package com.lind.algorithm.sanitize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * 文本清洗场景测试。
 */
public class TextSanitizerTest {

	@Test
	void nullPassthrough() {
		assertNull(TextSanitizers.html().sanitize(null));
	}

	@Test
	void htmlRemovesTagsAndScript() {
		String raw = "<div>hello<script>alert(1)</script><b>world</b>&nbsp;ok</div>";
		String clean = TextSanitizers.html().sanitize(raw);
		assertEquals("helloworld ok", clean);
		assertFalse(clean.contains("<"));
		assertFalse(clean.contains("alert"));
	}

	@Test
	void phoneRemovesMobileAndLandline() {
		String raw = "联系我 138-0013-8000 或 010-12345678 谢谢";
		String clean = TextSanitizers.phone().sanitize(raw);
		assertFalse(clean.contains("138"));
		assertFalse(clean.contains("010"));
		assertTrue(clean.contains("联系我"));
		assertTrue(clean.contains("谢谢"));
	}

	@Test
	void phoneCanMask() {
		String clean = new PhoneNumberSanitizer("****").sanitize("手机13800138000");
		assertTrue(clean.contains("****"));
		assertFalse(clean.contains("13800138000"));
	}

	@Test
	void emailRemovesAddress() {
		String raw = "写邮件到 user.name+tag@example.com 即可";
		String clean = TextSanitizers.email().sanitize(raw);
		assertFalse(clean.toLowerCase().contains("@example.com"));
		assertTrue(clean.contains("写邮件到"));
	}

	@Test
	void userAgentExtractsBareQoderWithoutSlash() {
		String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)  "
				+ "Qoder Chrome/150.0.7871.114 Electron/43.1.1 Safari/537.36";
		assertEquals("Qoder", TextSanitizers.userAgent().sanitize(ua));
	}

	@Test
	void userAgentExtractsQoderWithVersion() {
		String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
				+ "Qoder/0.4.4 Chrome/150.0.7871.114 Electron/43.1.1 Safari/537.36";
		assertEquals("Qoder", TextSanitizers.userAgent().sanitize(ua));
		assertEquals("Qoder/0.4.4", new UserAgentSanitizer(true).sanitize(ua));
	}

	@Test
	void userAgentKeepsMultipleBusinessProducts() {
		String ua = "Mozilla/5.0 AppleWebKit/537.36 MyApp/3.2.1 Chrome/120 BizChannel/huawei Safari/537.36";
		assertEquals("MyApp BizChannel", TextSanitizers.userAgent().sanitize(ua));
	}

	@Test
	void userAgentPureBrowserYieldsEmpty() {
		String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36";
		assertEquals("", TextSanitizers.userAgent().sanitize(ua));
	}

	@Test
	void userAgentSimpleClientName() {
		assertEquals("curl", TextSanitizers.userAgent().sanitize("curl/7.68.0"));
		assertEquals("PostmanRuntime", TextSanitizers.userAgent().sanitize("PostmanRuntime/7.29.0"));
	}

	@Test
	void whitespaceAndInvisible() {
		String raw = "a\u200B  \tb\n\nc";
		String clean = new CompositeTextSanitizer(TextSanitizers.invisibleChars(), TextSanitizers.whitespace())
				.sanitize(raw);
		assertEquals("a b c", clean);
	}

	@Test
	void idCardAndBankCardMask() {
		String id = "11010519491231002X";
		String card = "6222021234567890123";
		String raw = "身份证" + id + " 卡号" + card;
		String clean = new CompositeTextSanitizer(TextSanitizers.idCard(), TextSanitizers.bankCard()).sanitize(raw);
		assertFalse(clean.contains(id));
		assertFalse(clean.contains(card));
		assertTrue(clean.contains("1101"));
		assertTrue(clean.contains("6222"));
	}

	@Test
	void urlRemoved() {
		String raw = "详见 https://example.com/a?x=1 文档";
		assertEquals("详见  文档", TextSanitizers.url().sanitize(raw));
	}

	@Test
	void ugcAndPrivacyPipelines() {
		String ugc = TextSanitizers.ugcPipeline().sanitize("<p>hi</p> 打 13800138000 或 a@b.com 看 http://x.com");
		assertFalse(ugc.contains("<"));
		assertFalse(ugc.contains("13800138000"));
		assertFalse(ugc.contains("@"));
		assertFalse(ugc.contains("http"));

		String privacy = TextSanitizers.privacyPipeline().sanitize("手机13800138000 邮箱a@b.com");
		assertTrue(privacy.contains("****"));
	}

}

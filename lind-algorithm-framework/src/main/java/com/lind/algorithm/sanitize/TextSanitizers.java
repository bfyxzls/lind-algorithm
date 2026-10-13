package com.lind.algorithm.sanitize;

/**
 * 常用清洗组合工厂。
 */
public final class TextSanitizers {

	private TextSanitizers() {
	}

	public static TextSanitizer html() {
		return new HtmlTagSanitizer();
	}

	public static TextSanitizer phone() {
		return new PhoneNumberSanitizer();
	}

	public static TextSanitizer email() {
		return new EmailSanitizer();
	}

	public static TextSanitizer userAgent() {
		return new UserAgentSanitizer();
	}

	public static TextSanitizer whitespace() {
		return new WhitespaceSanitizer();
	}

	public static TextSanitizer invisibleChars() {
		return new InvisibleCharSanitizer();
	}

	public static TextSanitizer idCard() {
		return new IdCardSanitizer();
	}

	public static TextSanitizer bankCard() {
		return new BankCardSanitizer();
	}

	public static TextSanitizer url() {
		return new UrlSanitizer();
	}

	/**
	 * UGC / 评论入库：去 HTML、不可见字符、链接、电话、邮箱，再压空白。
	 */
	public static TextSanitizer ugcPipeline() {
		return new CompositeTextSanitizer("ugc", java.util.List.of(new HtmlTagSanitizer(), new InvisibleCharSanitizer(),
				new UrlSanitizer(), new PhoneNumberSanitizer(), new EmailSanitizer(), new WhitespaceSanitizer()));
	}

	/**
	 * 日志 / 工单脱敏：身份证、银行卡、电话、邮箱。
	 */
	public static TextSanitizer privacyPipeline() {
		return new CompositeTextSanitizer("privacy", java.util.List.of(new IdCardSanitizer(), new BankCardSanitizer(),
				new PhoneNumberSanitizer("****"), new EmailSanitizer("****")));
	}

}

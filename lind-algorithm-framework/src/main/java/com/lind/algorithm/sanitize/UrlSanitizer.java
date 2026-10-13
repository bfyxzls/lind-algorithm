package com.lind.algorithm.sanitize;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * URL 清洗：移除文本中的 http(s) 链接。
 */
public final class UrlSanitizer extends TextSanitizer {

	private static final Pattern URL = Pattern.compile("(?i)\\bhttps?://[^\\s<>\"']+");

	private final String replacement;

	public UrlSanitizer() {
		this("");
	}

	public UrlSanitizer(String replacement) {
		this.replacement = Objects.requireNonNull(replacement, "replacement");
	}

	@Override
	public String type() {
		return "url";
	}

	@Override
	protected String doSanitize(String input) {
		return URL.matcher(input).replaceAll(replacement);
	}

}

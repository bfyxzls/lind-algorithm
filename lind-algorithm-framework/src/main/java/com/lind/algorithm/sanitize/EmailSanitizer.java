package com.lind.algorithm.sanitize;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 电子邮箱清洗：移除或掩码文本中的邮箱地址。
 */
public final class EmailSanitizer extends TextSanitizer {

	private static final Pattern EMAIL = Pattern.compile("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b");

	private final String replacement;

	public EmailSanitizer() {
		this("");
	}

	public EmailSanitizer(String replacement) {
		this.replacement = Objects.requireNonNull(replacement, "replacement");
	}

	@Override
	public String type() {
		return "email";
	}

	@Override
	protected String doSanitize(String input) {
		return EMAIL.matcher(input).replaceAll(replacement);
	}

}

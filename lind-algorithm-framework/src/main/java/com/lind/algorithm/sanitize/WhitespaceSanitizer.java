package com.lind.algorithm.sanitize;

import java.util.regex.Pattern;

/**
 * 空白规范化：去首尾空白，连续空白（含换行、制表）压成单个空格。
 */
public final class WhitespaceSanitizer extends TextSanitizer {

	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	@Override
	public String type() {
		return "whitespace";
	}

	@Override
	protected String doSanitize(String input) {
		return WHITESPACE.matcher(input.trim()).replaceAll(" ");
	}

}

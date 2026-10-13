package com.lind.algorithm.sanitize;

import java.util.regex.Pattern;

/**
 * 不可见字符清洗：去掉 BOM、零宽字符、控制字符（保留常见空白 \\t \\n \\r）。
 */
public final class InvisibleCharSanitizer extends TextSanitizer {

	private static final Pattern INVISIBLE = Pattern.compile(
			"[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u007F\\uFEFF\\u200B-\\u200F\\u202A-\\u202E\\u2060\\u2066-\\u2069]");

	@Override
	public String type() {
		return "invisible-char";
	}

	@Override
	protected String doSanitize(String input) {
		return INVISIBLE.matcher(input).replaceAll("");
	}

}

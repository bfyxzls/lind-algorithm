package com.lind.algorithm.sanitize;

import java.util.regex.Pattern;

/**
 * HTML 标签清洗：移除标签，可选解码常见实体。
 */
public final class HtmlTagSanitizer extends TextSanitizer {

	private static final Pattern TAG = Pattern.compile("(?is)<[^>]*>");

	private static final Pattern SCRIPT_STYLE = Pattern.compile("(?is)<(script|style)[^>]*>.*?</\\1>");

	private final boolean decodeEntities;

	public HtmlTagSanitizer() {
		this(true);
	}

	public HtmlTagSanitizer(boolean decodeEntities) {
		this.decodeEntities = decodeEntities;
	}

	@Override
	public String type() {
		return "html-tag";
	}

	@Override
	protected String doSanitize(String input) {
		String text = SCRIPT_STYLE.matcher(input).replaceAll("");
		text = TAG.matcher(text).replaceAll("");
		if (decodeEntities) {
			text = decodeBasicEntities(text);
		}
		return text;
	}

	static String decodeBasicEntities(String text) {
		return text.replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
				.replace("&quot;", "\"").replace("&#39;", "'").replace("&apos;", "'");
	}

}

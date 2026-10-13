package com.lind.algorithm.sanitize;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 电话号码清洗：移除或掩码文本中的手机号 / 常见固话片段。
 */
public final class PhoneNumberSanitizer extends TextSanitizer {

	/** 大陆手机号，允许中间空格或横线。 */
	private static final Pattern MOBILE = Pattern.compile("(?<!\\d)(?:\\+?86[\\s-]*)?1[3-9]\\d([\\s-]*\\d){8}(?!\\d)");

	/** 固话：区号-号码 或 纯数字段。 */
	private static final Pattern LANDLINE = Pattern.compile("(?<!\\d)0\\d{2,3}[\\s-]?\\d{7,8}(?!\\d)");

	private final String replacement;

	public PhoneNumberSanitizer() {
		this("");
	}

	/**
	 * @param replacement 替换串，例如空串删除，或 {@code ****} 掩码
	 */
	public PhoneNumberSanitizer(String replacement) {
		this.replacement = Objects.requireNonNull(replacement, "replacement");
	}

	@Override
	public String type() {
		return "phone";
	}

	@Override
	protected String doSanitize(String input) {
		String text = MOBILE.matcher(input).replaceAll(replacement);
		return LANDLINE.matcher(text).replaceAll(replacement);
	}

}

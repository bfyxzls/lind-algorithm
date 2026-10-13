package com.lind.algorithm.sanitize;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 大陆身份证号清洗：默认掩码中间位（保留前 4 后 4），也可整段删除。
 */
public final class IdCardSanitizer extends TextSanitizer {

	private static final Pattern ID_CARD = Pattern
			.compile("(?<!\\d)[1-9]\\d{5}(?:19|20)\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx](?!\\d)");

	private final Mode mode;

	public IdCardSanitizer() {
		this(Mode.MASK);
	}

	public IdCardSanitizer(Mode mode) {
		this.mode = Objects.requireNonNull(mode, "mode");
	}

	@Override
	public String type() {
		return "id-card";
	}

	@Override
	protected String doSanitize(String input) {
		Matcher matcher = ID_CARD.matcher(input);
		StringBuffer sb = new StringBuffer();
		while (matcher.find()) {
			String id = matcher.group();
			String replacement = mode == Mode.REMOVE ? "" : mask(id);
			matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	private static String mask(String id) {
		if (id.length() < 8) {
			return "****";
		}
		return id.substring(0, 4) + "**********" + id.substring(id.length() - 4);
	}

	public enum Mode {

		MASK, REMOVE

	}

}

package com.lind.algorithm.sanitize;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 银行卡号清洗：识别 13～19 位连续数字卡号，默认掩码中间，也可删除。
 */
public final class BankCardSanitizer extends TextSanitizer {

	private static final Pattern CARD = Pattern.compile("(?<!\\d)(?:\\d[\\s-]?){12,18}\\d(?!\\d)");

	private final Mode mode;

	public BankCardSanitizer() {
		this(Mode.MASK);
	}

	public BankCardSanitizer(Mode mode) {
		this.mode = Objects.requireNonNull(mode, "mode");
	}

	@Override
	public String type() {
		return "bank-card";
	}

	@Override
	protected String doSanitize(String input) {
		Matcher matcher = CARD.matcher(input);
		StringBuffer sb = new StringBuffer();
		while (matcher.find()) {
			String raw = matcher.group();
			String digits = raw.replaceAll("[\\s-]", "");
			if (digits.length() < 13 || digits.length() > 19) {
				matcher.appendReplacement(sb, Matcher.quoteReplacement(raw));
				continue;
			}
			String replacement = mode == Mode.REMOVE ? "" : mask(digits);
			matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	private static String mask(String digits) {
		if (digits.length() < 8) {
			return "****";
		}
		return digits.substring(0, 4) + " **** **** " + digits.substring(digits.length() - 4);
	}

	public enum Mode {

		MASK, REMOVE

	}

}

package com.lind.algorithm.sanitize;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * User-Agent 清洗：去掉浏览器通用片段，保留业务自定义 UA。
 */
public final class UserAgentSanitizer extends TextSanitizer {

	private static final Set<String> DEFAULT_BROWSER_TOKENS = Set.of("mozilla", "applewebkit", "webkit", "chrome",
			"chromium", "safari", "firefox", "gecko", "khtml", "version", "mobile", "crios", "edg", "edge", "edga",
			"edgios", "opr", "opera", "electron", "msie", "trident", "dalvik");

	private static final Pattern SPLIT = Pattern.compile("[\\s;/()\\[\\]{},]+");

	private static final Pattern VERSION_LIKE = Pattern.compile("(?i)^[vV]?\\d+(?:\\.\\d+)*$");

	private final Set<String> browserTokens;

	private final boolean dropVersionTokens;

	public UserAgentSanitizer() {
		this(DEFAULT_BROWSER_TOKENS, true);
	}

	public UserAgentSanitizer(Set<String> browserTokens, boolean dropVersionTokens) {
		Objects.requireNonNull(browserTokens, "browserTokens");
		Set<String> normalized = new LinkedHashSet<>();
		for (String token : browserTokens) {
			if (token != null && !token.isBlank()) {
				normalized.add(token.toLowerCase(Locale.ROOT));
			}
		}
		this.browserTokens = Set.copyOf(normalized);
		this.dropVersionTokens = dropVersionTokens;
	}

	@Override
	public String type() {
		return "user-agent";
	}

	@Override
	protected String doSanitize(String input) {
		String[] parts = SPLIT.split(input.trim());
		List<String> kept = new ArrayList<>();
		for (String part : parts) {
			if (part == null || part.isBlank()) {
				continue;
			}
			String lower = part.toLowerCase(Locale.ROOT);
			if (isBrowserToken(lower)) {
				continue;
			}
			if (dropVersionTokens && VERSION_LIKE.matcher(part).matches()) {
				continue;
			}
			kept.add(part);
		}
		return String.join(" ", kept).trim();
	}

	private boolean isBrowserToken(String lower) {
		if (browserTokens.contains(lower)) {
			return true;
		}
		// chrome/120.0.0.0 → chrome
		int slash = lower.indexOf('/');
		if (slash > 0) {
			String name = lower.substring(0, slash);
			if (browserTokens.contains(name)) {
				return true;
			}
		}
		for (String token : browserTokens) {
			if (lower.startsWith(token)) {
				return true;
			}
		}
		return false;
	}

	public static Set<String> defaultBrowserTokens() {
		return DEFAULT_BROWSER_TOKENS;
	}

}

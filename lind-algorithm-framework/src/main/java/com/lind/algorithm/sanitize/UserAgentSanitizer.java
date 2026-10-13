package com.lind.algorithm.sanitize;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User-Agent 清洗：提取业务客户端名。
 * <ul>
 * <li>{@code Qoder/0.4.4} → {@code Qoder}</li>
 * <li>无斜杠的裸名 {@code Qoder} 同样保留</li>
 * <li>去掉浏览器 / 引擎 / Electron，以及 Windows、Win64、like 等系统噪声</li>
 * </ul>
 */
public final class UserAgentSanitizer extends TextSanitizer {

	/**
	 * 产品或裸名：{@code Name} 或 {@code Name/version}。
	 */
	private static final Pattern TOKEN = Pattern.compile("([A-Za-z][A-Za-z0-9._+-]*)(?:/([^\\s/;()\\[\\]{}]+))?");

	private static final Pattern VERSION_LIKE = Pattern.compile("(?i)^[vV]?\\d+(?:\\.\\d+)*$");

	private static final Set<String> DEFAULT_NOISE_PRODUCTS = Set.of(
			// 浏览器 / 引擎
			"mozilla", "applewebkit", "webkit", "chrome", "chromium", "safari", "firefox", "gecko", "khtml", "version",
			"mobile", "crios", "edg", "edge", "edga", "edgios", "opr", "opera", "electron", "msie", "trident", "dalvik",
			// 系统 / 平台噪声（括号内常见）
			"windows", "nt", "win64", "win32", "x64", "x86", "x86_64", "amd64", "arm", "arm64", "aarch64", "linux",
			"android", "ios", "iphone", "ipad", "mac", "macintosh", "os", "osx", "like", "compatible", "rv", "u",
			"wow64");

	private final Set<String> noiseProducts;

	/** true 时对 Name/version 保留版本号；裸名始终只输出 Name。 */
	private final boolean keepVersion;

	public UserAgentSanitizer() {
		this(DEFAULT_NOISE_PRODUCTS, false);
	}

	public UserAgentSanitizer(boolean keepVersion) {
		this(DEFAULT_NOISE_PRODUCTS, keepVersion);
	}

	public UserAgentSanitizer(Set<String> noiseProducts, boolean keepVersion) {
		Objects.requireNonNull(noiseProducts, "noiseProducts");
		Set<String> normalized = new LinkedHashSet<>();
		for (String token : noiseProducts) {
			if (token != null && !token.isBlank()) {
				normalized.add(token.toLowerCase(Locale.ROOT));
			}
		}
		this.noiseProducts = Set.copyOf(normalized);
		this.keepVersion = keepVersion;
	}

	@Override
	public String type() {
		return "user-agent";
	}

	@Override
	protected String doSanitize(String input) {
		List<String> business = new ArrayList<>();
		Set<String> seen = new LinkedHashSet<>();
		Matcher matcher = TOKEN.matcher(input);
		while (matcher.find()) {
			String name = matcher.group(1);
			String version = matcher.group(2);
			if (name == null || name.isEmpty() || isNoiseProduct(name)) {
				continue;
			}
			if (VERSION_LIKE.matcher(name).matches()) {
				continue;
			}
			String key = name.toLowerCase(Locale.ROOT);
			if (!seen.add(key)) {
				continue;
			}
			if (version != null && keepVersion) {
				business.add(name + "/" + version);
			}
			else {
				business.add(name);
			}
		}
		return String.join(" ", business);
	}

	private boolean isNoiseProduct(String product) {
		return noiseProducts.contains(product.toLowerCase(Locale.ROOT));
	}

	public static Set<String> defaultNoiseProducts() {
		return DEFAULT_NOISE_PRODUCTS;
	}

	/**
	 * @deprecated 使用 {@link #defaultNoiseProducts()}
	 */
	@Deprecated
	public static Set<String> defaultBrowserTokens() {
		return DEFAULT_NOISE_PRODUCTS;
	}

}

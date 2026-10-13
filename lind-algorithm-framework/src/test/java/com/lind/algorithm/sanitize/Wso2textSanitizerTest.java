package com.lind.algorithm.sanitize;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Wso2textSanitizerTest {

	/**
	 * UA 产品段：Name/version，如 {@code Qoder/0.4.4}、{@code WorkBuddy/5.8.1}。
	 */
	private static final Pattern UA_PRODUCT_TOKEN = Pattern.compile("([A-Za-z][A-Za-z0-9._+-]*)/([0-9][^\\s]*)");

	/**
	 * 浏览器 / 引擎等干扰项（大小写不敏感），归一化来源时跳过，优先保留业务客户端名。
	 */
	private static final Set<String> UA_NOISE_PRODUCTS;

	static {
		Set<String> noise = new HashSet<>();
		Collections.addAll(noise, "mozilla", "applewebkit", "webkit", "chrome", "chromium", "safari", "firefox",
				"gecko", "khtml", "version", "mobile", "crios", "edg", "edge", "edga", "edgios", "opr", "opera",
				"electron", "msie", "trident", "dalvik");
		UA_NOISE_PRODUCTS = Collections.unmodifiableSet(noise);
	}

	/**
	 * 将来源统计用的 User-Agent 清洗为单个业务标识（字符串，非数组）。
	 * <ul>
	 * <li>{@code WorkBuddy/5.8.1} → {@code WorkBuddy}</li>
	 * <li>浏览器壳 + {@code Qoder/0.4.4} + Chrome/Electron/Safari → {@code Qoder}</li>
	 * <li>{@code curl} / {@code curl/7.68.0} → {@code curl}</li>
	 * </ul>
	 * 跳过 Mozilla/AppleWebKit/Chrome/Safari/Electron 等干扰项；版本号丢弃。
	 */
	static String toUserAgentSourceLabel(Object userAgent) {
		if (userAgent == null) {
			return null;
		}
		String value = String.valueOf(userAgent).trim();
		if (value.isEmpty()) {
			return null;
		}

		List<String> products = new ArrayList<>();
		Matcher matcher = UA_PRODUCT_TOKEN.matcher(value);
		while (matcher.find()) {
			String name = matcher.group(1);
			if (name != null && !name.isEmpty()) {
				products.add(name);
			}
		}

		for (String product : products) {
			if (!isUaNoiseProduct(product)) {
				return product;
			}
		}

		// 无 Name/version，且整体较短、无空格：视为简单客户端名（如 curl、PostmanRuntime）
		if (products.isEmpty() && value.length() <= 64 && value.indexOf(' ') < 0 && value.indexOf('(') < 0) {
			int slash = value.indexOf('/');
			String simple = (slash < 0 ? value : value.substring(0, slash)).trim();
			return simple.isEmpty() ? null : simple;
		}

		// 仅剩浏览器壳：回退第一个产品名（通常是 Mozilla），避免丢字段
		if (!products.isEmpty()) {
			return products.get(0);
		}

		int slash = value.indexOf('/');
		String prefix = slash < 0 ? value : value.substring(0, slash).trim();
		return prefix.isEmpty() ? null : prefix;
	}

	private static boolean isUaNoiseProduct(String product) {
		return UA_NOISE_PRODUCTS.contains(product.toLowerCase(Locale.ROOT));
	}

	private static String toNonEmptyVersion(Object value) {
		if (value == null) {
			return null;
		}
		String text = String.valueOf(value);
		if (text.isEmpty() || "null".equals(text)) {
			return null;
		}
		return text;
	}

	@Test
	public void testUa1() {
		String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
				+ "Qoder/0.4.4 Chrome/150.0.7871.114 Electron/43.1.1 Safari/537.36";
		assertEquals("Qoder", toUserAgentSourceLabel(ua));

	}

	public void testUa2() {
		String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
				+ "Qoder Chrome/150.0.7871.114 Electron/43.1.1 Safari/537.36";
		assertEquals("Qoder", toUserAgentSourceLabel(ua));
	}

}

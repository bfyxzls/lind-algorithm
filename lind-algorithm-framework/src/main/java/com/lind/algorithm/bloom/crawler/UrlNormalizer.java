package com.lind.algorithm.bloom.crawler;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * URL 规范化：去掉 fragment、统一 host 小写、去掉默认端口、对 query 按 key 排序。 短链场景应先展开到最终长链，再规范化后写入 Bloom。
 */
public final class UrlNormalizer {

	private UrlNormalizer() {
	}

	public static String normalize(String url) {
		Objects.requireNonNull(url, "url");
		String trimmed = url.trim();
		if (trimmed.isEmpty()) {
			throw new IllegalArgumentException("url must not be empty");
		}
		try {
			URI uri = new URI(trimmed).normalize();
			String scheme = uri.getScheme() == null ? null : uri.getScheme().toLowerCase(Locale.ROOT);
			String host = uri.getHost() == null ? null : uri.getHost().toLowerCase(Locale.ROOT);
			int port = uri.getPort();
			if (port == 80 && "http".equals(scheme) || port == 443 && "https".equals(scheme)) {
				port = -1;
			}
			String path = uri.getPath();
			if (path == null || path.isEmpty()) {
				path = "/";
			}
			String query = sortQuery(uri.getRawQuery());
			URI normalized = new URI(scheme, uri.getUserInfo(), host, port, path, query, null);
			return normalized.toASCIIString();
		}
		catch (URISyntaxException ex) {
			throw new IllegalArgumentException("invalid url: " + url, ex);
		}
	}

	private static String sortQuery(String rawQuery) {
		if (rawQuery == null || rawQuery.isEmpty()) {
			return null;
		}
		String[] parts = rawQuery.split("&");
		List<String> pairs = new ArrayList<>(parts.length);
		Collections.addAll(pairs, parts);
		Collections.sort(pairs);
		return String.join("&", pairs);
	}

}

package com.lind.qps.web.catalog;

import com.lind.qps.web.job.LoadForm;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 * 将浏览器 / DevTools「Copy as cURL」命令解析为 {@link LoadForm}。
 */
final class CurlImporter {

	private static final Set<String> IGNORE_FLAGS = Set.of("--compressed", "-k", "--insecure", "-s", "--silent", "-S",
			"--show-error", "-v", "--verbose", "-L", "--location", "-i", "--include", "-#", "--progress-bar", "-N",
			"--no-buffer", "--http1.1", "--http2", "-4", "-6", "--raw");

	private static final Set<String> SKIP_VALUE_FLAGS = Set.of("-o", "--output", "-O", "--remote-name", "-w",
			"--write-out", "-m", "--max-time", "--connect-timeout", "-x", "--proxy", "--cacert", "--cert", "--key",
			"-E", "--cert-type", "--resolve", "--unix-socket", "-b", "--cookie", "-c", "--cookie-jar", "-e",
			"--referer", "--pubkey", "--pass", "--engine");

	private CurlImporter() {
	}

	record Result(LoadForm form, String suggestedName) {
	}

	static Result parse(String curl) {
		if (curl == null || curl.isBlank()) {
			throw new IllegalArgumentException("curl 命令不能为空");
		}
		List<String> tokens = tokenize(normalize(curl));
		if (tokens.isEmpty()) {
			throw new IllegalArgumentException("无法解析 curl 命令");
		}
		int i = 0;
		String first = tokens.get(0).toLowerCase(Locale.ROOT);
		if ("curl".equals(first) || "curl.exe".equals(first)) {
			i = 1;
		}
		if (i >= tokens.size()) {
			throw new IllegalArgumentException("curl 命令缺少 URL");
		}

		String method = null;
		String url = null;
		boolean forceGet = false;
		List<String> headers = new ArrayList<>();
		List<String> dataParts = new ArrayList<>();
		List<String> formParts = new ArrayList<>();

		while (i < tokens.size()) {
			String token = tokens.get(i);
			if (!token.startsWith("-")) {
				if (url == null) {
					url = token;
				}
				i++;
				continue;
			}

			FlagParts fp = splitFlag(token);
			String flag = fp.flag();

			if ("-X".equals(flag) || "--request".equals(flag)) {
				method = readArg(tokens, i, fp).toUpperCase(Locale.ROOT);
				i = nextIndex(i, fp);
			}
			else if ("-H".equals(flag) || "--header".equals(flag)) {
				headers.add(readArg(tokens, i, fp));
				i = nextIndex(i, fp);
			}
			else if ("-d".equals(flag) || "--data".equals(flag) || "--data-raw".equals(flag)
					|| "--data-binary".equals(flag) || "--data-ascii".equals(flag) || "--data-urlencode".equals(flag)) {
				dataParts.add(readArg(tokens, i, fp));
				i = nextIndex(i, fp);
			}
			else if ("-F".equals(flag) || "--form".equals(flag) || "--form-string".equals(flag)) {
				formParts.add(readArg(tokens, i, fp));
				i = nextIndex(i, fp);
			}
			else if ("--url".equals(flag)) {
				url = readArg(tokens, i, fp);
				i = nextIndex(i, fp);
			}
			else if ("-G".equals(flag) || "--get".equals(flag)) {
				forceGet = true;
				i++;
			}
			else if ("-I".equals(flag) || "--head".equals(flag)) {
				method = "HEAD";
				i++;
			}
			else if ("-A".equals(flag) || "--user-agent".equals(flag)) {
				headers.add("User-Agent: " + readArg(tokens, i, fp));
				i = nextIndex(i, fp);
			}
			else if ("-u".equals(flag) || "--user".equals(flag)) {
				String user = readArg(tokens, i, fp);
				headers.add("Authorization: Basic "
						+ Base64.getEncoder().encodeToString(user.getBytes(StandardCharsets.UTF_8)));
				i = nextIndex(i, fp);
			}
			else if (IGNORE_FLAGS.contains(flag)) {
				i++;
			}
			else if (SKIP_VALUE_FLAGS.contains(flag)) {
				if (fp.inline() == null) {
					readArg(tokens, i, fp); // validate present
				}
				i = nextIndex(i, fp);
			}
			else if (flag.startsWith("--") && fp.inline() == null && i + 1 < tokens.size()
					&& !tokens.get(i + 1).startsWith("-")) {
				i += 2;
			}
			else {
				i++;
			}
		}

		if (url == null || url.isBlank()) {
			throw new IllegalArgumentException("未找到请求 URL，请确认 curl 中包含完整地址");
		}

		LoadForm form = new LoadForm();
		UrlParts parts = splitUrl(url);
		form.setUrl(parts.baseUrl());
		form.setQueryParamsText(parts.queryText());

		boolean hasForm = !formParts.isEmpty();
		boolean hasData = !dataParts.isEmpty();
		if (forceGet || "GET".equalsIgnoreCase(method)) {
			if (hasData) {
				form.setQueryParamsText(mergeKvText(form.getQueryParamsText(), urlEncodedToLines(joinData(dataParts))));
			}
			form.setMethod(blankTo(method, "GET"));
		}
		else if (hasForm) {
			form.setMethod(blankTo(method, "POST"));
			form.setFormParamsText(String.join("\n", formParts));
			if (hasData) {
				form.setBody(joinData(dataParts));
			}
		}
		else if (hasData) {
			form.setMethod(blankTo(method, "POST"));
			String body = joinData(dataParts);
			if (looksLikeUrlEncoded(body) && !hasJsonContentType(headers)) {
				form.setFormParamsText(urlEncodedToLines(body));
			}
			else {
				form.setBody(body);
			}
		}
		else {
			form.setMethod(blankTo(method, "GET"));
		}

		form.setHeadersText(filterHeaders(headers));
		return new Result(form, suggestName(parts.baseUrl(), form.getMethod()));
	}

	private record FlagParts(String flag, String inline) {
	}

	private static FlagParts splitFlag(String token) {
		if (token.startsWith("--")) {
			int eq = token.indexOf('=');
			if (eq > 2) {
				return new FlagParts(token.substring(0, eq), token.substring(eq + 1));
			}
		}
		return new FlagParts(token, null);
	}

	private static String readArg(List<String> tokens, int flagIndex, FlagParts fp) {
		if (fp.inline() != null) {
			return fp.inline();
		}
		int valueIndex = flagIndex + 1;
		if (valueIndex >= tokens.size()) {
			throw new IllegalArgumentException("选项 " + fp.flag() + " 缺少参数");
		}
		return tokens.get(valueIndex);
	}

	private static int nextIndex(int flagIndex, FlagParts fp) {
		return fp.inline() != null ? flagIndex + 1 : flagIndex + 2;
	}

	static String normalize(String curl) {
		String text = curl.replace("\r\n", "\n").replace('\r', '\n').trim();
		text = text.replaceAll("\\^\\s*\\n", " ");
		text = text.replaceAll("\\\\\\s*\\n", " ");
		return text.trim();
	}

	static List<String> tokenize(String input) {
		List<String> tokens = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean inSingle = false;
		boolean inDouble = false;
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if (inSingle) {
				if (c == '\'') {
					inSingle = false;
				}
				else {
					current.append(c);
				}
				continue;
			}
			if (inDouble) {
				if (c == '"') {
					inDouble = false;
				}
				else if (c == '\\' && i + 1 < input.length()) {
					char next = input.charAt(i + 1);
					if (next == '"' || next == '\\') {
						current.append(next);
						i++;
					}
					else {
						current.append(c);
					}
				}
				else {
					current.append(c);
				}
				continue;
			}
			if (c == '\'') {
				inSingle = true;
			}
			else if (c == '"') {
				inDouble = true;
			}
			else if (Character.isWhitespace(c)) {
				if (!current.isEmpty()) {
					tokens.add(current.toString());
					current.setLength(0);
				}
			}
			else {
				current.append(c);
			}
		}
		if (inSingle || inDouble) {
			throw new IllegalArgumentException("curl 引号未闭合");
		}
		if (!current.isEmpty()) {
			tokens.add(current.toString());
		}
		return tokens;
	}

	private record UrlParts(String baseUrl, String queryText) {
	}

	private static UrlParts splitUrl(String rawUrl) {
		String trimmed = rawUrl.trim();
		int q = trimmed.indexOf('?');
		if (q < 0) {
			return new UrlParts(trimmed, "");
		}
		return new UrlParts(trimmed.substring(0, q), queryToLines(trimmed.substring(q + 1)));
	}

	private static String queryToLines(String query) {
		if (query == null || query.isBlank()) {
			return "";
		}
		StringJoiner joiner = new StringJoiner("\n");
		for (String part : query.split("&")) {
			if (!part.isBlank()) {
				joiner.add(part);
			}
		}
		return joiner.toString();
	}

	private static String joinData(List<String> parts) {
		return parts.size() == 1 ? parts.get(0) : String.join("&", parts);
	}

	private static String mergeKvText(String first, String second) {
		if (first == null || first.isBlank()) {
			return second == null ? "" : second;
		}
		if (second == null || second.isBlank()) {
			return first;
		}
		return first + "\n" + second;
	}

	private static boolean looksLikeUrlEncoded(String body) {
		if (body == null || body.isBlank()) {
			return false;
		}
		String t = body.trim();
		if (t.startsWith("{") || t.startsWith("[") || t.startsWith("<")) {
			return false;
		}
		return t.contains("=") && !t.contains("\n");
	}

	private static boolean hasJsonContentType(List<String> headers) {
		for (String h : headers) {
			String lower = h.toLowerCase(Locale.ROOT);
			if (lower.startsWith("content-type:") && lower.contains("application/json")) {
				return true;
			}
		}
		return false;
	}

	private static String urlEncodedToLines(String body) {
		Map<String, String> map = new LinkedHashMap<>();
		for (String part : body.split("&")) {
			if (part.isBlank()) {
				continue;
			}
			int eq = part.indexOf('=');
			if (eq <= 0) {
				map.put(decode(part), "");
			}
			else {
				map.put(decode(part.substring(0, eq)), decode(part.substring(eq + 1)));
			}
		}
		StringJoiner joiner = new StringJoiner("\n");
		map.forEach((k, v) -> joiner.add(k + "=" + v));
		return joiner.toString();
	}

	private static String decode(String value) {
		try {
			return URLDecoder.decode(value, StandardCharsets.UTF_8);
		}
		catch (IllegalArgumentException ex) {
			return value;
		}
	}

	private static String filterHeaders(List<String> headers) {
		StringJoiner joiner = new StringJoiner("\n");
		for (String header : headers) {
			String name = header;
			int idx = header.indexOf(':');
			if (idx > 0) {
				name = header.substring(0, idx).trim();
			}
			String lower = name.toLowerCase(Locale.ROOT);
			if (lower.equals("content-length") || lower.equals("expect") || lower.equals("connection")
					|| lower.equals("host") || lower.equals("accept-encoding")) {
				continue;
			}
			joiner.add(header.trim());
		}
		return joiner.toString();
	}

	static String suggestName(String url, String method) {
		try {
			URI uri = URI.create(url);
			String path = uri.getPath();
			if (path == null || path.isBlank() || "/".equals(path)) {
				return method + " " + (uri.getHost() == null ? url : uri.getHost());
			}
			String last = "";
			for (String seg : path.split("/")) {
				if (!seg.isBlank()) {
					last = seg;
				}
			}
			return method + " " + (last.isBlank() ? path : last);
		}
		catch (Exception ex) {
			return method + " 导入用例";
		}
	}

	private static String blankTo(String value, String defaultValue) {
		return value == null || value.isBlank() ? defaultValue : value.trim();
	}

}

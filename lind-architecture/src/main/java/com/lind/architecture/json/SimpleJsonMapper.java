package com.lind.architecture.json;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识点 (3)：可替换证明——手写极简 JSON（仅支持 Map/List/标量），不依赖 Jackson。
 * <p>
 * 业务侧 API 不变，只换装配即可验证「协议层不绑死某一 JSON 库」。
 */
public final class SimpleJsonMapper implements JsonMapper {

	@Override
	public void write(Object value, Writer writer) throws IOException {
		writeValue(value, writer);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T read(Reader reader, Type type) throws IOException {
		String json = readAll(reader).trim();
		Object parsed = parseValue(json);
		if (type == String.class) {
			return (T) (parsed instanceof String s ? s : json);
		}
		if (type == Map.class || (type instanceof Class<?> c && Map.class.isAssignableFrom(c))) {
			return (T) parsed;
		}
		if (parsed instanceof Map<?, ?> map) {
			return (T) map;
		}
		throw new IOException("SimpleJsonMapper cannot bind " + type + " from: " + json);
	}

	private static void writeValue(Object value, Writer w) throws IOException {
		if (value == null) {
			w.write("null");
			return;
		}
		if (value instanceof String s) {
			w.write('"');
			w.write(escape(s));
			w.write('"');
			return;
		}
		if (value instanceof Number || value instanceof Boolean) {
			w.write(String.valueOf(value));
			return;
		}
		if (value instanceof Map<?, ?> map) {
			w.write('{');
			boolean first = true;
			for (Map.Entry<?, ?> e : map.entrySet()) {
				if (!first) {
					w.write(',');
				}
				first = false;
				writeValue(String.valueOf(e.getKey()), w);
				w.write(':');
				writeValue(e.getValue(), w);
			}
			w.write('}');
			return;
		}
		if (value instanceof Iterable<?> it) {
			w.write('[');
			boolean first = true;
			for (Object o : it) {
				if (!first) {
					w.write(',');
				}
				first = false;
				writeValue(o, w);
			}
			w.write(']');
			return;
		}
		if (value instanceof JsonWritable writable) {
			writeValue(writable.toJsonTree(), w);
			return;
		}
		throw new IOException("Unsupported type for SimpleJsonMapper: " + value.getClass().getName());
	}

	private static String escape(String s) {
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private static String readAll(Reader reader) throws IOException {
		StringBuilder sb = new StringBuilder();
		char[] buf = new char[256];
		int n;
		while ((n = reader.read(buf)) >= 0) {
			sb.append(buf, 0, n);
		}
		return sb.toString();
	}

	/** 极简 parser：只够测 Map 树。 */
	private static Object parseValue(String json) throws IOException {
		json = json.trim();
		if (json.isEmpty()) {
			throw new IOException("empty json");
		}
		char c = json.charAt(0);
		if (c == '{') {
			return parseObject(json);
		}
		if (c == '[') {
			return parseArray(json);
		}
		if (c == '"') {
			return unescape(json.substring(1, json.length() - 1));
		}
		if ("true".equals(json)) {
			return Boolean.TRUE;
		}
		if ("false".equals(json)) {
			return Boolean.FALSE;
		}
		if ("null".equals(json)) {
			return null;
		}
		try {
			if (json.contains(".")) {
				return Double.parseDouble(json);
			}
			return Long.parseLong(json);
		}
		catch (NumberFormatException e) {
			throw new IOException("bad token: " + json, e);
		}
	}

	private static Map<String, Object> parseObject(String json) throws IOException {
		Map<String, Object> map = new LinkedHashMap<>();
		String body = json.substring(1, json.length() - 1).trim();
		if (body.isEmpty()) {
			return map;
		}
		for (String part : splitTopLevel(body)) {
			int colon = indexOfTopLevel(part, ':');
			if (colon < 0) {
				throw new IOException("bad object entry: " + part);
			}
			String keyRaw = part.substring(0, colon).trim();
			String valRaw = part.substring(colon + 1).trim();
			String key = (String) parseValue(keyRaw);
			map.put(key, parseValue(valRaw));
		}
		return map;
	}

	private static List<Object> parseArray(String json) throws IOException {
		List<Object> list = new ArrayList<>();
		String body = json.substring(1, json.length() - 1).trim();
		if (body.isEmpty()) {
			return list;
		}
		for (String part : splitTopLevel(body)) {
			list.add(parseValue(part.trim()));
		}
		return list;
	}

	private static List<String> splitTopLevel(String body) {
		List<String> parts = new ArrayList<>();
		StringBuilder cur = new StringBuilder();
		int depth = 0;
		boolean inStr = false;
		for (int i = 0; i < body.length(); i++) {
			char c = body.charAt(i);
			if (c == '"' && (i == 0 || body.charAt(i - 1) != '\\')) {
				inStr = !inStr;
			}
			if (!inStr) {
				if (c == '{' || c == '[') {
					depth++;
				}
				else if (c == '}' || c == ']') {
					depth--;
				}
				else if (c == ',' && depth == 0) {
					parts.add(cur.toString());
					cur.setLength(0);
					continue;
				}
			}
			cur.append(c);
		}
		if (!cur.isEmpty()) {
			parts.add(cur.toString());
		}
		return parts;
	}

	private static int indexOfTopLevel(String s, char target) {
		int depth = 0;
		boolean inStr = false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '"' && (i == 0 || s.charAt(i - 1) != '\\')) {
				inStr = !inStr;
			}
			if (!inStr) {
				if (c == '{' || c == '[') {
					depth++;
				}
				else if (c == '}' || c == ']') {
					depth--;
				}
				else if (c == target && depth == 0) {
					return i;
				}
			}
		}
		return -1;
	}

	private static String unescape(String s) {
		return s.replace("\\\"", "\"").replace("\\\\", "\\");
	}

}

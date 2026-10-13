package com.lind.algorithm.sanitize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 组合清洗：按顺序串联多个 {@link TextSanitizer}，适合日志脱敏、UGC 入库前处理。
 */
public final class CompositeTextSanitizer extends TextSanitizer {

	private final List<TextSanitizer> sanitizers;

	private final String type;

	public CompositeTextSanitizer(TextSanitizer... sanitizers) {
		this("composite", Arrays.asList(sanitizers));
	}

	public CompositeTextSanitizer(String type, List<TextSanitizer> sanitizers) {
		this.type = Objects.requireNonNull(type, "type");
		Objects.requireNonNull(sanitizers, "sanitizers");
		if (sanitizers.isEmpty()) {
			throw new IllegalArgumentException("sanitizers must not be empty");
		}
		this.sanitizers = List.copyOf(new ArrayList<>(sanitizers));
	}

	@Override
	public String type() {
		return type;
	}

	@Override
	protected String doSanitize(String input) {
		String current = input;
		for (TextSanitizer sanitizer : sanitizers) {
			current = sanitizer.sanitize(current);
			if (current == null) {
				return null;
			}
		}
		return current;
	}

	public List<TextSanitizer> sanitizers() {
		return sanitizers;
	}

}

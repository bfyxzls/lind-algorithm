package com.lind.algorithm.stringmatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * KMP 单模式串匹配。
 */
public final class KmpMatcher {

	private final String pattern;

	private final int[] lps;

	public KmpMatcher(String pattern) {
		this.pattern = Objects.requireNonNull(pattern, "pattern");
		if (pattern.isEmpty()) {
			throw new IllegalArgumentException("pattern must not be empty");
		}
		this.lps = buildLps(pattern);
	}

	public String pattern() {
		return pattern;
	}

	public List<Integer> search(String text) {
		Objects.requireNonNull(text, "text");
		List<Integer> hits = new ArrayList<>();
		int i = 0;
		int j = 0;
		while (i < text.length()) {
			if (text.charAt(i) == pattern.charAt(j)) {
				i++;
				j++;
				if (j == pattern.length()) {
					hits.add(i - j);
					j = lps[j - 1];
				}
			}
			else if (j > 0) {
				j = lps[j - 1];
			}
			else {
				i++;
			}
		}
		return Collections.unmodifiableList(hits);
	}

	public boolean contains(String text) {
		return !search(text).isEmpty();
	}

	static int[] buildLps(String pattern) {
		int[] lps = new int[pattern.length()];
		int len = 0;
		int i = 1;
		while (i < pattern.length()) {
			if (pattern.charAt(i) == pattern.charAt(len)) {
				lps[i++] = ++len;
			}
			else if (len > 0) {
				len = lps[len - 1];
			}
			else {
				lps[i++] = 0;
			}
		}
		return lps;
	}

}

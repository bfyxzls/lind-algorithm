package com.lind.algorithm.stringmatch;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 字符串匹配单元测试。
 */
public class StringMatchTest {

	@Test
	void kmpFindsAllOccurrences() {
		KmpMatcher kmp = new KmpMatcher("aba");
		assertEquals(List.of(0, 2), kmp.search("ababa"));
		assertTrue(kmp.contains("xxaba yy"));
	}

	@Test
	void ahoCorasickMultiPattern() {
		AhoCorasick ac = new AhoCorasick();
		ac.addKeyword("he");
		ac.addKeyword("she");
		ac.addKeyword("his");
		ac.addKeyword("hers");
		List<AhoCorasick.Match> matches = ac.findAll("ushers");
		assertTrue(matches.stream().anyMatch(m -> m.keyword().equals("she")));
		assertTrue(matches.stream().anyMatch(m -> m.keyword().equals("he")));
		assertTrue(matches.stream().anyMatch(m -> m.keyword().equals("hers")));
		assertFalse(matches.stream().anyMatch(m -> m.keyword().equals("user")));

	}

}

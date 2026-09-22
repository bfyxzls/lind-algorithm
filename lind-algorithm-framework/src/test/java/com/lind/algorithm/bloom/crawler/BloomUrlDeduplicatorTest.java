package com.lind.algorithm.bloom.crawler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 短链爬虫 URL 去重：规范化、认领、最终 URL / 指纹标记。
 */
public class BloomUrlDeduplicatorTest {

	@Test
	void normalizeDropsFragmentAndSortsQuery() {
		assertEquals("https://example.com/a?b=2&c=1",
				UrlNormalizer.normalize("HTTPS://Example.com:443/a?c=1&b=2#section"));
	}

	@Test
	void tryClaimMarksOnceAndSkipsDuplicate() {
		CountingStore store = new CountingStore();
		BloomUrlDeduplicator dedup = new BloomUrlDeduplicator(store, 1000, 0.01);

		assertTrue(dedup.tryClaim("https://example.com/page"));
		assertFalse(dedup.tryClaim("https://example.com/page#x"));
		assertEquals(1, store.containsCalls);
	}

	@Test
	void missDoesNotTouchStore() {
		CountingStore store = new CountingStore();
		BloomUrlDeduplicator dedup = new BloomUrlDeduplicator(store, 1000, 0.01);

		assertFalse(dedup.mightHaveSeen("https://example.com/never"));
		assertEquals(0, store.containsCalls);
	}

	@Test
	void markFetchedTracksFinalUrlAndFingerprint() {
		CountingStore store = new CountingStore();
		BloomUrlDeduplicator dedup = new BloomUrlDeduplicator(store, 1000, 0.01);

		dedup.markFetched("https://example.com/final", "abc123");
		assertTrue(dedup.mightHaveSeen("https://example.com/final"));
		assertTrue(dedup.mightHaveFetchedFingerprint("abc123"));
	}

	private static final class CountingStore implements UrlDedupStore {

		private final InMemoryUrlDedupStore delegate = new InMemoryUrlDedupStore();

		private int containsCalls;

		@Override
		public void add(String key) {
			delegate.add(key);
		}

		@Override
		public boolean contains(String key) {
			containsCalls++;
			return delegate.contains(key);
		}

		@Override
		public int size() {
			return delegate.size();
		}

		@Override
		public Collection<String> keys() {
			return List.copyOf(delegate.keys());
		}

	}

}

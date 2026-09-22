package com.lind.algorithm.bloom.recommend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 推荐去重：用户维度过滤 + 内容指纹入库。
 */
public class BloomRecommendDeduplicatorTest {

	@Test
	void filterUnseenDropsConfirmedRecommendations() {
		CountingStore store = new CountingStore();
		BloomRecommendDeduplicator dedup = new BloomRecommendDeduplicator(store, 1000, 0.01);

		dedup.markRecommended("u1", "c1");
		List<String> kept = dedup.filterUnseen("u1", List.of("c1", "c2", "c3"));

		assertEquals(List.of("c2", "c3"), kept);
		assertTrue(dedup.hasRecommended("u1", "c1"));
		assertFalse(dedup.hasRecommended("u1", "c2"));
	}

	@Test
	void windowKeepsSameContentAcrossDifferentDays() {
		BloomRecommendDeduplicator dedup = new BloomRecommendDeduplicator(new InMemoryRecommendDedupStore(), 1000,
				0.01);
		dedup.markRecommended("u1", "c1", "2026-09-21");

		assertTrue(dedup.hasRecommended("u1", "c1", "2026-09-21"));
		assertFalse(dedup.hasRecommended("u1", "c1", "2026-09-22"));
	}

	@Test
	void missDoesNotTouchStore() {
		CountingStore store = new CountingStore();
		BloomRecommendDeduplicator dedup = new BloomRecommendDeduplicator(store, 1000, 0.01);

		assertFalse(dedup.hasRecommended("u1", "never"));
		assertEquals(0, store.containsCalls);
	}

	@Test
	void tryAdmitContentRejectsDuplicateFingerprint() {
		BloomRecommendDeduplicator dedup = new BloomRecommendDeduplicator(new InMemoryRecommendDedupStore(), 1000,
				0.01);

		assertTrue(dedup.tryAdmitContent("fp-1"));
		assertFalse(dedup.tryAdmitContent("fp-1"));
	}

	private static final class CountingStore implements RecommendDedupStore {

		private final InMemoryRecommendDedupStore delegate = new InMemoryRecommendDedupStore();

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

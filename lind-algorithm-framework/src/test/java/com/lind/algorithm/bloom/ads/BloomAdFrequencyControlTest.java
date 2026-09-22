package com.lind.algorithm.bloom.ads;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 广告频控：时间窗口分片、曝光标记、候选过滤。
 */
public class BloomAdFrequencyControlTest {

	private static final long T0 = Instant.parse("2026-09-22T10:15:00Z").toEpochMilli();

	private static final long NEXT_DAY = Instant.parse("2026-09-23T10:15:00Z").toEpochMilli();

	@Test
	void dayWindowAllowsAgainNextDay() {
		BloomAdFrequencyControl control = new BloomAdFrequencyControl(new InMemoryAdImpressionStore(),
				FrequencyWindow.DAY, 1000, 0.01);

		control.markImpressed("u1", "ad1", T0);
		assertTrue(control.hasImpressed("u1", "ad1", T0));
		assertFalse(control.hasImpressed("u1", "ad1", NEXT_DAY));
	}

	@Test
	void filterEligibleDropsImpressedAds() {
		CountingStore store = new CountingStore();
		BloomAdFrequencyControl control = new BloomAdFrequencyControl(store, FrequencyWindow.HOUR, 1000, 0.01);

		control.markImpressed("u1", "ad1", T0);
		List<String> eligible = control.filterEligible("u1", List.of("ad1", "ad2"), T0);

		assertEquals(List.of("ad2"), eligible);
	}

	@Test
	void missDoesNotTouchStore() {
		CountingStore store = new CountingStore();
		BloomAdFrequencyControl control = new BloomAdFrequencyControl(store, FrequencyWindow.DAY, 1000, 0.01);

		assertFalse(control.hasImpressed("u1", "never", T0));
		assertEquals(0, store.containsCalls);
	}

	@Test
	void hourShardKeyFormat() {
		assertEquals("h:2026092210", FrequencyWindow.HOUR.shardKey(T0));
		assertEquals("d:20260922", FrequencyWindow.DAY.shardKey(T0));
	}

	private static final class CountingStore implements AdImpressionStore {

		private final InMemoryAdImpressionStore delegate = new InMemoryAdImpressionStore();

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

package com.lind.algorithm.bloom.blacklist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;

import org.junit.jupiter.api.Test;

/**
 * 黑名单流程：启动加载、先落库再 put、未命中不回源、命中回源、删除后重建。
 */
public class BloomBlacklistTest {

	@Test
	void startupLoadsExistingKeys() {
		CountingStore store = new CountingStore();
		store.add("user:9");
		BloomBlacklist blacklist = new BloomBlacklist(store, 1000, 0.01);

		assertTrue(blacklist.isBlocked("user:9"));
		assertEquals(1, store.containsCalls);
	}

	@Test
	void missDoesNotTouchStore() {
		CountingStore store = new CountingStore();
		BloomBlacklist blacklist = new BloomBlacklist(store, 1000, 0.01);

		assertFalse(blacklist.isBlocked("nobody"));
		assertEquals(0, store.containsCalls);
	}

	@Test
	void blockWritesStoreBeforeFilterAndHitConfirms() {
		CountingStore store = new CountingStore();
		BloomBlacklist blacklist = new BloomBlacklist(store, 1000, 0.01);

		blacklist.block("user:1");
		assertTrue(store.stored("user:1"));
		assertTrue(blacklist.isBlocked("user:1"));
		assertEquals(1, store.containsCalls);
	}

	@Test
	void storeFailureDoesNotPolluteFilter() {
		CountingStore store = new CountingStore();
		BloomBlacklist blacklist = new BloomBlacklist(store, 1000, 0.01);
		store.failNextAdd = true;

		assertThrows(IllegalStateException.class, () -> blacklist.block("user:2"));
		assertFalse(store.stored("user:2"));
		assertFalse(blacklist.isBlocked("user:2"));
		assertEquals(0, store.containsCalls);
	}

	@Test
	void unblockRebuildsFromStore() {
		CountingStore store = new CountingStore();
		BloomBlacklist blacklist = new BloomBlacklist(store, 1000, 0.01);
		blacklist.block("user:1");
		int confirmsBefore = store.containsCalls;

		assertTrue(blacklist.unblock("user:1"));
		assertFalse(store.stored("user:1"));
		assertFalse(blacklist.isBlocked("user:1"));
		assertEquals(confirmsBefore, store.containsCalls);
		assertFalse(blacklist.unblock("user:1"));
	}

	/**
	 * 统计 {@link BlacklistStore#contains} 调用次数，用来断言「一定不在」不会回源。
	 */
	private static final class CountingStore implements BlacklistStore {

		private final InMemoryBlacklistStore delegate = new InMemoryBlacklistStore();

		private int containsCalls;

		private boolean failNextAdd;

		boolean stored(String key) {
			return delegate.contains(key);
		}

		@Override
		public void add(String key) {
			if (failNextAdd) {
				failNextAdd = false;
				throw new IllegalStateException("store unavailable");
			}
			delegate.add(key);
		}

		@Override
		public boolean remove(String key) {
			return delegate.remove(key);
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
			return delegate.keys();
		}

	}

}

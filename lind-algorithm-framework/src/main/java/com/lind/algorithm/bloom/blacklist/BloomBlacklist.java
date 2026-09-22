package com.lind.algorithm.bloom.blacklist;

import com.lind.algorithm.bloom.BloomFilter;

import java.util.Objects;

/**
 * 黑名单前置过滤。
 * <p>
 * 真相源在 {@link BlacklistStore}。本类在构造时把已有 key 灌进内存 {@link BloomFilter}， 之后新增先落库再
 * {@code put}。查询时「一定不在」直接放行、不访问真相源；「可能在」再回源确认。 标准 Bloom 不能单点删除，解禁后按真相源重建过滤器。 运行中的拉黑和解禁走
 * {@link #block} / {@link #unblock}；若绕过本类改了真相源，需要再 {@link #reload()}。
 */
public final class BloomBlacklist {

	private final BlacklistStore store;

	private final long expectedInsertions;

	private final double falsePositiveRate;

	private BloomFilter filter;

	public BloomBlacklist(BlacklistStore store, long expectedInsertions, double falsePositiveRate) {
		this.store = Objects.requireNonNull(store, "store");
		if (expectedInsertions <= 0) {
			throw new IllegalArgumentException("expectedInsertions must be > 0");
		}
		if (falsePositiveRate <= 0 || falsePositiveRate >= 1) {
			throw new IllegalArgumentException("falsePositiveRate must be in (0,1)");
		}
		this.expectedInsertions = expectedInsertions;
		this.falsePositiveRate = falsePositiveRate;
		reload();
	}

	/**
	 * 按真相源当前数据重建 Bloom。启动加载和删除后都会走这里；也可以定时调用，避免超量插入把误判率抬高。
	 */
	public synchronized void reload() {
		long capacity = Math.max(expectedInsertions, store.size());
		BloomFilter next = new BloomFilter(capacity, falsePositiveRate);
		for (String key : store.keys()) {
			next.put(key);
		}
		this.filter = next;
	}

	/**
	 * 先写入真相源，成功后再放入 Bloom。库写入失败时过滤器保持原样。
	 */
	public synchronized void block(String key) {
		Objects.requireNonNull(key, "key");
		store.add(key);
		filter.put(key);
	}

	/**
	 * 从真相源删除，然后重建 Bloom。重建完成前若仍被旧过滤器判成「可能在」，回源会放行。
	 * @return 真相源里原本有这条记录时为 true
	 */
	public synchronized boolean unblock(String key) {
		Objects.requireNonNull(key, "key");
		boolean removed = store.remove(key);
		reload();
		return removed;
	}

	/**
	 * @return true 表示在黑名单中。Bloom 判「一定不在」时不会访问真相源。
	 */
	public synchronized boolean isBlocked(String key) {
		Objects.requireNonNull(key, "key");
		if (!filter.mightContain(key)) {
			return false;
		}
		return store.contains(key);
	}

}

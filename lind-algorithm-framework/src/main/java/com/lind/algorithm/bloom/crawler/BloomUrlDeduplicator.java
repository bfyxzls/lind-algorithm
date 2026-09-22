package com.lind.algorithm.bloom.crawler;

import com.lind.algorithm.bloom.BloomFilter;

import java.util.Objects;

/**
 * 短链爬虫 URL 去重：Bloom 前置粗筛 + 精确存储兜底。
 * <p>
 * 流程：规范化 URL → 查 Bloom；未命中一定没抓过，入队并写入；命中后回源确认，假阳性仍可抓取。 抓取完成后把最终长链、内容指纹一并标记。
 */
public final class BloomUrlDeduplicator {

	private final UrlDedupStore store;

	private final BloomFilter filter;

	public BloomUrlDeduplicator(UrlDedupStore store, long expectedInsertions, double falsePositiveRate) {
		this.store = Objects.requireNonNull(store, "store");
		this.filter = new BloomFilter(expectedInsertions, falsePositiveRate);
		for (String key : store.keys()) {
			filter.put(key);
		}
	}

	/**
	 * 尝试认领一个待抓 URL。
	 * @return true 表示一定/确认未抓过，已写入 Bloom 与真相源，应加入抓取队列；false 表示已抓过应跳过
	 */
	public synchronized boolean tryClaim(String url) {
		String key = UrlNormalizer.normalize(url);
		if (!filter.mightContain(key)) {
			persist(key);
			return true;
		}
		if (store.contains(key)) {
			return false;
		}
		// Bloom 假阳性：精确存储没有，仍可抓取
		persist(key);
		return true;
	}

	/**
	 * 抓取完成后标记最终 URL 与内容指纹，避免短链/镜像页重复抓。
	 */
	public synchronized void markFetched(String finalUrl, String contentFingerprint) {
		persist(UrlNormalizer.normalize(finalUrl));
		if (contentFingerprint != null && !contentFingerprint.isBlank()) {
			persist("fp:" + contentFingerprint.trim());
		}
	}

	/**
	 * 内容指纹是否可能已抓过（Bloom 粗判，命中后回源）。
	 */
	public synchronized boolean mightHaveFetchedFingerprint(String contentFingerprint) {
		Objects.requireNonNull(contentFingerprint, "contentFingerprint");
		String key = "fp:" + contentFingerprint.trim();
		if (!filter.mightContain(key)) {
			return false;
		}
		return store.contains(key);
	}

	public synchronized boolean mightHaveSeen(String url) {
		String key = UrlNormalizer.normalize(url);
		if (!filter.mightContain(key)) {
			return false;
		}
		return store.contains(key);
	}

	private void persist(String key) {
		store.add(key);
		filter.put(key);
	}

}

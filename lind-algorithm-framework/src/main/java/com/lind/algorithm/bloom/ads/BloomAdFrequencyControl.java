package com.lind.algorithm.bloom.ads;

import com.lind.algorithm.bloom.BloomFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 广告曝光频控：Bloom 前置过滤 + 精确存储兜底。
 * <p>
 * Key：{@code userId + adId + 时间窗口分片}。未命中可参与竞价；命中后回源确认再决定过滤/降权。
 * 曝光成功后再写入。假阳性会少曝光（损收入），因此不能只信 Bloom。
 */
public final class BloomAdFrequencyControl {

	private final AdImpressionStore store;

	private final BloomFilter filter;

	private final FrequencyWindow window;

	public BloomAdFrequencyControl(AdImpressionStore store, FrequencyWindow window, long expectedInsertions,
			double falsePositiveRate) {
		this.store = Objects.requireNonNull(store, "store");
		this.window = Objects.requireNonNull(window, "window");
		this.filter = new BloomFilter(expectedInsertions, falsePositiveRate);
		for (String key : store.keys()) {
			filter.put(key);
		}
	}

	/**
	 * 过滤掉当前窗口内「确认已曝光」的广告，其余可参与竞价/展示。
	 */
	public synchronized List<String> filterEligible(String userId, List<String> adIds, long nowEpochMillis) {
		Objects.requireNonNull(userId, "userId");
		Objects.requireNonNull(adIds, "adIds");
		List<String> eligible = new ArrayList<>(adIds.size());
		for (String adId : adIds) {
			if (!hasImpressed(userId, adId, nowEpochMillis)) {
				eligible.add(adId);
			}
		}
		return eligible;
	}

	/**
	 * 广告成功曝光后写入当前时间窗口分片。
	 */
	public synchronized void markImpressed(String userId, String adId, long nowEpochMillis) {
		persist(impressionKey(userId, adId, nowEpochMillis));
	}

	/**
	 * 也可按 campaign / creative 做频控，key 由调用方拼好后传入。
	 */
	public synchronized void markImpressedKey(String impressionKey) {
		persist(requireKey(impressionKey));
	}

	public synchronized boolean hasImpressed(String userId, String adId, long nowEpochMillis) {
		String key = impressionKey(userId, adId, nowEpochMillis);
		if (!filter.mightContain(key)) {
			return false;
		}
		return store.contains(key);
	}

	public synchronized boolean hasImpressedKey(String impressionKey) {
		String key = requireKey(impressionKey);
		if (!filter.mightContain(key)) {
			return false;
		}
		return store.contains(key);
	}

	String impressionKey(String userId, String adId, long nowEpochMillis) {
		Objects.requireNonNull(userId, "userId");
		Objects.requireNonNull(adId, "adId");
		return "u:" + userId + "|a:" + adId + "|" + window.shardKey(nowEpochMillis);
	}

	private void persist(String key) {
		store.add(key);
		filter.put(key);
	}

	private static String requireKey(String key) {
		Objects.requireNonNull(key, "key");
		if (key.isEmpty()) {
			throw new IllegalArgumentException("key must not be empty");
		}
		return key;
	}

}

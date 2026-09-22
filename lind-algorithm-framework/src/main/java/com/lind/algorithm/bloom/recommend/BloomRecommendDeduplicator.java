package com.lind.algorithm.bloom.recommend;

import com.lind.algorithm.bloom.BloomFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 推荐系统去重：用户维度（避免重复推荐）+ 内容库维度（避免重复内容入库）。
 * <p>
 * Key：{@code userId + contentId}（可选时间窗口后缀）、内容指纹。Bloom 粗筛后回源确认。
 */
public final class BloomRecommendDeduplicator {

	private final RecommendDedupStore store;

	private final BloomFilter filter;

	public BloomRecommendDeduplicator(RecommendDedupStore store, long expectedInsertions, double falsePositiveRate) {
		this.store = Objects.requireNonNull(store, "store");
		this.filter = new BloomFilter(expectedInsertions, falsePositiveRate);
		for (String key : store.keys()) {
			filter.put(key);
		}
	}

	/**
	 * 从候选中过滤掉该用户「确认已推荐过」的内容；Bloom 未命中的保留，命中则回源。
	 */
	public synchronized List<String> filterUnseen(String userId, List<String> contentIds) {
		Objects.requireNonNull(userId, "userId");
		Objects.requireNonNull(contentIds, "contentIds");
		List<String> kept = new ArrayList<>(contentIds.size());
		for (String contentId : contentIds) {
			if (!hasRecommended(userId, contentId, null)) {
				kept.add(contentId);
			}
		}
		return kept;
	}

	/**
	 * 带时间窗口的过滤，例如 {@code windowKey = "2026-09-22"} 表示按天去重。
	 */
	public synchronized List<String> filterUnseen(String userId, List<String> contentIds, String windowKey) {
		Objects.requireNonNull(userId, "userId");
		Objects.requireNonNull(contentIds, "contentIds");
		List<String> kept = new ArrayList<>(contentIds.size());
		for (String contentId : contentIds) {
			if (!hasRecommended(userId, contentId, windowKey)) {
				kept.add(contentId);
			}
		}
		return kept;
	}

	/**
	 * 展示后写入，后续该用户不会再被推荐同一内容（在同一窗口内）。
	 */
	public synchronized void markRecommended(String userId, String contentId) {
		markRecommended(userId, contentId, null);
	}

	public synchronized void markRecommended(String userId, String contentId, String windowKey) {
		persist(userContentKey(userId, contentId, windowKey));
	}

	public synchronized boolean hasRecommended(String userId, String contentId) {
		return hasRecommended(userId, contentId, null);
	}

	public synchronized boolean hasRecommended(String userId, String contentId, String windowKey) {
		String key = userContentKey(userId, contentId, windowKey);
		if (!filter.mightContain(key)) {
			return false;
		}
		return store.contains(key);
	}

	/**
	 * 内容库去重：指纹不存在则允许入库并写入；存在则可能重复，返回 false 交给精确相似度判断。
	 */
	public synchronized boolean tryAdmitContent(String contentFingerprint) {
		Objects.requireNonNull(contentFingerprint, "contentFingerprint");
		String key = "content-fp:" + contentFingerprint.trim();
		if (!filter.mightContain(key)) {
			persist(key);
			return true;
		}
		if (store.contains(key)) {
			return false;
		}
		persist(key);
		return true;
	}

	static String userContentKey(String userId, String contentId, String windowKey) {
		Objects.requireNonNull(userId, "userId");
		Objects.requireNonNull(contentId, "contentId");
		if (windowKey == null || windowKey.isBlank()) {
			return "u:" + userId + "|c:" + contentId;
		}
		return "u:" + userId + "|c:" + contentId + "|w:" + windowKey.trim();
	}

	private void persist(String key) {
		store.add(key);
		filter.put(key);
	}

}

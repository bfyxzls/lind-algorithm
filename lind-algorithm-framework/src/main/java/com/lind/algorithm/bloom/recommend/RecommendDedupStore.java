package com.lind.algorithm.bloom.recommend;

import java.util.Collection;

/**
 * 推荐去重精确存储。Bloom 命中后回源，避免假阳性过度过滤。
 */
public interface RecommendDedupStore {

	void add(String key);

	boolean contains(String key);

	int size();

	Collection<String> keys();

}

package com.lind.algorithm.bloom.crawler;

import java.util.Collection;

/**
 * 爬虫 URL 精确去重真相源。Bloom 命中后可回源确认，避免假阳性导致漏抓。
 */
public interface UrlDedupStore {

	void add(String key);

	boolean contains(String key);

	int size();

	Collection<String> keys();

}

package com.lind.algorithm.bloom.ads;

import java.util.Collection;

/**
 * 广告曝光精确计数/记录真相源。Bloom 命中后回源，避免假阳性少曝光。
 */
public interface AdImpressionStore {

	void add(String key);

	boolean contains(String key);

	int size();

	Collection<String> keys();

}

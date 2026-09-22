package com.lind.algorithm.bloom.blacklist;

import java.util.Collection;

/**
 * 黑名单真相源。Bloom 只是前置过滤器，增删与最终判定都以这里为准。
 */
public interface BlacklistStore {

	void add(String key);

	/**
	 * @return 原本存在并被删掉时为 true
	 */
	boolean remove(String key);

	boolean contains(String key);

	int size();

	/**
	 * 当前有效 key 的快照，供启动加载和删除后重建使用。
	 */
	Collection<String> keys();

}

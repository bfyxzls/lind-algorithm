package com.lind.algorithm.loadbalance;

import java.util.Collection;
import java.util.Objects;

/**
 * 负载均衡选择器：从候选节点中选一个。
 * <p>
 * {@code key} 常为服务名 / 用户 ID / 请求指纹；部分策略（轮询、LRU、LFU）会按 key 维护独立状态， 一致性哈希用 key 做哈希定位。
 * </p>
 */
@FunctionalInterface
public interface LoadBalancer {

	/**
	 * @param key 路由键（不可为 null）
	 * @param nodes 候选节点地址（不可为 null / 空）
	 * @return 选中的节点
	 * @throws IllegalArgumentException nodes 为空或含 null
	 */
	String select(String key, Collection<String> nodes);

	/**
	 * 校验并转为数组，保证后续下标访问安全。
	 */
	static String[] requireNodes(Collection<String> nodes) {
		Objects.requireNonNull(nodes, "nodes");
		if (nodes.isEmpty()) {
			throw new IllegalArgumentException("nodes must not be empty");
		}
		String[] arr = nodes.toArray(new String[0]);
		for (String n : arr) {
			if (n == null) {
				throw new IllegalArgumentException("nodes must not contain null");
			}
		}
		return arr;
	}

}

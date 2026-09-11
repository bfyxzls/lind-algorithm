package com.lind.algorithm.loadbalance;

/**
 * 带权重的节点（用于加权轮询）。
 * @param address 节点地址
 * @param weight 权重，必须 &gt; 0
 */
public record WeightedNode(String address, int weight) {

	public WeightedNode {
		if (address == null || address.isEmpty()) {
			throw new IllegalArgumentException("address must not be blank");
		}
		if (weight <= 0) {
			throw new IllegalArgumentException("weight must be > 0");
		}
	}

}

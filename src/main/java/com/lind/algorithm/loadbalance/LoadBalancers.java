package com.lind.algorithm.loadbalance;

/**
 * 内置负载均衡策略枚举（每个枚举值持有独立策略实例）。
 */
public enum LoadBalancers {

	RANDOM(new RandomLoadBalancer()),

	ROUND_ROBIN(new RoundRobinLoadBalancer()),

	LRU(new LruLoadBalancer()),

	LFU(new LfuLoadBalancer()),

	CONSISTENT_HASH(new ConsistentHashLoadBalancer());

	private final LoadBalancer balancer;

	LoadBalancers(LoadBalancer balancer) {
		this.balancer = balancer;
	}

	public LoadBalancer balancer() {
		return balancer;
	}

	/**
	 * 按名称匹配（忽略大小写）；未匹配返回 defaultBalancer。
	 */
	public static LoadBalancers match(String name, LoadBalancers defaultBalancer) {
		if (name != null) {
			for (LoadBalancers item : values()) {
				if (item.name().equalsIgnoreCase(name)) {
					return item;
				}
			}
		}
		return defaultBalancer;
	}

}

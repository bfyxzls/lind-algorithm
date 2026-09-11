package com.lind.algorithm.loadbalance;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 随机负载均衡。
 */
public final class RandomLoadBalancer implements LoadBalancer {

	@Override
	public String select(String key, Collection<String> nodes) {
		Objects.requireNonNull(key, "key");
		String[] arr = LoadBalancer.requireNodes(nodes);
		return arr[ThreadLocalRandom.current().nextInt(arr.length)];
	}

}

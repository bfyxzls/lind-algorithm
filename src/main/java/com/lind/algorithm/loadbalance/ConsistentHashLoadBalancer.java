package com.lind.algorithm.loadbalance;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * 一致性哈希负载均衡（MD5 环 + 虚拟节点），同一 key 尽量落到同一物理节点。
 */
public final class ConsistentHashLoadBalancer implements LoadBalancer {

	private final int virtualNodeNum;

	public ConsistentHashLoadBalancer() {
		this(100);
	}

	public ConsistentHashLoadBalancer(int virtualNodeNum) {
		if (virtualNodeNum <= 0) {
			throw new IllegalArgumentException("virtualNodeNum must be > 0");
		}
		this.virtualNodeNum = virtualNodeNum;
	}

	@Override
	public String select(String key, Collection<String> nodes) {
		Objects.requireNonNull(key, "key");
		String[] arr = LoadBalancer.requireNodes(nodes);

		TreeMap<Long, String> ring = new TreeMap<>();
		for (String address : arr) {
			for (int i = 0; i < virtualNodeNum; i++) {
				ring.put(hash("SHARD-" + address + "-NODE-" + i), address);
			}
		}

		long jobHash = hash(key);
		SortedMap<Long, String> tail = ring.tailMap(jobHash);
		if (!tail.isEmpty()) {
			return tail.get(tail.firstKey());
		}
		return ring.firstEntry().getValue();
	}

	/**
	 * MD5 截断为 32-bit 环上哈希。
	 */
	long hash(String key) {
		MessageDigest md5;
		try {
			md5 = MessageDigest.getInstance("MD5");
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("MD5 not supported", e);
		}
		byte[] digest = md5.digest(key.getBytes(StandardCharsets.UTF_8));
		long hashCode = ((long) (digest[3] & 0xFF) << 24) | ((long) (digest[2] & 0xFF) << 16)
				| ((long) (digest[1] & 0xFF) << 8) | (digest[0] & 0xFF);
		return hashCode & 0xffffffffL;
	}

}

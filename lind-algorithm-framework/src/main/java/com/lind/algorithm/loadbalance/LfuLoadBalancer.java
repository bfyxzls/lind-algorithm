package com.lind.algorithm.loadbalance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LFU 负载均衡：优先选择累计被选次数最少的节点。
 * <p>
 * 修复点：{@code putIfAbsent} 失败后仍写本地 map；HashMap 并发不安全；去掉 slf4j 依赖。
 * </p>
 */
public final class LfuLoadBalancer implements LoadBalancer {

	private static final long DAY_MS = 24L * 60 * 60 * 1000;

	private static final int COUNTER_RESET = 1_000_000;

	private final ConcurrentMap<String, HashMap<String, Integer>> jobLfuMap = new ConcurrentHashMap<>();

	private final AtomicLong cacheValidUntil = new AtomicLong(0);

	@Override
	public String select(String key, Collection<String> nodes) {
		Objects.requireNonNull(key, "key");
		String[] arr = LoadBalancer.requireNodes(nodes);
		maybeClearExpired();

		HashMap<String, Integer> lfu = jobLfuMap.computeIfAbsent(key, k -> new HashMap<>());
		synchronized (lfu) {
			Set<String> alive = new HashSet<>(arr.length * 2);
			for (String address : arr) {
				alive.add(address);
				Integer cnt = lfu.get(address);
				if (cnt == null || cnt > COUNTER_RESET) {
					lfu.put(address, 0);
				}
			}
			List<String> stale = new ArrayList<>();
			for (String exist : lfu.keySet()) {
				if (!alive.contains(exist)) {
					stale.add(exist);
				}
			}
			for (String del : stale) {
				lfu.remove(del);
			}
			if (lfu.isEmpty()) {
				throw new IllegalStateException("no available node after sync");
			}

			String selected = null;
			int min = Integer.MAX_VALUE;
			for (Map.Entry<String, Integer> e : lfu.entrySet()) {
				int v = e.getValue();
				if (v < min || (v == min && (selected == null || e.getKey().compareTo(selected) < 0))) {
					min = v;
					selected = e.getKey();
				}
			}
			lfu.put(selected, min + 1);
			return selected;
		}
	}

	private void maybeClearExpired() {
		long now = System.currentTimeMillis();
		long valid = cacheValidUntil.get();
		if (now > valid && cacheValidUntil.compareAndSet(valid, now + DAY_MS)) {
			jobLfuMap.clear();
		}
	}

}

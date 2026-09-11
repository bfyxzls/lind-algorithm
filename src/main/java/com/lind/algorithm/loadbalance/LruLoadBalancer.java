package com.lind.algorithm.loadbalance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LRU 负载均衡：优先选择最近最少被选中的节点（access-order LinkedHashMap 队头）。
 * <p>
 * 修复点：`putIfAbsent` 失败后仍写本地 map；`LinkedHashMap`/`HashMap` 未同步 → `computeIfAbsent` + 按 map 加锁；access-order 下禁止用 `putIfAbsent` 同步节点（会触发 afterNodeAccess 打乱 LRU）
 * </p>
 */
public final class LruLoadBalancer implements LoadBalancer {

	private static final long DAY_MS = 24L * 60 * 60 * 1000;

	private static final int MAX_CACHE_ENTRIES = 1000;

	private final ConcurrentMap<String, LinkedHashMap<String, String>> jobLruMap = new ConcurrentHashMap<>();

	private final AtomicLong cacheValidUntil = new AtomicLong(0);

	@Override
	public String select(String key, Collection<String> nodes) {
		Objects.requireNonNull(key, "key");
		String[] arr = LoadBalancer.requireNodes(nodes);
		maybeClearExpired();

		LinkedHashMap<String, String> lru = jobLruMap.computeIfAbsent(key, k -> newLruMap());
		synchronized (lru) {
			Set<String> alive = new HashSet<>(arr.length * 2);
			for (String address : arr) {
				alive.add(address);
				// 不可用 putIfAbsent：access-order 下仍会 afterNodeAccess，打乱 LRU 顺序
				if (!lru.containsKey(address)) {
					lru.put(address, address);
				}
			}
			List<String> stale = new ArrayList<>();
			for (String exist : lru.keySet()) {
				if (!alive.contains(exist)) {
					stale.add(exist);
				}
			}
			for (String del : stale) {
				lru.remove(del);
			}
			if (lru.isEmpty()) {
				throw new IllegalStateException("no available node after sync");
			}
			// 队头为最久未访问；get 将其移到队尾
			String eldest = lru.entrySet().iterator().next().getKey();
			lru.get(eldest);
			return eldest;
		}
	}

	private static LinkedHashMap<String, String> newLruMap() {
		return new LinkedHashMap<>(16, 0.75f, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
				return size() > MAX_CACHE_ENTRIES;
			}
		};
	}

	private void maybeClearExpired() {
		long now = System.currentTimeMillis();
		long valid = cacheValidUntil.get();
		if (now > valid && cacheValidUntil.compareAndSet(valid, now + DAY_MS)) {
			jobLruMap.clear();
		}
	}

}

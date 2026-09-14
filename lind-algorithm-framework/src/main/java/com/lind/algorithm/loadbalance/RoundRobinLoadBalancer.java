package com.lind.algorithm.loadbalance;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 轮询负载均衡（按 key 独立计数）。
 * <p>
 * 修复点：使用 {@link Math#floorMod} 避免计数溢出后出现负下标；用 {@link ConcurrentHashMap#computeIfAbsent}
 * 消除 put 竞态。
 * </p>
 */
public final class RoundRobinLoadBalancer implements LoadBalancer {

	private static final long DAY_MS = 24L * 60 * 60 * 1000;

	private final ConcurrentMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();

	private final AtomicLong cacheValidUntil = new AtomicLong(0);

	@Override
	public String select(String key, Collection<String> nodes) {
		Objects.requireNonNull(key, "key");
		String[] arr = LoadBalancer.requireNodes(nodes);
		maybeClearExpired();
		int seq = next(key);
		return arr[Math.floorMod(seq, arr.length)];
	}

	private int next(String key) {
		AtomicInteger counter = counters.computeIfAbsent(key,
				k -> new AtomicInteger(ThreadLocalRandom.current().nextInt(100)));
		return counter.incrementAndGet();
	}

	private void maybeClearExpired() {
		long now = System.currentTimeMillis();
		long valid = cacheValidUntil.get();
		if (now > valid && cacheValidUntil.compareAndSet(valid, now + DAY_MS)) {
			counters.clear();
		}
	}

}

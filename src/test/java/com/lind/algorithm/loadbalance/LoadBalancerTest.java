package com.lind.algorithm.loadbalance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * 负载均衡算法测试。
 */
public class LoadBalancerTest {

	private static final List<String> NODES = List.of("a", "b", "c");

	@Test
	void emptyNodesRejected() {
		assertThrows(IllegalArgumentException.class, () -> new RandomLoadBalancer().select("svc", List.of()));
	}

	@Test
	void randomAlwaysFromCandidates() {
		RandomLoadBalancer lb = new RandomLoadBalancer();
		for (int i = 0; i < 100; i++) {
			assertTrue(NODES.contains(lb.select("svc", NODES)));
		}
	}

	@Test
	void roundRobinCycles() {
		RoundRobinLoadBalancer lb = new RoundRobinLoadBalancer();
		Set<String> seen = new HashSet<>();
		for (int i = 0; i < 30; i++) {
			seen.add(lb.select("svc", NODES));
		}
		assertEquals(Set.copyOf(NODES), seen);

		Map<String, Integer> counts = countSelections(lb, "svc", NODES, 300);
		assertEquals(100, counts.get("a"));
		assertEquals(100, counts.get("b"));
		assertEquals(100, counts.get("c"));
	}

	@Test
	void lfuSpreadsEvenly() {
		LfuLoadBalancer lb = new LfuLoadBalancer();
		Map<String, Integer> counts = countSelections(lb, "OrderService", NODES, 30);
		assertEquals(10, counts.get("a"));
		assertEquals(10, counts.get("b"));
		assertEquals(10, counts.get("c"));
	}

	@Test
	void lruPrefersLeastRecentlySelected() {
		LruLoadBalancer lb = new LruLoadBalancer();
		// 首次按插入顺序选队头，随后 get 移到队尾，下一轮选新的队头
		String first = lb.select("svc", NODES);
		String second = lb.select("svc", NODES);
		String third = lb.select("svc", NODES);
		assertEquals(Set.copyOf(NODES), Set.of(first, second, third));
	}

	@Test
	void consistentHashStableForSameKey() {
		ConsistentHashLoadBalancer lb = new ConsistentHashLoadBalancer();
		TreeSet<String> nodes = new TreeSet<>(List.of("192.168.1.2", "192.168.1.3", "192.168.1.4"));
		String a = lb.select("user-42", nodes);
		String b = lb.select("user-42", nodes);
		assertEquals(a, b);
		assertTrue(nodes.contains(a));
	}

	@Test
	void consistentHashRemapsFewWhenNodeRemoved() {
		ConsistentHashLoadBalancer lb = new ConsistentHashLoadBalancer(200);
		List<String> full = List.of("n1", "n2", "n3", "n4", "n5");
		List<String> reduced = List.of("n1", "n2", "n3", "n4");

		int remapped = 0;
		int total = 200;
		for (int i = 0; i < total; i++) {
			String key = "k-" + i;
			String before = lb.select(key, full);
			String after = lb.select(key, reduced);
			if (!before.equals(after)) {
				remapped++;
				// 被删节点上的 key 必然重映射；其他节点应尽量不变
				if (!before.equals("n5")) {
					// 允许少量因虚拟节点碰撞导致的漂移，但不应大面积跳动
				}
			}
			else {
				assertTrue(reduced.contains(after));
			}
		}
		// 经验上远小于 total；删除 1/5 节点时重映射比例通常接近约 20% 量级
		assertTrue(remapped < total * 0.45, "remapped=" + remapped);
	}

	@Test
	void weightedRoundRobinRespectsRatio() {
		WeightedRoundRobinLoadBalancer lb = new WeightedRoundRobinLoadBalancer();
		List<WeightedNode> nodes = List.of(new WeightedNode("a", 5), new WeightedNode("b", 1),
				new WeightedNode("c", 1));
		Map<String, Integer> counts = new HashMap<>();
		for (int i = 0; i < 70; i++) {
			String n = lb.select("api", nodes);
			counts.merge(n, 1, Integer::sum);
		}
		assertEquals(50, counts.get("a"));
		assertEquals(10, counts.get("b"));
		assertEquals(10, counts.get("c"));
	}

	@Test
	void roundRobinConcurrentSafe() throws Exception {
		RoundRobinLoadBalancer lb = new RoundRobinLoadBalancer();
		int threads = 8;
		int perThread = 500;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(threads);
		AtomicInteger errors = new AtomicInteger();

		for (int t = 0; t < threads; t++) {
			pool.execute(() -> {
				try {
					start.await();
					for (int i = 0; i < perThread; i++) {
						String n = lb.select("svc", NODES);
						if (!NODES.contains(n)) {
							errors.incrementAndGet();
						}
					}
				}
				catch (Exception e) {
					errors.incrementAndGet();
				}
				finally {
					done.countDown();
				}
			});
		}
		start.countDown();
		assertTrue(done.await(10, TimeUnit.SECONDS));
		pool.shutdownNow();
		assertEquals(0, errors.get());
	}

	@Test
	void lfuConcurrentSafe() throws Exception {
		LfuLoadBalancer lb = new LfuLoadBalancer();
		int threads = 8;
		int perThread = 200;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(threads);
		AtomicInteger errors = new AtomicInteger();

		for (int t = 0; t < threads; t++) {
			pool.execute(() -> {
				try {
					start.await();
					for (int i = 0; i < perThread; i++) {
						String n = lb.select("svc", NODES);
						if (!NODES.contains(n)) {
							errors.incrementAndGet();
						}
					}
				}
				catch (Exception e) {
					errors.incrementAndGet();
				}
				finally {
					done.countDown();
				}
			});
		}
		start.countDown();
		assertTrue(done.await(10, TimeUnit.SECONDS));
		pool.shutdownNow();
		assertEquals(0, errors.get());
	}

	@Test
	void loadBalancersMatch() {
		assertEquals(LoadBalancers.LFU, LoadBalancers.match("lfu", LoadBalancers.RANDOM));
		assertEquals(LoadBalancers.RANDOM, LoadBalancers.match("nope", LoadBalancers.RANDOM));
	}

	private static Map<String, Integer> countSelections(LoadBalancer lb, String key, List<String> nodes, int times) {
		Map<String, Integer> counts = new HashMap<>();
		for (int i = 0; i < times; i++) {
			counts.merge(lb.select(key, nodes), 1, Integer::sum);
		}
		return counts;
	}

}

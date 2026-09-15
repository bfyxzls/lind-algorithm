结论先说：**这是真正的滑动窗口限流**（更准确是 **Sliding Window Log / 滑动日志窗口**），**不是**固定窗口。

### 判断依据

核心在 `evict` + `tryAcquire`：

```72:76:lind-algorithm-framework/src/main/java/com/lind/algorithm/ratelimit/SlidingWindowRateLimiter.java
	private void evict(long now) {
		long threshold = now - windowNanos;
		while (!timestamps.isEmpty() && timestamps.peekFirst() <= threshold) {
			timestamps.removeFirst();
		}
	}
```

每次请求都会：

1. 取当前时间 `now`
2. 丢掉所有 `<= now - windowNanos` 的时间戳
3. 看剩下「落在最近一个窗口内」的请求数是否超限
4. 通过则把本次请求的时间戳记入队列

窗口始终是相对当前时刻的区间 `[now - window, now]`，会随请求时间连续向前滑，而不是对齐到整秒/整分钟这类固定边界。

### 和固定窗口的差别


|      | 固定窗口 Fixed Window          | 本实现 Sliding Window Log       |
| ---- | -------------------------- | ---------------------------- |
| 窗口边界 | 对齐时钟，如 `10:00:00–10:00:59` | 相对 `now` 连续滑动                |
| 计数方式 | 当前桶一个计数器，到点清零              | 保存每次请求时间戳，过期逐条剔除             |
| 边界突刺 | 窗口交界处可能打满 2 倍配额            | 任意时刻看的都是「最近 window 时长」内的真实次数 |
| 内存   | O(1)                       | O(窗口内请求数)                    |


举例：窗口 1 秒、限 100 次。

- **固定窗口**：`0.9s` 打 100 次、`1.1s` 再打 100 次，都算合法（落在两个桶），短时间实际到了 200。
- **本实现**：`1.1s` 时会先清掉 `<= 0.1s` 的记录；若 `0.9s` 那 100 次还在窗口内，第 101 次会被拒。



### 补充归类

业界常把滑动窗口再细分为：

1. **Sliding Window Log**（本类）：存时间戳，精确，内存随 QPS 涨
2. **Sliding Window Counter**：用相邻两个固定窗口按比例加权，近似滑动，更省内存

你们这个 `Deque<Long> timestamps` 方案属于第 1 种，类名和实现是一致的。
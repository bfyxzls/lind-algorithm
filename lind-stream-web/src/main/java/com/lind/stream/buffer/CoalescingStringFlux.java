package com.lind.stream.buffer;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * 将细粒度 token {@link Flux} 攒成更大片段再下游推送，减少 SSE 事件次数与小包写出。
 * <p>
 * 触发条件（满足其一即 emit）：
 * <ul>
 * <li>缓冲字符数 ≥ {@code flushChars}</li>
 * <li>距本批第一个 token 已过 {@code maxDelay}</li>
 * <li>上游 complete（刷出残余）</li>
 * </ul>
 */
public final class CoalescingStringFlux {

	private CoalescingStringFlux() {
	}

	/**
	 * 把上游每个小 token(大模型返回的内容的最小单位） 攒进内存，够长或超时再 {@code sink.next} 一次。
	 * @param source 上游细粒度字符串流（例如逐词片断）
	 * @param flushChars 缓冲里字符数达到该值就立刻发出
	 * @param maxDelay 本批第一个 token 进来后，最多再等这么久也要发出（未满阈值时的兜底）
	 */
	public static Flux<String> coalesce(Flux<String> source, int flushChars, Duration maxDelay) {
		// ---- 参数校验：非法配置直接失败，避免静默算出奇怪行为 ----
		Objects.requireNonNull(source, "source");
		Objects.requireNonNull(maxDelay, "maxDelay");
		if (flushChars <= 0) {
			throw new IllegalArgumentException("flushChars must be > 0");
		}
		if (maxDelay.isZero() || maxDelay.isNegative()) {
			throw new IllegalArgumentException("maxDelay must be > 0");
		}

		// Flux.create：自己控制「何时对下游 next / error / complete」
		// 为什么不用现成的 bufferTimeout？因为它按「元素个数」切窗，我们要按「字符数」切窗。
		return Flux.create(sink -> {
			// 当前这一批还没发出去的字符（线程间共享，下面用 lock 保护）
			StringBuilder buffer = new StringBuilder();
			// 时间窗定时器要跑在独立 Worker 上，不能堵在上游订阅线程里空转
			Scheduler.Worker worker = Schedulers.boundedElastic().createWorker();
			// 当前已预约的「延迟 flush」任务；null 表示还没预约
			AtomicReference<Disposable> scheduled = new AtomicReference<>();
			// 保护 buffer 与「是否已 schedule」相关的临界区（上游 onNext 与定时器线程会并发）
			Object lock = new Object();

			// 取消尚未触发的延迟 flush，避免空缓冲被定时器误刷、或 close 后仍回调
			Runnable cancelTimer = () -> {
				// getAndSet(null)：取出旧任务并清空引用，保证只取消一次
				Disposable d = scheduled.getAndSet(null);
				if (d != null) {
					d.dispose(); // 告诉 Scheduler：这个延迟任务作废
				}
			};

			// 真正把 buffer 里的内容推给下游（一次 SSE 片段）
			Runnable flush = () -> {
				String out;
				synchronized (lock) {
					// 没东西可发：只清定时器（例如定时器触发时已被字符阈值刷空）
					if (buffer.isEmpty()) {
						cancelTimer.run();
						return;
					}
					// 取出整批内容，立刻清空缓冲，准备攒下一批
					out = buffer.toString();
					buffer.setLength(0);
					// 本批已发出，延迟任务不再需要
					cancelTimer.run();
				}
				// 在锁外 next，避免下游慢消费时长时间占着 lock
				sink.next(out);
			};

			// 若本批还没有定时器，则预约一次「maxDelay 后 flush」
			Runnable scheduleIfNeeded = () -> {
				// 已有定时器：说明本批第一个 token 已经开过窗，不要重复重置（固定从首 token 起算）
				if (scheduled.get() != null) {
					return;
				}
				// 到点执行 flush（可能与「字符满阈值」的 flush 竞争，靠 lock + 空缓冲判断消化）
				Disposable d = worker.schedule(flush, maxDelay.toMillis(), TimeUnit.MILLISECONDS);
				// CAS：只有「当前仍是 null」才挂上；若别人已挂上，丢掉这次多余的 schedule
				if (!scheduled.compareAndSet(null, d)) {
					d.dispose();
				}
			};

			// 订阅上游：三个回调分别对应 onNext / onError / onComplete
			Disposable upstream = source.subscribe(
					// onNext：来了一个小 token
					token -> {
						// 空串没有攒包意义，直接忽略
						if (token == null || token.isEmpty()) {
							return;
						}
						boolean shouldFlush;
						synchronized (lock) {
							// 拼进当前批次
							buffer.append(token);
							// 是否已达字符阈值
							shouldFlush = buffer.length() >= flushChars;
							if (!shouldFlush) {
								// 未满：确保有一个时间窗兜底（首 token 时会真正 schedule）
								scheduleIfNeeded.run();
							}
							// 已满：不在锁里 flush，先出锁再刷，减少持锁时间
						}
						if (shouldFlush) {
							// 字符够了：立刻发出，不等定时器
							flush.run();
						}
					},
					// onError：上游失败，清理资源后把错误传给下游
					err -> {
						cancelTimer.run();
						worker.dispose(); // 释放 Worker 线程资源
						sink.error(err);
					},
					// onComplete：上游结束，把残余字符刷出去，再 complete
					() -> {
						flush.run(); // 尾包：哪怕不满 flushChars 也要发给客户端
						worker.dispose();
						sink.complete();
					});

			// 下游取消订阅（客户端断开 SSE 等）时，停掉上游与定时器，避免泄漏
			sink.onDispose(() -> {
				upstream.dispose();
				cancelTimer.run();
				worker.dispose();
			});
		});
	}

	/** 读配置版：关闭攒包时原样返回 upstream。 */
	public static Flux<String> coalesce(Flux<String> source, SseCoalesceProperties props) {
		Objects.requireNonNull(props, "props");
		if (!props.isEnabled()) {
			return source;
		}
		return coalesce(source, props.getFlushChars(), props.getMaxDelay());
	}

}

package com.lind.depth.stream;

import java.time.Duration;
import java.util.Objects;

/**
 * 攒包策略：<b>字节阈值</b> + <b>时间窗兜底</b>。
 *
 * <p>
 * 对应 {@code docs/工程化分析.md} 场景一的两条工程建议：
 * <ul>
 * <li>buffer 不满一个 MTU（约 1500 字节）就不急着发 —— 用 {@link #flushBytes} 表达</li>
 * <li>用时间窗口（如 10ms）兜底 —— 用 {@link #maxDelay} 表达，避免首包一直等满包</li>
 * </ul>
 * @param flushBytes 缓冲区累计到该字节数则立即 flush；以太网 MTU≈1500，有效载荷常取 1200~1400
 * @param maxDelay 距上次 flush 超过该时长也 flush，控制端到端延迟上界
 */
public record FlushPolicy(int flushBytes, Duration maxDelay) {

	/**
	 * 默认策略：接近一个 MTU 有效载荷，时间窗 10ms。
	 * <p>
	 * 可直接用于演示；生产环境应按实测 RTT / 包长再调。
	 */
	public static FlushPolicy mtuDefault() {
		return of(1400, Duration.ofMillis(10));
	}

	/**
	 * 构造策略并校验参数合法性。
	 * @throws IllegalArgumentException flushBytes ≤ 0，或 maxDelay 非正
	 */
	public static FlushPolicy of(int flushBytes, Duration maxDelay) {
		if (flushBytes <= 0) {
			throw new IllegalArgumentException("flushBytes 必须 > 0");
		}
		Objects.requireNonNull(maxDelay, "maxDelay");
		if (maxDelay.isNegative() || maxDelay.isZero()) {
			throw new IllegalArgumentException("maxDelay 必须 > 0");
		}
		return new FlushPolicy(flushBytes, maxDelay);
	}

}

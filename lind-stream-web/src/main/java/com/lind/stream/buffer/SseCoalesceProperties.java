package com.lind.stream.buffer;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SSE 攒包策略：字符阈值 + 时间窗（对照 lind-depth-research FlushPolicy 思想，落在 WebFlux 字符层）。
 * <p>
 * <b>改配置请改 {@code application.yml} 的 {@code lind.stream.coalesce.*}</b>； 下面字段默认值只在 YAML
 * 未配置时生效，改 Java 默认值经常「看起来没变化」。
 * <p>
 * 两个旋钮要一起看：模拟 token 约 40ms 一个、约 2 字符。若 {@code maxDelay=50ms}，缓冲里往往只有 1～2 个 token
 * 就被时间窗刷出，此时把 {@code flushChars} 从 48 改成 480 几乎无感。
 */
@ConfigurationProperties(prefix = "lind.stream.coalesce")
public class SseCoalesceProperties {

	/** 是否启用攒包；关闭则每 token 一事件（旧行为）。 */
	private boolean enabled = true;

	/**
	 * 累计字符数达到该值则立即发出一个 SSE 片段。
	 * <p>
	 * 仅当在 {@code maxDelay} 内真能攒到这么多字时才会触发；否则总是时间窗先刷。
	 */
	private int flushChars = 48;

	/**
	 * 距本批第一个 token 超过该时长也发出（未满阈值的兜底）。
	 * <p>
	 * 演示大包时请调大（如 2s～3s）；调太小会把 {@link #flushChars} 架空。
	 */
	private Duration maxDelay = Duration.ofSeconds(3);

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getFlushChars() {
		return flushChars;
	}

	public void setFlushChars(int flushChars) {
		this.flushChars = flushChars;
	}

	public Duration getMaxDelay() {
		return maxDelay;
	}

	public void setMaxDelay(Duration maxDelay) {
		this.maxDelay = maxDelay;
	}

	@Override
	public String toString() {
		return "SseCoalesceProperties{enabled=" + enabled + ", flushChars=" + flushChars + ", maxDelay=" + maxDelay
				+ '}';
	}

}

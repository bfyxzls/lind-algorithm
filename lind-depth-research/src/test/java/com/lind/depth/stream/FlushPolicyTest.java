package com.lind.depth.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link FlushPolicy} 参数校验与默认值测试。
 *
 * <p>
 * 策略本身很薄，但错误的 flushBytes / maxDelay 会让攒包行为「永远不刷」或「疯狂刷」。 这里锁死默认值与非法入参，避免回归时 silently 配错。
 */
class FlushPolicyTest {

	@Test
	@DisplayName("mtuDefault：1400 字节阈值 + 10ms 时间窗")
	void mtuDefaultIsSane() {
		FlushPolicy p = FlushPolicy.mtuDefault();
		// 1400 ≈ 以太网 MTU 1500 扣除常见头后的有效载荷量级（教学用近似值）
		assertThat(p.flushBytes()).isEqualTo(1400);
		assertThat(p.maxDelay()).isEqualTo(Duration.ofMillis(10));
	}

	@Test
	@DisplayName("非法参数应直接失败，而不是带着错误配置运行")
	void rejectsInvalidArgs() {
		assertThatThrownBy(() -> FlushPolicy.of(0, Duration.ofMillis(1))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> FlushPolicy.of(100, Duration.ZERO)).isInstanceOf(IllegalArgumentException.class);
	}

}

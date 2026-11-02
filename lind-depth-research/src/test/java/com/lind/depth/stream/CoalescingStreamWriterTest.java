package com.lind.depth.stream;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 场景一核心对照实验。
 *
 * <p>
 * <b>读这个测试前先读：</b>{@code docs/工程化分析.md} 场景一，以及 {@link PerTokenStreamWriter} /
 * {@link CoalescingStreamWriter} 类注释。
 *
 * <p>
 * 我们验证三件事：
 * <ol>
 * <li>玩具做法：flush 次数 = token 数（每片一刷）</li>
 * <li>工程做法：flush 次数显著更少（攒满阈值再刷）</li>
 * <li>两种做法写出的字节内容完全一致（只改「怎么发」，不改「发什么」）</li>
 * </ol>
 */
class CoalescingStreamWriterTest {

	/**
	 * 主实验：同样一段「切得很碎」的 token 流，对比 flush 次数与内容。
	 *
	 * <p>
	 * 步骤说明：
	 * <ol>
	 * <li>构造较长文本，按 3 字符切片 → 得到大量小 token（模拟流式吐字）</li>
	 * <li>用 {@link PerTokenStreamWriter} 推一遍，期望 flushCount == tokens.size()</li>
	 * <li>用 {@link CoalescingStreamWriter} 再推一遍；时间窗开到 60s，让本用例只测「字节阈值」</li>
	 * <li>断言攒包 flush 远小于逐 token，且两边字节内容相同</li>
	 * </ol>
	 */
	@Test
	@DisplayName("攒包写出的 flush 次数应远小于逐 token 写出，且内容一致")
	void coalescingFlushesFarLessThanPerToken() throws Exception {
		// ---------- 1. 准备「很多很小的」token ----------
		// repeat(20) 拉长文本；chunkSize=3 让片数变多，更容易拉开两种策略的 flush 差距
		String text = "工程级流式：数据保持流的形态，DirectBuffer 攒包后再写，避免每 token 一次系统调用。".repeat(20);
		List<String> tokens = TokenSource.chunk(text, 3);
		assertThat(tokens.size()).isGreaterThan(50);

		// ---------- 2. 玩具路径：每 token 立刻写出 ----------
		CountingByteSink naiveSink = new CountingByteSink(true); // true=保留内容，后面要比对正文
		PerTokenStreamWriter naive = new PerTokenStreamWriter(naiveSink);
		TokenSource.emit(tokens, t -> writeQuietly(naive, t));
		naive.close();

		// ---------- 3. 工程路径：DirectBuffer 攒到 1400 字节再刷 ----------
		// maxDelay=60s：本测试不考察时间窗，避免偶发超时干扰「纯字节阈值」结论
		CountingByteSink coalesceSink = new CountingByteSink(true);
		CoalescingStreamWriter coalesce = new CoalescingStreamWriter(coalesceSink,
				FlushPolicy.of(1400, Duration.ofSeconds(60)));
		TokenSource.emit(tokens, t -> writeQuietly(coalesce, t));
		coalesce.close(); // 关闭时刷出不足 1400 的尾包，否则内容会少一截

		// ---------- 4. 断言：成本下降、语义不变 ----------
		// 玩具：一次 token 一次 flush
		assertThat(naive.stats().flushCount()).isEqualTo(tokens.size());
		// 工程：至少比玩具少一个数量级的「一半」以上（阈值 1400、token 很小，通常只刷几次）
		assertThat(coalesce.stats().flushCount()).isLessThan(naive.stats().flushCount() / 5);
		// 总字节必须相同：攒包不是丢数据，只是合并发送
		assertThat(coalesce.stats().bytesWritten()).isEqualTo(naive.stats().bytesWritten());
		assertThat(new String(naiveSink.concatenated(), StandardCharsets.UTF_8)).isEqualTo(text);
		assertThat(new String(coalesceSink.concatenated(), StandardCharsets.UTF_8)).isEqualTo(text);
	}

	/**
	 * 补充实验：字节阈值故意设得极大，单靠「攒满」几乎不会触发； 睡眠超过时间窗后调用 {@link CoalescingStreamWriter#tick()}，应强制
	 * flush。
	 *
	 * <p>
	 * 对应文档「用时间窗口兜底」——否则稀疏 token 会一直卡在缓冲里，首包延迟失控。
	 */
	@Test
	@DisplayName("时间窗兜底：未满包但超时也会 flush")
	void timeWindowForcesFlush() throws Exception {
		CountingByteSink sink = new CountingByteSink();
		// flushBytes=1_000_000：单靠体积几乎刷不出去；靠 20ms 时间窗
		FlushPolicy policy = FlushPolicy.of(1_000_000, Duration.ofMillis(20));
		CoalescingStreamWriter writer = new CoalescingStreamWriter(sink, policy);

		writer.onToken("hello");
		// 刚写入，未超时 → 不应已刷
		assertThat(writer.stats().flushCount()).isZero();

		Thread.sleep(40); // 大于 20ms
		writer.tick(); // 模拟 EventLoop / 定时器空闲时检查时间窗
		assertThat(writer.stats().flushCount()).isEqualTo(1);

		writer.close();
	}

	/** 测试里把受检异常收成 RuntimeException，方便当 lambda 用。 */
	private static void writeQuietly(StreamWriter writer, String token) {
		try {
			writer.onToken(token);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}

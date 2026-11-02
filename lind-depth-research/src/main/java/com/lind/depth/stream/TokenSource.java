package com.lind.depth.stream;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 模拟大模型「流式吐 token」的数据源。
 *
 * <p>
 * <b>为什么需要它？</b><br>
 * 场景一研究的是「token 流 → 网络写出」之间的拷贝与系统调用成本，不依赖真实 GPU / 模型。 用可控文本切成小片，就能稳定复现「token
 * 很多、每片很小」时的写出风暴，方便对比两种 Writer。
 *
 * <p>
 * 阅读提示：先看 {@link #chunk(String, int)}，再看 {@link #emit(List, Consumer)}。
 */
public final class TokenSource {

	private TokenSource() {
	}

	/**
	 * 把整段文本切成固定宽度的伪 token（最后一片可以更短）。
	 * @param text 原始文本，不能为 null
	 * @param chunkSize 每片字符数，必须 &gt; 0；越小则 token 越多，越能拉开「逐 token flush」与「攒包」的差距
	 * @return 按顺序排列的 token 列表（可能为空列表，当 text 为空串时）
	 */
	public static List<String> chunk(String text, int chunkSize) {
		Objects.requireNonNull(text, "text");
		if (chunkSize <= 0) {
			throw new IllegalArgumentException("chunkSize 必须 > 0");
		}
		List<String> tokens = new ArrayList<>();
		for (int i = 0; i < text.length(); i += chunkSize) {
			tokens.add(text.substring(i, Math.min(i + chunkSize, text.length())));
		}
		return tokens;
	}

	/**
	 * 依次把每个 token 交给回调，模拟推理线程「一边生成一边往下游推」。
	 * @param tokens token 序列
	 * @param onToken 下游消费者（通常是 {@link StreamWriter#onToken(String)}）
	 */
	public static void emit(List<String> tokens, Consumer<String> onToken) {
		Objects.requireNonNull(tokens, "tokens");
		Objects.requireNonNull(onToken, "onToken");
		for (String token : tokens) {
			onToken.accept(token);
		}
	}

}

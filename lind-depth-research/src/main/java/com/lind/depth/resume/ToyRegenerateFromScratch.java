package com.lind.depth.resume;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <b>玩具做法（反面教材）</b>：断线后不看 Last-Event-ID，整段内容重新「生成」一遍。
 *
 * <p>
 * 用户会看到重复内容或「请重试」——文档里说的「重新来过」。 对比 {@link ResumeService} 的无感续推。
 */
public final class ToyRegenerateFromScratch {

	private ToyRegenerateFromScratch() {
	}

	/**
	 * 假装重连后服务端只会把完整 tokens 再吐一遍（无断点）。
	 */
	public static List<String> regenerateAll(List<String> originalTokens) {
		Objects.requireNonNull(originalTokens, "originalTokens");
		// 作用：一次"防御性拷贝"的双重保险
		// 这行代码的核心目的是做一次不可变的防御性拷贝（defensive copy），把调用方传进来的可变 List 变成一个"和外界彻底隔离、且不可修改"的 List 再返回
		return List.copyOf(
				new ArrayList<>(originalTokens) // ② 拷贝到新的可变 ArrayList
		); // ③ 生成不可变 List 返回
	}

}

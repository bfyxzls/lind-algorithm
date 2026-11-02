package com.lind.depth.stream;

import java.io.IOException;

/**
 * 流式 token 写出器统一接口。
 *
 * <p>
 * 两种实现形成对照（务必一起读）：
 * <ul>
 * <li>{@link PerTokenStreamWriter} —— 玩具：每 token 立刻刷</li>
 * <li>{@link CoalescingStreamWriter} —— 工程：DirectBuffer 攒包再刷</li>
 * </ul>
 *
 * <p>
 * 实现 {@link AutoCloseable}：关闭时必须刷出残留缓冲（攒包实现尤其重要）。
 */
public interface StreamWriter extends AutoCloseable {

	/**
	 * 收到一个 token。实现侧负责编码（本模块统一 UTF-8）并按自身策略写出。
	 * @param token 非 null；允许空串（仍计一次 token，但可能不产生字节）
	 */
	void onToken(String token) throws IOException;

	/** 累计统计，可在写出过程中随时读取。 */
	StreamWriteStats stats();

	/** 刷出缓冲并释放资源；可重复调用，第二次应为空操作。 */
	@Override
	void close() throws IOException;

}

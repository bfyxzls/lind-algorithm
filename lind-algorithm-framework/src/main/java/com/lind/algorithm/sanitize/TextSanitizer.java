package com.lind.algorithm.sanitize;

/**
 * 文本清洗抽象基类。每种实现对应一类业务清洗策略。
 * <p>
 * {@code null} 入参原样返回；子类只需实现 {@link #doSanitize(String)}。
 */
public abstract class TextSanitizer {

	/**
	 * 清洗类型标识，便于日志与管道编排。
	 */
	public abstract String type();

	/**
	 * 执行清洗。
	 */
	public final String sanitize(String input) {
		if (input == null) {
			return null;
		}
		return doSanitize(input);
	}

	/**
	 * 子类实现具体清洗逻辑；入参保证非 null。
	 */
	protected abstract String doSanitize(String input);

}

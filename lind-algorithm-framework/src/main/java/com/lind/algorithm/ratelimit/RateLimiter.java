package com.lind.algorithm.ratelimit;

/**
 * 限流器：尝试获取 1 个许可，成功则放行。
 */
public interface RateLimiter {

	/**
	 * @return true 表示允许通过
	 */
	boolean tryAcquire();

	/**
	 * 尝试获取 {@code permits} 个许可。
	 */
	boolean tryAcquire(int permits);

}

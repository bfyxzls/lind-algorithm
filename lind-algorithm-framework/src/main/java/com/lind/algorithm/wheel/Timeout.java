package com.lind.algorithm.wheel;

/**
 * 一次延时调度的句柄，支持取消。
 */
public interface Timeout {

	/**
	 * @return 所属时间轮定时器
	 */
	HashedWheelTimer timer();

	/**
	 * @return 关联任务
	 */
	TimerTask task();

	/**
	 * 尝试取消尚未执行的任务。
	 * @return {@code true} 取消成功；已到期或已取消时返回 {@code false}
	 */
	boolean cancel();

	/**
	 * @return 是否已取消
	 */
	boolean isCancelled();

	/**
	 * @return 是否已到期（含正在执行 / 已执行）
	 */
	boolean isExpired();

}

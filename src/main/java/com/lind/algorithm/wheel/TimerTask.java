package com.lind.algorithm.wheel;

/**
 * 时间轮延时任务。
 */
@FunctionalInterface
public interface TimerTask {

	/**
	 * 到期执行。
	 * @param timeout 本次调度句柄（可用于查询是否已取消等）
	 * @throws Exception 任务异常由时间轮捕获并交给 {@link HashedWheelTimer.TaskExceptionHandler}
	 */
	void run(Timeout timeout) throws Exception;

}

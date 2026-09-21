package com.lind.delay.task;

/**
 * 延时任务状态（持久层权威）。
 */
public enum DelayTaskStatus {

	PENDING,

	RUNNING,

	DONE,

	CANCELLED,

	DEAD

}

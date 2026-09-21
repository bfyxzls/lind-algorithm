package com.lind.delay.task;

@FunctionalInterface
public interface DelayTaskHandler {

	void handle(DelayTask task) throws Exception;

}

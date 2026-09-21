package com.lind.delay.task;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class DelayTaskHandlerRegistry {

	private final Map<String, DelayTaskHandler> handlers = new ConcurrentHashMap<>();

	public void register(String bizType, DelayTaskHandler handler) {
		handlers.put(bizType, handler);
	}

	public DelayTaskHandler get(String bizType) {
		return handlers.get(bizType);
	}

}

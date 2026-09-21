package com.lind.delay.task;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DemoDelayTaskHandlers {

	public static final String BIZ_TYPE_DEMO = "demo";

	private final DelayTaskHandlerRegistry registry;

	@PostConstruct
	void register() {
		registry.register(BIZ_TYPE_DEMO, task -> log.info("delay task fired: id={}, bizId={}, payload={}", task.getId(),
				task.getBizId(), task.getPayload()));
	}

}

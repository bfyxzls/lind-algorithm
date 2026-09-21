package com.lind.delay.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DelayTaskBootstrap implements ApplicationRunner {

	private final DelayTaskService delayTaskService;

	@Override
	public void run(ApplicationArguments args) {
		int n = delayTaskService.reloadFromStore();
		log.info("delay task bootstrap finished, loaded={}", n);
	}

}

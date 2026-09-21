package com.lind.delay.config;

import com.lind.algorithm.wheel.HashedWheelTimer;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WheelConfig {

	@Bean(destroyMethod = "stop")
	public HashedWheelTimer hashedWheelTimer() {
		return new HashedWheelTimer(10, 20, Executors.newFixedThreadPool(4));
	}

}

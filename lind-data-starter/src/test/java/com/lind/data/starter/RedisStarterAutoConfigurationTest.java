package com.lind.data.starter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.lind.data.starter.redis.LindSpringRedis;
import com.lind.data.starter.redis.RedisStarterAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisStarterAutoConfigurationTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(RedisStarterAutoConfiguration.class))
			.withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class));

	@Test
	void registersLindSpringRedisWhenEnabled() {
		runner.run(context -> assertThat(context).hasSingleBean(LindSpringRedis.class));
	}

	@Test
	void skipsWhenDisabled() {
		runner.withPropertyValues("lind.data.redis.enabled=false")
				.run(context -> assertThat(context).doesNotHaveBean(LindSpringRedis.class));
	}

}

package com.lind.data.starter.redis;

import com.lind.data.starter.LindDataProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

@AutoConfiguration
@ConditionalOnClass(StringRedisTemplate.class)
@ConditionalOnProperty(prefix = "lind.data.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(LindDataProperties.class)
public class RedisStarterAutoConfiguration {

	@Bean
	@ConditionalOnBean(StringRedisTemplate.class)
	@ConditionalOnMissingBean
	public LindSpringRedis lindSpringRedis(StringRedisTemplate stringRedisTemplate) {
		return new LindSpringRedis(stringRedisTemplate);
	}

}

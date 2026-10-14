package com.lind.data.starter.redis.redisson;

import com.lind.data.starter.LindDataProperties;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Redisson 场景自动配置：存在 {@link RedissonClient} 时装配 {@link LindRedisson}。
 * <p>
 * {@code RedissonClient} 通常由 {@code redisson-spring-boot-starter} 根据
 * {@code spring.data.redis.*} 自动创建。
 */
@AutoConfiguration
@ConditionalOnClass(RedissonClient.class)
@ConditionalOnProperty(prefix = "lind.data.redisson", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(LindDataProperties.class)
public class RedissonStarterAutoConfiguration {

	@Bean
	@ConditionalOnBean(RedissonClient.class)
	@ConditionalOnMissingBean
	public LindRedisson lindRedisson(RedissonClient redissonClient) {
		return new LindRedisson(redissonClient);
	}

}

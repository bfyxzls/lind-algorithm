package com.lind.data.starter.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class SpringRedisCacheTest {

	@Test
	void setAndGet() {
		StringRedisTemplate template = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		ValueOperations<String, String> ops = mock(ValueOperations.class);
		when(template.opsForValue()).thenReturn(ops);
		when(ops.get("k")).thenReturn("v");

		SpringRedisCache cache = new SpringRedisCache(template);
		cache.set("k", "v", Duration.ofSeconds(10));
		Optional<String> value = cache.get("k");

		assertThat(value).contains("v");
		verify(ops).set(eq("k"), eq("v"), eq(Duration.ofSeconds(10)));
	}

	@Test
	void tryLockUsesSetIfAbsent() {
		StringRedisTemplate template = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		ValueOperations<String, String> ops = mock(ValueOperations.class);
		when(template.opsForValue()).thenReturn(ops);
		when(ops.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

		SpringRedisLock lock = new SpringRedisLock(template, "lock:k", Duration.ofSeconds(5));
		assertThat(lock.tryLock()).isTrue();
	}

}

package com.lind.stream.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.server.WebFilter;

/**
 * WebFlux CORS 与 SSE 响应头。
 */
@Configuration
public class WebConfig implements WebFluxConfigurer {

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**").allowedOriginPatterns("*").allowedMethods("*").allowedHeaders("*");
	}

	@Bean
	public WebFilter sseHeadersFilter() {
		return (exchange, chain) -> {
			String path = exchange.getRequest().getPath().value();
			if (path.contains("/stream") || path.contains("/chat/completions")) {
				HttpHeaders headers = exchange.getResponse().getHeaders();
				headers.add("Cache-Control", "no-cache");// 可以缓存，但每次使用前必须回源验证；验证通过可用 304
															// 复用，不通过则取新内容
				headers.add("X-Accel-Buffering", "no");// 禁用对该响应的代理缓冲（proxy_buffering）
			}
			return chain.filter(exchange);
		};
	}

}

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
				headers.add("Cache-Control", "no-cache");
				headers.add("X-Accel-Buffering", "no");
			}
			return chain.filter(exchange);
		};
	}

}

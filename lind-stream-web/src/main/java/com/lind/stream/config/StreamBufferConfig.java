package com.lind.stream.config;

import com.lind.stream.buffer.SseCoalesceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SseCoalesceProperties.class)
public class StreamBufferConfig {

}

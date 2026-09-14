package com.lind.stream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 流式 Web 服务入口：演示 text/event-stream 逐段返回（类大模型接口）。
 */
@SpringBootApplication
public class StreamWebApplication {

	public static void main(String[] args) {
		SpringApplication.run(StreamWebApplication.class, args);
	}

}

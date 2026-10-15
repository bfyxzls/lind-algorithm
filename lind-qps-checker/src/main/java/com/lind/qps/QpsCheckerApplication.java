package com.lind.qps;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * QPS Checker Web 入口（FreeMarker 管理页）。
 */
@SpringBootApplication(scanBasePackages = "com.lind.qps")
public class QpsCheckerApplication {

	public static void main(String[] args) {
		SpringApplication.run(QpsCheckerApplication.class, args);
	}

}

package com.lind.qps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * {@link CliArgs} 解析测试。
 */
public class CliArgsTest {

	@Test
	void parseFixedMode() {
		CliArgs args = CliArgs.parse(new String[] { "--url", "http://localhost/x", "--concurrency", "20", "--duration",
				"15", "--method", "POST", "--body", "{}", "--header", "X-Token:abc" });
		LoadConfig config = args.toLoadConfig();
		assertEquals("http://localhost/x", config.url().toString());
		assertEquals(20, config.concurrency());
		assertEquals(15, config.duration().toSeconds());
		assertEquals("POST", config.method());
		assertEquals("{}", config.body());
		assertEquals("abc", config.headers().get("X-Token"));
	}

	@Test
	void parseRepeatedHeaderParamAndForm() {
		CliArgs args = CliArgs.parse(new String[] { "--url", "http://localhost/login", "--method", "POST", "--header",
				"Authorization:Bearer t", "--header", "X-Trace=1", "--param", "from=app", "--param", "lang=zh",
				"--form", "username=admin", "--form", "password=123" });
		LoadConfig config = args.toLoadConfig();
		assertEquals("Bearer t", config.headers().get("Authorization"));
		assertEquals("1", config.headers().get("X-Trace"));
		assertEquals("app", config.queryParams().get("from"));
		assertEquals("zh", config.queryParams().get("lang"));
		assertEquals("admin", config.formParams().get("username"));
		assertEquals("123", config.formParams().get("password"));
		assertTrue(config.requestUri().toString().contains("from=app"));
		assertTrue(config.requestUri().toString().contains("lang=zh"));
		assertEquals("username=admin&password=123", config.requestBody());
	}

	@Test
	void rejectsBodyTogetherWithForm() {
		CliArgs args = CliArgs.parse(
				new String[] { "--url", "http://localhost/x", "--method", "POST", "--body", "{}", "--form", "a=1" });
		assertThrows(IllegalArgumentException.class, args::toLoadConfig);
	}

	@Test
	void helpAndUsage() {
		assertTrue(CliArgs.parse(new String[] { "--help" }).help());
		assertTrue(CliArgs.usage().contains("--form"));
		assertTrue(CliArgs.usage().contains("--param"));
	}

}

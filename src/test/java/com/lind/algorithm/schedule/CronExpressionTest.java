package com.lind.algorithm.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * {@link CronExpression} 单元测试。
 */
public class CronExpressionTest {

	@Test
	void nextEveryMinute() {
		CronExpression cron = new CronExpression("* * * * *");
		LocalDateTime from = LocalDateTime.of(2026, 1, 1, 10, 15, 30);
		assertEquals(LocalDateTime.of(2026, 1, 1, 10, 16, 0), cron.next(from).orElseThrow());
	}

	@Test
	void matchesHourlyAtMinuteZero() {
		CronExpression cron = new CronExpression("0 * * * *");
		assertTrue(cron.matches(LocalDateTime.of(2026, 3, 1, 8, 0)));
		assertTrue(cron.next(LocalDateTime.of(2026, 3, 1, 8, 0)).orElseThrow().getMinute() == 0);
	}

}

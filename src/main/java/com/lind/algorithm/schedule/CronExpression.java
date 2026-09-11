package com.lind.algorithm.schedule;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;

/**
 * 简化 Cron 表达式（5 域）：分 时 日 月 周；支持星号、步进、列表、区间与单值。
 * <p>
 * 不含秒域与年域；日与周同时指定时，满足任一即触发（类 Unix 常见行为）。
 * </p>
 */
public final class CronExpression {

	private final String expression;

	private final Field minutes;

	private final Field hours;

	private final Field daysOfMonth;

	private final Field months;

	private final Field daysOfWeek;

	public CronExpression(String expression) {
		this.expression = Objects.requireNonNull(expression, "expression").trim();
		String[] parts = this.expression.split("\\s+");
		if (parts.length != 5) {
			throw new IllegalArgumentException("cron must have 5 fields: min hour day month weekday");
		}
		this.minutes = Field.parse(parts[0], 0, 59);
		this.hours = Field.parse(parts[1], 0, 23);
		this.daysOfMonth = Field.parse(parts[2], 1, 31);
		this.months = Field.parse(parts[3], 1, 12);
		this.daysOfWeek = Field.parse(parts[4], 0, 6); // 0=Sun ... 6=Sat
	}

	public String expression() {
		return expression;
	}

	/**
	 * 计算 {@code fromExclusive} 之后的下一次触发时间。
	 */
	public Optional<LocalDateTime> next(LocalDateTime fromExclusive) {
		Objects.requireNonNull(fromExclusive, "fromExclusive");
		LocalDateTime cursor = fromExclusive.withNano(0).plusMinutes(1).withSecond(0);
		for (int i = 0; i < 366 * 24 * 60; i++) {
			if (matches(cursor)) {
				return Optional.of(cursor);
			}
			cursor = cursor.plusMinutes(1);
		}
		return Optional.empty();
	}

	public boolean matches(LocalDateTime time) {
		Objects.requireNonNull(time, "time");
		if (!minutes.contains(time.getMinute()) || !hours.contains(time.getHour()) || !months.contains(time.getMonthValue())) {
			return false;
		}
		boolean dayMatch = daysOfMonth.contains(time.getDayOfMonth());
		boolean weekMatch = daysOfWeek.contains(toCronDow(time.getDayOfWeek()));
		boolean dayStar = daysOfMonth.isStar();
		boolean weekStar = daysOfWeek.isStar();
		if (dayStar && weekStar) {
			return true;
		}
		if (!dayStar && !weekStar) {
			return dayMatch || weekMatch;
		}
		return dayStar ? weekMatch : dayMatch;
	}

	private static int toCronDow(DayOfWeek dayOfWeek) {
		return dayOfWeek == DayOfWeek.SUNDAY ? 0 : dayOfWeek.getValue(); // Mon=1..Sat=6
	}

	static final class Field {

		private final TreeSet<Integer> values;

		private final boolean star;

		private Field(TreeSet<Integer> values, boolean star) {
			this.values = values;
			this.star = star;
		}

		static Field parse(String token, int min, int max) {
			TreeSet<Integer> set = new TreeSet<>();
			boolean star = false;
			for (String part : token.split(",")) {
				part = part.trim();
				if (part.equals("*")) {
					star = true;
					for (int i = min; i <= max; i++) {
						set.add(i);
					}
					continue;
				}
				if (part.startsWith("*/")) {
					int step = Integer.parseInt(part.substring(2));
					if (step <= 0) {
						throw new IllegalArgumentException("invalid step: " + part);
					}
					star = true;
					for (int i = min; i <= max; i += step) {
						set.add(i);
					}
					continue;
				}
				if (part.contains("-")) {
					String[] range = part.split("-");
					int start = Integer.parseInt(range[0]);
					int end = Integer.parseInt(range[1]);
					for (int i = start; i <= end; i++) {
						add(set, i, min, max);
					}
					continue;
				}
				add(set, Integer.parseInt(part), min, max);
			}
			return new Field(set, star || token.equals("*") || token.startsWith("*/"));
		}

		private static void add(TreeSet<Integer> set, int value, int min, int max) {
			if (value < min || value > max) {
				throw new IllegalArgumentException("value out of range: " + value);
			}
			set.add(value);
		}

		boolean contains(int value) {
			return values.contains(value);
		}

		boolean isStar() {
			return star;
		}

		List<Integer> values() {
			return Collections.unmodifiableList(new ArrayList<>(values));
		}

	}

}

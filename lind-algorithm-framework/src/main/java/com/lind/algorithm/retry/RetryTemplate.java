package com.lind.algorithm.retry;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * 重试模板：支持最大次数、指数退避与抖动；失败判定可自定义。
 */
public final class RetryTemplate {

	private final int maxAttempts;

	private final long initialBackoffMillis;

	private final double multiplier;

	private final long maxBackoffMillis;

	private final boolean jitter;

	private final Predicate<Throwable> retryOn;

	private RetryTemplate(Builder builder) {
		this.maxAttempts = builder.maxAttempts;
		this.initialBackoffMillis = builder.initialBackoffMillis;
		this.multiplier = builder.multiplier;
		this.maxBackoffMillis = builder.maxBackoffMillis;
		this.jitter = builder.jitter;
		this.retryOn = builder.retryOn;
	}

	public static Builder builder() {
		return new Builder();
	}

	public <T> T execute(Callable<T> action) throws Exception {
		Objects.requireNonNull(action, "action");
		Exception last = null;
		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				return action.call();
			}
			catch (Exception ex) {
				last = ex;
				if (attempt >= maxAttempts || !retryOn.test(ex)) {
					throw ex;
				}
				sleep(backoffMillis(attempt));
			}
		}
		throw last;
	}

	public void execute(Runnable action) throws Exception {
		execute(() -> {
			action.run();
			return null;
		});
	}

	long backoffMillis(int attempt) {
		double value = initialBackoffMillis * Math.pow(multiplier, attempt - 1);
		long capped = Math.min(maxBackoffMillis, (long) value);
		if (!jitter || capped <= 1) {
			return capped;
		}
		return ThreadLocalRandom.current().nextLong(capped / 2, capped + 1);
	}

	private static void sleep(long millis) throws InterruptedException {
		if (millis > 0) {
			Thread.sleep(millis);
		}
	}

	public static final class Builder {

		private int maxAttempts = 3;

		private long initialBackoffMillis = 100;

		private double multiplier = 2.0;

		private long maxBackoffMillis = TimeUnit.SECONDS.toMillis(30);

		private boolean jitter = true;

		private Predicate<Throwable> retryOn = t -> true;

		public Builder maxAttempts(int maxAttempts) {
			if (maxAttempts <= 0) {
				throw new IllegalArgumentException("maxAttempts must be > 0");
			}
			this.maxAttempts = maxAttempts;
			return this;
		}

		public Builder initialBackoff(long duration, TimeUnit unit) {
			Objects.requireNonNull(unit, "unit");
			long millis = unit.toMillis(duration);
			if (millis < 0) {
				throw new IllegalArgumentException("initialBackoff must be >= 0");
			}
			this.initialBackoffMillis = millis;
			return this;
		}

		public Builder multiplier(double multiplier) {
			if (multiplier < 1.0) {
				throw new IllegalArgumentException("multiplier must be >= 1");
			}
			this.multiplier = multiplier;
			return this;
		}

		public Builder maxBackoff(long duration, TimeUnit unit) {
			Objects.requireNonNull(unit, "unit");
			this.maxBackoffMillis = unit.toMillis(duration);
			return this;
		}

		public Builder jitter(boolean jitter) {
			this.jitter = jitter;
			return this;
		}

		public Builder retryOn(Predicate<Throwable> retryOn) {
			this.retryOn = Objects.requireNonNull(retryOn, "retryOn");
			return this;
		}

		public RetryTemplate build() {
			return new RetryTemplate(this);
		}

	}

}

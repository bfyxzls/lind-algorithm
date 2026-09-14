package com.lind.algorithm.circuit;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * 熔断器：Closed → Open → Half-Open 三态，失败次数达阈值后开闸，冷却后试探恢复。
 */
public final class CircuitBreaker {

	public enum State {

		CLOSED, OPEN, HALF_OPEN

	}

	private final int failureThreshold;

	private final int successThreshold;

	private final long openWaitNanos;

	private final LongSupplier nanoClock;

	private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);

	private final AtomicInteger consecutiveFailures = new AtomicInteger();

	private final AtomicInteger consecutiveSuccesses = new AtomicInteger();

	private final AtomicLong openedAt = new AtomicLong();

	public CircuitBreaker(int failureThreshold, int successThreshold, long openWait, TimeUnit unit) {
		this(failureThreshold, successThreshold, openWait, unit, System::nanoTime);
	}

	CircuitBreaker(int failureThreshold, int successThreshold, long openWait, TimeUnit unit, LongSupplier nanoClock) {
		if (failureThreshold <= 0 || successThreshold <= 0) {
			throw new IllegalArgumentException("thresholds must be > 0");
		}
		Objects.requireNonNull(unit, "unit");
		long nanos = unit.toNanos(openWait);
		if (nanos <= 0) {
			throw new IllegalArgumentException("openWait must be > 0");
		}
		this.failureThreshold = failureThreshold;
		this.successThreshold = successThreshold;
		this.openWaitNanos = nanos;
		this.nanoClock = Objects.requireNonNull(nanoClock, "nanoClock");
	}

	public State getState() {
		State current = state.get();
		if (current == State.OPEN && canAttemptReset()) {
			state.compareAndSet(State.OPEN, State.HALF_OPEN);
			consecutiveSuccesses.set(0);
			return state.get();
		}
		return state.get();
	}

	public boolean allowRequest() {
		return getState() != State.OPEN;
	}

	public <T> T execute(Supplier<T> supplier) {
		Objects.requireNonNull(supplier, "supplier");
		if (!allowRequest()) {
			throw new CircuitOpenException("circuit is OPEN");
		}
		try {
			T result = supplier.get();
			onSuccess();
			return result;
		}
		catch (RuntimeException ex) {
			onFailure();
			throw ex;
		}
	}

	public void execute(Runnable runnable) {
		execute(() -> {
			runnable.run();
			return null;
		});
	}

	public void onSuccess() {
		State current = getState();
		consecutiveFailures.set(0);
		if (current == State.HALF_OPEN) {
			if (consecutiveSuccesses.incrementAndGet() >= successThreshold) {
				state.set(State.CLOSED);
				consecutiveSuccesses.set(0);
			}
		}
	}

	public void onFailure() {
		State current = getState();
		consecutiveSuccesses.set(0);
		if (current == State.HALF_OPEN) {
			tripOpen();
			return;
		}
		if (current == State.CLOSED && consecutiveFailures.incrementAndGet() >= failureThreshold) {
			tripOpen();
		}
	}

	public void reset() {
		state.set(State.CLOSED);
		consecutiveFailures.set(0);
		consecutiveSuccesses.set(0);
	}

	private void tripOpen() {
		openedAt.set(nanoClock.getAsLong());
		state.set(State.OPEN);
		consecutiveFailures.set(0);
		consecutiveSuccesses.set(0);
	}

	private boolean canAttemptReset() {
		return nanoClock.getAsLong() - openedAt.get() >= openWaitNanos;
	}

}

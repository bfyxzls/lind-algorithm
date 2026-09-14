package com.lind.algorithm.fsm;

import java.util.Objects;
import java.util.Optional;

/**
 * 一条状态转换规则：from + event → to，可选守卫与动作。
 */
final class Transition<S, E, C> {

	private final S from;

	private final E event;

	private final S to;

	private final TransitionGuard<C> guard;

	private final TransitionAction<S, E, C> action;

	Transition(S from, E event, S to, TransitionGuard<C> guard, TransitionAction<S, E, C> action) {
		this.from = Objects.requireNonNull(from, "from");
		this.event = Objects.requireNonNull(event, "event");
		this.to = Objects.requireNonNull(to, "to");
		this.guard = guard;
		this.action = action;
	}

	S from() {
		return from;
	}

	E event() {
		return event;
	}

	S to() {
		return to;
	}

	boolean isAllowed(C context) {
		return guard == null || guard.evaluate(context);
	}

	Optional<TransitionAction<S, E, C>> action() {
		return Optional.ofNullable(action);
	}

}

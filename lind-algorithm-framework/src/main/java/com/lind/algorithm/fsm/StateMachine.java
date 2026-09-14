package com.lind.algorithm.fsm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

/**
 * 有限状态机（FSM），风格对齐业界常用的 Fluent API（Stateless4j / Squirrel）。
 * <p>
 * 特性：
 * <ul>
 * <li>状态 / 事件 / 上下文三元组</li>
 * <li>转换规则：{@code from + event → to}</li>
 * <li>可选 Guard、Transition Action、onEntry / onExit</li>
 * <li>监听器：转换成功回调</li>
 * </ul>
 * 线程安全：单实例默认非线程安全；多线程请自行加锁或每线程一实例。
 * </p>
 *
 * @param <S> 状态类型（常用枚举）
 * @param <E> 事件类型（常用枚举）
 * @param <C> 业务上下文
 */
public final class StateMachine<S, E, C> {

	private final Map<S, Map<E, List<Transition<S, E, C>>>> transitionTable;

	private final Map<S, List<StateAction<S, C>>> entryActions;

	private final Map<S, List<StateAction<S, C>>> exitActions;

	private final List<BiConsumer<TransitionEvent<S, E, C>, C>> listeners = new CopyOnWriteArrayList<>();

	private S currentState;

	private C context;

	private StateMachine(S initialState, C context, Map<S, Map<E, List<Transition<S, E, C>>>> transitionTable,
			Map<S, List<StateAction<S, C>>> entryActions, Map<S, List<StateAction<S, C>>> exitActions) {
		this.currentState = Objects.requireNonNull(initialState, "initialState");
		this.context = context;
		this.transitionTable = transitionTable;
		this.entryActions = entryActions;
		this.exitActions = exitActions;
	}

	public static <S, E, C> Builder<S, E, C> builder() {
		return new Builder<>();
	}

	public S getState() {
		return currentState;
	}

	public C getContext() {
		return context;
	}

	public void setContext(C context) {
		this.context = context;
	}

	/**
	 * 当前状态是否可响应该事件（含守卫判断）。
	 */
	public boolean canFire(E event) {
		Objects.requireNonNull(event, "event");
		List<Transition<S, E, C>> candidates = findTransitions(currentState, event);
		for (Transition<S, E, C> t : candidates) {
			if (t.isAllowed(context)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 触发事件并尝试转换；无匹配或守卫失败时抛出 {@link TransitionException}。
	 */
	public S fire(E event) {
		Objects.requireNonNull(event, "event");
		List<Transition<S, E, C>> candidates = findTransitions(currentState, event);
		if (candidates.isEmpty()) {
			throw new TransitionException(currentState, event,
					"No transition defined for state=" + currentState + ", event=" + event);
		}
		Transition<S, E, C> matched = null;
		for (Transition<S, E, C> t : candidates) {
			if (t.isAllowed(context)) {
				matched = t;
				break;
			}
		}
		if (matched == null) {
			throw new TransitionException(currentState, event,
					"All transitions rejected by guard for state=" + currentState + ", event=" + event);
		}

		S from = currentState;
		S to = matched.to();
		fireExit(from);
		matched.action().ifPresent(a -> a.execute(from, to, event, context));
		currentState = to;
		fireEntry(to);
		TransitionEvent<S, E, C> te = new TransitionEvent<>(from, to, event);
		for (BiConsumer<TransitionEvent<S, E, C>, C> listener : listeners) {
			listener.accept(te, context);
		}
		return currentState;
	}

	/**
	 * 尝试触发事件；成功返回 true，失败返回 false（不抛异常）。
	 */
	public boolean tryFire(E event) {
		try {
			fire(event);
			return true;
		}
		catch (TransitionException ex) {
			return false;
		}
	}

	/**
	 * 注册转换成功监听器。
	 */
	public void addTransitionListener(BiConsumer<TransitionEvent<S, E, C>, C> listener) {
		listeners.add(Objects.requireNonNull(listener, "listener"));
	}

	private List<Transition<S, E, C>> findTransitions(S state, E event) {
		Map<E, List<Transition<S, E, C>>> byEvent = transitionTable.get(state);
		if (byEvent == null) {
			return Collections.emptyList();
		}
		List<Transition<S, E, C>> list = byEvent.get(event);
		return list == null ? Collections.emptyList() : list;
	}

	private void fireEntry(S state) {
		List<StateAction<S, C>> actions = entryActions.get(state);
		if (actions != null) {
			for (StateAction<S, C> action : actions) {
				action.execute(state, context);
			}
		}
	}

	private void fireExit(S state) {
		List<StateAction<S, C>> actions = exitActions.get(state);
		if (actions != null) {
			for (StateAction<S, C> action : actions) {
				action.execute(state, context);
			}
		}
	}

	/**
	 * 一次成功转换的事件快照。
	 */
	public record TransitionEvent<S, E, C> (S from, S to, E event) {
	}

	/**
	 * Fluent 构建器。
	 */
	public static final class Builder<S, E, C> {

		private S initialState;

		private C context;

		private final Map<S, Map<E, List<Transition<S, E, C>>>> transitions = new LinkedHashMap<>();

		private final Map<S, List<StateAction<S, C>>> entryActions = new HashMap<>();

		private final Map<S, List<StateAction<S, C>>> exitActions = new HashMap<>();

		private Builder() {
		}

		public Builder<S, E, C> initial(S state) {
			this.initialState = Objects.requireNonNull(state, "state");
			return this;
		}

		public Builder<S, E, C> context(C context) {
			this.context = context;
			return this;
		}

		public FromClause from(S state) {
			return new FromClause(Objects.requireNonNull(state, "state"));
		}

		public Builder<S, E, C> onEntry(S state, StateAction<S, C> action) {
			entryActions.computeIfAbsent(state, k -> new ArrayList<>()).add(Objects.requireNonNull(action));
			return this;
		}

		public Builder<S, E, C> onExit(S state, StateAction<S, C> action) {
			exitActions.computeIfAbsent(state, k -> new ArrayList<>()).add(Objects.requireNonNull(action));
			return this;
		}

		public StateMachine<S, E, C> build() {
			if (initialState == null) {
				throw new IllegalStateException("initial state is required");
			}
			return new StateMachine<>(initialState, context, freeze(transitions), freezeActions(entryActions),
					freezeActions(exitActions));
		}

		private void addTransition(Transition<S, E, C> transition) {
			transitions.computeIfAbsent(transition.from(), k -> new LinkedHashMap<>())
					.computeIfAbsent(transition.event(), k -> new ArrayList<>()).add(transition);
		}

		private static <S, E, C> Map<S, Map<E, List<Transition<S, E, C>>>> freeze(
				Map<S, Map<E, List<Transition<S, E, C>>>> source) {
			Map<S, Map<E, List<Transition<S, E, C>>>> copy = new HashMap<>();
			for (Map.Entry<S, Map<E, List<Transition<S, E, C>>>> e : source.entrySet()) {
				Map<E, List<Transition<S, E, C>>> inner = new HashMap<>();
				for (Map.Entry<E, List<Transition<S, E, C>>> ie : e.getValue().entrySet()) {
					inner.put(ie.getKey(), List.copyOf(ie.getValue()));
				}
				copy.put(e.getKey(), Collections.unmodifiableMap(inner));
			}
			return Collections.unmodifiableMap(copy);
		}

		private static <S, C> Map<S, List<StateAction<S, C>>> freezeActions(Map<S, List<StateAction<S, C>>> source) {
			Map<S, List<StateAction<S, C>>> copy = new HashMap<>();
			for (Map.Entry<S, List<StateAction<S, C>>> e : source.entrySet()) {
				copy.put(e.getKey(), List.copyOf(e.getValue()));
			}
			return Collections.unmodifiableMap(copy);
		}

		public final class FromClause {

			private final S from;

			private FromClause(S from) {
				this.from = from;
			}

			public OnClause on(E event) {
				return new OnClause(from, Objects.requireNonNull(event, "event"));
			}

			public FromClause from(S state) {
				return Builder.this.from(state);
			}

			public Builder<S, E, C> and() {
				return Builder.this;
			}

			public StateMachine<S, E, C> build() {
				return Builder.this.build();
			}

		}

		public final class OnClause {

			private final S from;

			private final E event;

			private TransitionGuard<C> guard;

			private TransitionAction<S, E, C> action;

			private OnClause(S from, E event) {
				this.from = from;
				this.event = event;
			}

			public OnClause when(TransitionGuard<C> guard) {
				this.guard = Objects.requireNonNull(guard, "guard");
				return this;
			}

			public OnClause action(TransitionAction<S, E, C> action) {
				this.action = Objects.requireNonNull(action, "action");
				return this;
			}

			public FromClause to(S target) {
				Builder.this.addTransition(new Transition<>(from, event, target, guard, action));
				return new FromClause(from);
			}

		}

	}

}

package com.lind.algorithm.fsm;

/**
 * 进入 / 离开状态时的动作。
 *
 * @param <S> 状态
 * @param <C> 上下文
 */
@FunctionalInterface
public interface StateAction<S, C> {

	void execute(S state, C context);

}

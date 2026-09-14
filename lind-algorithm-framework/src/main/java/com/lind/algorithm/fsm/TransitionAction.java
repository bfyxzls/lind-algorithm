package com.lind.algorithm.fsm;

/**
 * 状态转换动作：在成功转换时执行。
 *
 * @param <S> 状态
 * @param <E> 事件
 * @param <C> 上下文
 */
@FunctionalInterface
public interface TransitionAction<S, E, C> {

	void execute(S from, S to, E event, C context);

}

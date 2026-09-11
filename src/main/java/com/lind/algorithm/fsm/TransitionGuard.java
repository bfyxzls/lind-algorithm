package com.lind.algorithm.fsm;

/**
 * 转换守卫：返回 true 时允许转换。
 *
 * @param <C> 上下文
 */
@FunctionalInterface
public interface TransitionGuard<C> {

	boolean evaluate(C context);

}

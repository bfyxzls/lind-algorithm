package com.lind.algorithm.fsm;

/**
 * 非法或被守卫拒绝的状态转换。
 */
public class TransitionException extends RuntimeException {

	private final Object from;

	private final Object event;

	public TransitionException(Object from, Object event, String message) {
		super(message);
		this.from = from;
		this.event = event;
	}

	public Object getFrom() {
		return from;
	}

	public Object getEvent() {
		return event;
	}

}

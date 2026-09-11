package com.lind.algorithm.circuit;

/**
 * 熔断器处于 OPEN 且不允许请求时抛出。
 */
public class CircuitOpenException extends RuntimeException {

	public CircuitOpenException(String message) {
		super(message);
	}

}

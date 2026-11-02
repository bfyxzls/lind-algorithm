package com.lind.architecture.util;

/**
 * 知识点 (1)：Fluent Builder 的统一收口——{@code build()} 产出不可变对象。
 */
@FunctionalInterface
public interface ObjectBuilder<T> {

	T build();

}

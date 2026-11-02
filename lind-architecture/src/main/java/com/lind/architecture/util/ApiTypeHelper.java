package com.lind.architecture.util;

import java.util.Objects;
import java.util.function.Function;

/**
 * 知识点 (1)：把 {@code Function<Builder, ObjectBuilder<T>>} 收成最终对象，形成内嵌 DSL。
 *
 * <pre>{@code
 * client.search(s -> s.index("docs").query(q -> q.term(t -> t.field("status").value("OPEN")));
 * }</pre>
 */
public final class ApiTypeHelper {

	private ApiTypeHelper() {
	}

	public static <B extends ObjectBuilder<T>, T> T resolve(Function<B, ObjectBuilder<T>> fn, B builder) {
		Objects.requireNonNull(fn, "fn");
		Objects.requireNonNull(builder, "builder");
		return Objects.requireNonNull(fn.apply(builder), "builder result").build();
	}

	public static <T> T requireNonNull(T value, String name) {
		return Objects.requireNonNull(value, name + " is required");
	}

}

package com.lind.architecture.codegen;

import com.lind.architecture.transport.Endpoint;
import com.lind.architecture.transport.HttpMethod;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 知识点 (5)：玩具「代码生成」——从规格产出 Endpoint，而不是手写 HTTP。
 * <p>
 * 生产里通常是 OpenAPI → 生成 Java 源文件；这里在运行时从 {@link ApiSpec} 构造 Endpoint， 证明「API
 * 面由规格驱动，Transport 保持稳定」。
 */
public final class SpecDrivenEndpoints {

	private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^}/]+)}");

	private SpecDrivenEndpoints() {
	}

	/**
	 * @param pathParams 从请求对象抽取路径参数的函数（教学简化：直接传 Map）
	 */
	@SuppressWarnings("unchecked")
	public static <Req, Res> Endpoint<Req, Res> fromSpec(ApiSpec spec,
			java.util.function.Function<Req, Map<String, String>> pathParams) {
		Objects.requireNonNull(spec, "spec");
		Objects.requireNonNull(pathParams, "pathParams");
		Class<Res> responseType = (Class<Res>) spec.responseType();
		return new Endpoint<>() {
			@Override
			public String id() {
				return spec.id();
			}

			@Override
			public HttpMethod method() {
				return spec.method();
			}

			@Override
			public String pathTemplate() {
				return spec.pathTemplate();
			}

			@Override
			public String path(Req request) {
				return render(spec.pathTemplate(), pathParams.apply(request));
			}

			@Override
			public boolean hasRequestBody() {
				return spec.hasBody();
			}

			@Override
			public Class<Res> responseType() {
				return responseType;
			}
		};
	}

	static String render(String template, Map<String, String> params) {
		Matcher m = PLACEHOLDER.matcher(template);
		StringBuilder sb = new StringBuilder();
		while (m.find()) {
			String key = m.group(1);
			String value = params.get(key);
			if (value == null) {
				throw new IllegalArgumentException("missing path param: " + key);
			}
			m.appendReplacement(sb, Matcher.quoteReplacement(value));
		}
		m.appendTail(sb);
		return sb.toString();
	}

}

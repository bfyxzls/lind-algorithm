package com.lind.architecture.transport;

import java.util.Map;

/**
 * 知识点 (2)/(5)：把「URL + 方法 + 路径参数 + 请求/响应类型」统一建模。
 * <p>
 * 上层几百个 API，底层只认 Endpoint；新增 API = 注册 Endpoint，而不是再写一套 HTTP。
 *
 * @param <Req> 请求类型
 * @param <Res> 响应类型
 */
public interface Endpoint<Req, Res> {

	String id();

	HttpMethod method();

	/** 可用 {@code {name}} 占位，由 {@link #path(Object)} 填充。 */
	String pathTemplate();

	String path(Req request);

	default Map<String, String> queryParameters(Req request) {
		return Map.of();
	}

	boolean hasRequestBody();

	Class<Res> responseType();

}

package com.lind.architecture.codegen;

import com.lind.architecture.transport.HttpMethod;

/**
 * 知识点 (5)：规格条目——真实世界对应 OpenAPI operation；这里用 record 模拟「规格源」。
 */
public record ApiSpec(String id, HttpMethod method, String pathTemplate, boolean hasBody, Class<?> responseType) {
}

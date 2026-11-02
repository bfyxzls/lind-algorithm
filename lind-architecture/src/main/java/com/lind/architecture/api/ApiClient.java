package com.lind.architecture.api;

import com.lind.architecture.json.JsonMapper;
import com.lind.architecture.transport.Endpoint;
import com.lind.architecture.transport.Transport;
import com.lind.architecture.transport.TransportException;
import com.lind.architecture.util.ApiTypeHelper;
import com.lind.architecture.util.ObjectBuilder;

import java.util.Objects;
import java.util.function.Function;

/**
 * 所有命名空间 Client 的公共基类：持有 Transport + JsonMapper。
 */
public abstract class ApiClient {

	protected final Transport transport;

	protected final JsonMapper jsonMapper;

	protected ApiClient(Transport transport, JsonMapper jsonMapper) {
		this.transport = Objects.requireNonNull(transport, "transport");
		this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
	}

	protected <Req, Res> Res perform(Endpoint<Req, Res> endpoint, Req request) {
		try {
			return transport.performRequest(endpoint, request, jsonMapper);
		}
		catch (TransportException e) {
			throw new RuntimeException(e);
		}
	}

	protected <B extends ObjectBuilder<Req>, Req, Res> Res perform(Endpoint<Req, Res> endpoint,
			Function<B, ObjectBuilder<Req>> fn, B builder) {
		return perform(endpoint, ApiTypeHelper.resolve(fn, builder));
	}

}

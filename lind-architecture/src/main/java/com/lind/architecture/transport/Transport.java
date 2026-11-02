package com.lind.architecture.transport;

import com.lind.architecture.json.JsonMapper;

import java.util.concurrent.CompletableFuture;

/**
 * 知识点 (2)/(6)：稳定内核——同步 + 异步共用同一套 Endpoint 管道。
 */
public interface Transport extends AutoCloseable {

	<Req, Res> Res performRequest(Endpoint<Req, Res> endpoint, Req request, JsonMapper mapper)
			throws TransportException;

	default <Req, Res> CompletableFuture<Res> performRequestAsync(Endpoint<Req, Res> endpoint, Req request,
			JsonMapper mapper) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return performRequest(endpoint, request, mapper);
			}
			catch (TransportException e) {
				throw new java.util.concurrent.CompletionException(e);
			}
		});
	}

	@Override
	default void close() {
	}

}

package com.lind.architecture.api;

import com.lind.architecture.api.documents.GetRequest;
import com.lind.architecture.api.documents.GetResponse;
import com.lind.architecture.api.documents.IndexRequest;
import com.lind.architecture.api.documents.IndexResponse;
import com.lind.architecture.api.search.SearchRequest;
import com.lind.architecture.api.search.SearchResponse;
import com.lind.architecture.json.JsonMapper;
import com.lind.architecture.transport.Transport;
import com.lind.architecture.transport.TransportException;
import com.lind.architecture.util.ApiTypeHelper;
import com.lind.architecture.util.ObjectBuilder;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * 知识点 (6)：异步外观——同一套 Endpoint / 模型，返回 {@link CompletableFuture}。
 */
public final class ToyAsyncClient {

	private final Transport transport;

	private final JsonMapper jsonMapper;

	public ToyAsyncClient(Transport transport, JsonMapper jsonMapper) {
		this.transport = Objects.requireNonNull(transport, "transport");
		this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
	}

	public CompletableFuture<IndexResponse> index(Function<IndexRequest.Builder, ObjectBuilder<IndexRequest>> fn) {
		IndexRequest req = ApiTypeHelper.resolve(fn, new IndexRequest.Builder());
		return transport.performRequestAsync(Endpoints.DOCUMENTS_INDEX, req, jsonMapper).exceptionally(this::rethrow);
	}

	public CompletableFuture<GetResponse> get(Function<GetRequest.Builder, ObjectBuilder<GetRequest>> fn) {
		GetRequest req = ApiTypeHelper.resolve(fn, new GetRequest.Builder());
		return transport.performRequestAsync(Endpoints.DOCUMENTS_GET, req, jsonMapper).exceptionally(this::rethrow);
	}

	public CompletableFuture<SearchResponse> search(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> fn) {
		SearchRequest req = ApiTypeHelper.resolve(fn, new SearchRequest.Builder());
		return transport.performRequestAsync(Endpoints.SEARCH, req, jsonMapper).exceptionally(this::rethrow);
	}

	private <T> T rethrow(Throwable t) {
		Throwable c = t instanceof java.util.concurrent.CompletionException && t.getCause() != null ? t.getCause() : t;
		if (c instanceof TransportException te) {
			throw new RuntimeException(te);
		}
		if (c instanceof RuntimeException re) {
			throw re;
		}
		throw new RuntimeException(c);
	}

}

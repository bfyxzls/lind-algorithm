package com.lind.architecture.api.search;

import com.lind.architecture.api.ApiClient;
import com.lind.architecture.api.Endpoints;
import com.lind.architecture.json.JsonMapper;
import com.lind.architecture.transport.Transport;
import com.lind.architecture.util.ObjectBuilder;

import java.util.function.Function;

/**
 * 知识点 (7)：search 命名空间。
 */
public final class SearchNamespace extends ApiClient {

	public SearchNamespace(Transport transport, JsonMapper jsonMapper) {
		super(transport, jsonMapper);
	}

	public SearchResponse search(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> fn) {
		return perform(Endpoints.SEARCH, fn, new SearchRequest.Builder());
	}

}

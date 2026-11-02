package com.lind.architecture.api.documents;

import com.lind.architecture.api.ApiClient;
import com.lind.architecture.api.Endpoints;
import com.lind.architecture.json.JsonMapper;
import com.lind.architecture.transport.Transport;
import com.lind.architecture.util.ObjectBuilder;

import java.util.function.Function;

/**
 * 知识点 (7)：documents 命名空间——按领域拆门面，避免上帝 Client。
 */
public final class DocumentsNamespace extends ApiClient {

	public DocumentsNamespace(Transport transport, JsonMapper jsonMapper) {
		super(transport, jsonMapper);
	}

	public IndexResponse index(Function<IndexRequest.Builder, ObjectBuilder<IndexRequest>> fn) {
		return perform(Endpoints.DOCUMENTS_INDEX, fn, new IndexRequest.Builder());
	}

	public GetResponse get(Function<GetRequest.Builder, ObjectBuilder<GetRequest>> fn) {
		return perform(Endpoints.DOCUMENTS_GET, fn, new GetRequest.Builder());
	}

}

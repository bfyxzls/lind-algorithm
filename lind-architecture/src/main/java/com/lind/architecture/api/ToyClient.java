package com.lind.architecture.api;

import com.lind.architecture.api.documents.DocumentsNamespace;
import com.lind.architecture.api.search.SearchNamespace;
import com.lind.architecture.api.search.SearchRequest;
import com.lind.architecture.api.search.SearchResponse;
import com.lind.architecture.json.JacksonJsonMapper;
import com.lind.architecture.json.JsonMapper;
import com.lind.architecture.transport.InMemoryTransport;
import com.lind.architecture.transport.Transport;
import com.lind.architecture.util.ObjectBuilder;

import java.util.Objects;
import java.util.function.Function;

/**
 * 知识点 (6)/(7)：同步门面 + 命名空间入口（对照 {@code ElasticsearchClient}）。
 */
public final class ToyClient implements AutoCloseable {

	private final Transport transport;

	private final JsonMapper jsonMapper;

	private final DocumentsNamespace documents;

	private final SearchNamespace search;

	public ToyClient(Transport transport, JsonMapper jsonMapper) {
		this.transport = Objects.requireNonNull(transport, "transport");
		this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
		this.documents = new DocumentsNamespace(transport, jsonMapper);
		this.search = new SearchNamespace(transport, jsonMapper);
	}

	/** 教学默认：内存 Transport + Jackson。 */
	public static ToyClient inMemory() {
		return new ToyClient(new InMemoryTransport(), new JacksonJsonMapper());
	}

	public DocumentsNamespace documents() {
		return documents;
	}

	public SearchNamespace search() {
		return search;
	}

	/** 便捷方法：等价于 {@code search().search(...)}。 */
	public SearchResponse search(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> fn) {
		return search.search(fn);
	}

	public Transport transport() {
		return transport;
	}

	public JsonMapper jsonMapper() {
		return jsonMapper;
	}

	/** 同一套 Transport/Mapper，暴露异步外观。 */
	public ToyAsyncClient async() {
		return new ToyAsyncClient(transport, jsonMapper);
	}

	@Override
	public void close() {
		transport.close();
	}

}

package com.lind.architecture.api.documents;

import com.lind.architecture.json.JsonWritable;
import com.lind.architecture.util.ApiTypeHelper;
import com.lind.architecture.util.ObjectBuilder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public final class IndexRequest implements JsonWritable {

	private final String index;

	private final String id;

	private final Map<String, Object> document;

	private IndexRequest(String index, String id, Map<String, Object> document) {
		this.index = index;
		this.id = id;
		this.document = Map.copyOf(document);
	}

	public String index() {
		return index;
	}

	public String id() {
		return id;
	}

	public Map<String, Object> document() {
		return document;
	}

	public static IndexRequest of(Function<Builder, ObjectBuilder<IndexRequest>> fn) {
		return ApiTypeHelper.resolve(fn, new Builder());
	}

	@Override
	public Object toJsonTree() {
		return new LinkedHashMap<>(document);
	}

	public static final class Builder implements ObjectBuilder<IndexRequest> {

		private String index;

		private String id;

		private Map<String, Object> document = Map.of();

		public Builder index(String index) {
			this.index = index;
			return this;
		}

		public Builder id(String id) {
			this.id = id;
			return this;
		}

		public Builder document(Map<String, Object> document) {
			this.document = document;
			return this;
		}

		@Override
		public IndexRequest build() {
			return new IndexRequest(ApiTypeHelper.requireNonNull(index, "index"),
					ApiTypeHelper.requireNonNull(id, "id"), ApiTypeHelper.requireNonNull(document, "document"));
		}

	}

}

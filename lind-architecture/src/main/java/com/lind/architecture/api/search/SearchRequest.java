package com.lind.architecture.api.search;

import com.lind.architecture.api.search.query.Query;
import com.lind.architecture.json.JsonWritable;
import com.lind.architecture.util.ApiTypeHelper;
import com.lind.architecture.util.ObjectBuilder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/** 不可变请求对象 + Fluent Builder（知识点 1）。 */
public final class SearchRequest implements JsonWritable {

	private final String index;

	private final Query query;

	private final Integer size;

	private SearchRequest(String index, Query query, Integer size) {
		this.index = index;
		this.query = query;
		this.size = size;
	}

	public String index() {
		return index;
	}

	public Query query() {
		return query;
	}

	public Integer size() {
		return size;
	}

	public static SearchRequest of(Function<Builder, ObjectBuilder<SearchRequest>> fn) {
		return ApiTypeHelper.resolve(fn, new Builder());
	}

	@Override
	public Object toJsonTree() {
		Map<String, Object> m = new LinkedHashMap<>();
		if (query != null) {
			m.put("query", query.toJsonTree());
		}
		if (size != null) {
			m.put("size", size);
		}
		return m;
	}

	public static final class Builder implements ObjectBuilder<SearchRequest> {

		private String index;

		private Query query;

		private Integer size;

		public Builder index(String index) {
			this.index = index;
			return this;
		}

		public Builder query(Function<Query.Builder, ObjectBuilder<Query>> fn) {
			this.query = ApiTypeHelper.resolve(fn, new Query.Builder());
			return this;
		}

		public Builder query(Query query) {
			this.query = query;
			return this;
		}

		public Builder size(int size) {
			this.size = size;
			return this;
		}

		@Override
		public SearchRequest build() {
			return new SearchRequest(ApiTypeHelper.requireNonNull(index, "index"), query, size);
		}

	}

}

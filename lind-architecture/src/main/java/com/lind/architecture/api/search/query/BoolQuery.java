package com.lind.architecture.api.search.query;

import com.lind.architecture.json.JsonWritable;
import com.lind.architecture.util.ApiTypeHelper;
import com.lind.architecture.util.ObjectBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class BoolQuery implements JsonWritable {

	private final List<Query> must;

	private BoolQuery(List<Query> must) {
		this.must = List.copyOf(must);
	}

	public List<Query> must() {
		return must;
	}

	@Override
	public Object toJsonTree() {
		Map<String, Object> m = new LinkedHashMap<>();
		List<Object> mustTrees = new ArrayList<>();
		for (Query q : must) {
			mustTrees.add(q.toJsonTree());
		}
		m.put("must", mustTrees);
		return m;
	}

	public static final class Builder implements ObjectBuilder<BoolQuery> {

		private final List<Query> must = new ArrayList<>();

		public Builder must(Function<Query.Builder, ObjectBuilder<Query>> fn) {
			must.add(ApiTypeHelper.resolve(fn, new Query.Builder()));
			return this;
		}

		public Builder must(Query query) {
			must.add(ApiTypeHelper.requireNonNull(query, "query"));
			return this;
		}

		@Override
		public BoolQuery build() {
			return new BoolQuery(must);
		}

	}

}

package com.lind.architecture.api.search.query;

import com.lind.architecture.json.JsonWritable;
import com.lind.architecture.util.ApiTypeHelper;
import com.lind.architecture.util.ObjectBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MatchQuery implements JsonWritable {

	private final String field;

	private final String query;

	private MatchQuery(String field, String query) {
		this.field = field;
		this.query = query;
	}

	public String field() {
		return field;
	}

	public String query() {
		return query;
	}

	@Override
	public Object toJsonTree() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("field", field);
		m.put("query", query);
		return m;
	}

	public static final class Builder implements ObjectBuilder<MatchQuery> {

		private String field;

		private String query;

		public Builder field(String field) {
			this.field = field;
			return this;
		}

		public Builder query(String query) {
			this.query = query;
			return this;
		}

		@Override
		public MatchQuery build() {
			return new MatchQuery(ApiTypeHelper.requireNonNull(field, "field"),
					ApiTypeHelper.requireNonNull(query, "query"));
		}

	}

}

package com.lind.architecture.api.search.query;

import com.lind.architecture.json.JsonWritable;
import com.lind.architecture.util.ApiTypeHelper;
import com.lind.architecture.util.ObjectBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TermQuery implements JsonWritable {

	private final String field;

	private final String value;

	private TermQuery(String field, String value) {
		this.field = field;
		this.value = value;
	}

	public String field() {
		return field;
	}

	public String value() {
		return value;
	}

	@Override
	public Object toJsonTree() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("field", field);
		m.put("value", value);
		return m;
	}

	public static final class Builder implements ObjectBuilder<TermQuery> {

		private String field;

		private String value;

		public Builder field(String field) {
			this.field = field;
			return this;
		}

		public Builder value(String value) {
			this.value = value;
			return this;
		}

		@Override
		public TermQuery build() {
			return new TermQuery(ApiTypeHelper.requireNonNull(field, "field"),
					ApiTypeHelper.requireNonNull(value, "value"));
		}

	}

}

package com.lind.architecture.api.search.query;

import com.lind.architecture.json.JsonWritable;
import com.lind.architecture.util.ApiTypeHelper;
import com.lind.architecture.util.ObjectBuilder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * 知识点 (4)：Tagged Union / Variant——用 kind + 访问器表达互斥 JSON 形态（对照 ES {@code Query}）。
 */
public final class Query implements JsonWritable {

	public enum Kind {

		TERM, MATCH, BOOL

	}

	private final Kind kind;

	private final TermQuery term;

	private final MatchQuery match;

	private final BoolQuery bool;

	private Query(Kind kind, TermQuery term, MatchQuery match, BoolQuery bool) {
		this.kind = kind;
		this.term = term;
		this.match = match;
		this.bool = bool;
	}

	public Kind kind() {
		return kind;
	}

	public TermQuery term() {
		return term;
	}

	public MatchQuery match() {
		return match;
	}

	public BoolQuery bool() {
		return bool;
	}

	public static Query of(Function<Builder, ObjectBuilder<Query>> fn) {
		return ApiTypeHelper.resolve(fn, new Builder());
	}

	@Override
	public Object toJsonTree() {
		Map<String, Object> root = new LinkedHashMap<>();
		switch (kind) {
			case TERM -> root.put("term", term.toJsonTree());
			case MATCH -> root.put("match", match.toJsonTree());
			case BOOL -> root.put("bool", bool.toJsonTree());
		}
		return root;
	}

	public static final class Builder implements ObjectBuilder<Query> {

		private Query value;

		public Builder term(Function<TermQuery.Builder, ObjectBuilder<TermQuery>> fn) {
			this.value = new Query(Kind.TERM, ApiTypeHelper.resolve(fn, new TermQuery.Builder()), null, null);
			return this;
		}

		public Builder match(Function<MatchQuery.Builder, ObjectBuilder<MatchQuery>> fn) {
			this.value = new Query(Kind.MATCH, null, ApiTypeHelper.resolve(fn, new MatchQuery.Builder()), null);
			return this;
		}

		public Builder bool(Function<BoolQuery.Builder, ObjectBuilder<BoolQuery>> fn) {
			this.value = new Query(Kind.BOOL, null, null, ApiTypeHelper.resolve(fn, new BoolQuery.Builder()));
			return this;
		}

		@Override
		public Query build() {
			return Objects.requireNonNull(value, "query variant is required");
		}

	}

}

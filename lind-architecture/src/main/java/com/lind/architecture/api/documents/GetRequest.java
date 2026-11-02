package com.lind.architecture.api.documents;

import com.lind.architecture.util.ApiTypeHelper;
import com.lind.architecture.util.ObjectBuilder;

import java.util.function.Function;

public final class GetRequest {

	private final String index;

	private final String id;

	private GetRequest(String index, String id) {
		this.index = index;
		this.id = id;
	}

	public String index() {
		return index;
	}

	public String id() {
		return id;
	}

	public static GetRequest of(Function<Builder, ObjectBuilder<GetRequest>> fn) {
		return ApiTypeHelper.resolve(fn, new Builder());
	}

	public static final class Builder implements ObjectBuilder<GetRequest> {

		private String index;

		private String id;

		public Builder index(String index) {
			this.index = index;
			return this;
		}

		public Builder id(String id) {
			this.id = id;
			return this;
		}

		@Override
		public GetRequest build() {
			return new GetRequest(ApiTypeHelper.requireNonNull(index, "index"), ApiTypeHelper.requireNonNull(id, "id"));
		}

	}

}

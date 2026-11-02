package com.lind.architecture.api;

import com.lind.architecture.api.documents.GetRequest;
import com.lind.architecture.api.documents.GetResponse;
import com.lind.architecture.api.documents.IndexRequest;
import com.lind.architecture.api.documents.IndexResponse;
import com.lind.architecture.api.search.SearchRequest;
import com.lind.architecture.api.search.SearchResponse;
import com.lind.architecture.transport.Endpoint;
import com.lind.architecture.transport.HttpMethod;

/**
 * 手写 Endpoint 注册表——对照「稳定内核」：API 面增长 = 加 Endpoint，不改 Transport。
 */
public final class Endpoints {

	private Endpoints() {
	}

	public static final Endpoint<IndexRequest, IndexResponse> DOCUMENTS_INDEX = new Endpoint<>() {
		@Override
		public String id() {
			return "documents.index";
		}

		@Override
		public HttpMethod method() {
			return HttpMethod.PUT;
		}

		@Override
		public String pathTemplate() {
			return "/{index}/_doc/{id}";
		}

		@Override
		public String path(IndexRequest request) {
			return "/" + request.index() + "/_doc/" + request.id();
		}

		@Override
		public boolean hasRequestBody() {
			return true;
		}

		@Override
		public Class<IndexResponse> responseType() {
			return IndexResponse.class;
		}
	};

	public static final Endpoint<GetRequest, GetResponse> DOCUMENTS_GET = new Endpoint<>() {
		@Override
		public String id() {
			return "documents.get";
		}

		@Override
		public HttpMethod method() {
			return HttpMethod.GET;
		}

		@Override
		public String pathTemplate() {
			return "/{index}/_doc/{id}";
		}

		@Override
		public String path(GetRequest request) {
			return "/" + request.index() + "/_doc/" + request.id();
		}

		@Override
		public boolean hasRequestBody() {
			return false;
		}

		@Override
		public Class<GetResponse> responseType() {
			return GetResponse.class;
		}
	};

	public static final Endpoint<SearchRequest, SearchResponse> SEARCH = new Endpoint<>() {
		@Override
		public String id() {
			return "search";
		}

		@Override
		public HttpMethod method() {
			return HttpMethod.POST;
		}

		@Override
		public String pathTemplate() {
			return "/{index}/_search";
		}

		@Override
		public String path(SearchRequest request) {
			return "/" + request.index() + "/_search";
		}

		@Override
		public boolean hasRequestBody() {
			return true;
		}

		@Override
		public Class<SearchResponse> responseType() {
			return SearchResponse.class;
		}
	};

}

package com.lind.architecture.api.documents;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class IndexResponse {

	private String index;

	private String id;

	private String result;

	public String index() {
		return index;
	}

	public void setIndex(String index) {
		this.index = index;
	}

	public String id() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String result() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

}

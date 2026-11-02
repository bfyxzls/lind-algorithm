package com.lind.architecture.api.documents;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class GetResponse {

	private String index;

	private String id;

	private boolean found;

	@JsonProperty("source")
	private Map<String, Object> source;

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

	public boolean found() {
		return found;
	}

	public void setFound(boolean found) {
		this.found = found;
	}

	public Map<String, Object> source() {
		return source;
	}

	public void setSource(Map<String, Object> source) {
		this.source = source;
	}

}

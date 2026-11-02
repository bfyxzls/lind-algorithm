package com.lind.architecture.api.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class SearchResponse {

	private int took;

	private HitsWrapper hits;

	public int took() {
		return took;
	}

	public void setTook(int took) {
		this.took = took;
	}

	public HitsWrapper hits() {
		return hits;
	}

	public void setHits(HitsWrapper hits) {
		this.hits = hits;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class HitsWrapper {

		private int total;

		private List<Hit> hits = List.of();

		public int total() {
			return total;
		}

		public void setTotal(int total) {
			this.total = total;
		}

		public List<Hit> hits() {
			return hits;
		}

		public void setHits(List<Hit> hits) {
			this.hits = hits == null ? List.of() : hits;
		}

	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class Hit {

		private String id;

		private double score;

		@JsonProperty("source")
		private Map<String, Object> source;

		public String id() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public double score() {
			return score;
		}

		public void setScore(double score) {
			this.score = score;
		}

		public Map<String, Object> source() {
			return source;
		}

		public void setSource(Map<String, Object> source) {
			this.source = source;
		}

	}

}

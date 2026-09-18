package com.lind.data.elasticsearch;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 搜索结果。
 */
public final class EsSearchResult {

	private final long total;

	private final List<Map<String, Object>> hits;

	public EsSearchResult(long total, List<Map<String, Object>> hits) {
		this.total = total;
		this.hits = List.copyOf(hits == null ? List.of() : hits);
	}

	public long total() {
		return total;
	}

	public List<Map<String, Object>> hits() {
		return hits;
	}

	public static EsSearchResult empty() {
		return new EsSearchResult(0, Collections.emptyList());
	}

	@Override
	public String toString() {
		return "EsSearchResult{total=" + total + ", hits=" + hits.size() + "}";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof EsSearchResult that)) {
			return false;
		}
		return total == that.total && Objects.equals(hits, that.hits);
	}

	@Override
	public int hashCode() {
		return Objects.hash(total, hits);
	}

}

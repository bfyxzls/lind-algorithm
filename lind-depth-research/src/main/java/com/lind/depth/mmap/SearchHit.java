package com.lind.depth.mmap;

/**
 * 一条检索命中：向量下标 + 距离（越小越近）。
 */
public record SearchHit(int id, float distance) implements Comparable<SearchHit> {

	@Override
	public int compareTo(SearchHit o) {
		return Float.compare(this.distance, o.distance);
	}

}

package com.lind.delay.task;

import java.util.List;

/**
 * 简单分页结果。
 */
public class PageResult<T> {

	private final List<T> items;

	private final int page;

	private final int size;

	private final long total;

	public PageResult(List<T> items, int page, int size, long total) {
		this.items = items;
		this.page = page;
		this.size = size;
		this.total = total;
	}

	public List<T> getItems() {
		return items;
	}

	public int getPage() {
		return page;
	}

	public int getSize() {
		return size;
	}

	public long getTotal() {
		return total;
	}

	public int getTotalPages() {
		if (size <= 0) {
			return 0;
		}
		return (int) ((total + size - 1) / size);
	}

}

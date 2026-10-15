package com.lind.qps.web.catalog;

import java.time.Instant;

/**
 * 测试用例文件夹。
 */
public final class TestFolder {

	private final String id;

	private String name;

	private String parentId;

	private Instant createdAt;

	private Instant updatedAt;

	public TestFolder(String id, String name, String parentId) {
		this.id = id;
		this.name = name;
		this.parentId = parentId;
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	public TestFolder(String id, String name, String parentId, Instant createdAt, Instant updatedAt) {
		this.id = id;
		this.name = name;
		this.parentId = parentId;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public String id() {
		return id;
	}

	public String name() {
		return name;
	}

	public void name(String name) {
		this.name = name;
	}

	public String parentId() {
		return parentId;
	}

	public void parentId(String parentId) {
		this.parentId = parentId;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public Instant updatedAt() {
		return updatedAt;
	}

	public void touch() {
		this.updatedAt = Instant.now();
	}

}

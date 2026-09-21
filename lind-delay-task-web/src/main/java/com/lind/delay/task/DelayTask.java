package com.lind.delay.task;

/**
 * 延时任务持久化记录。
 * <p>
 * 真实执行时间：{@code executeAt = createdAt + delayMs}（创建时写入）； 重启灌轮用绝对 {@code executeAt}， 剩余
 * delay = {@code max(0, executeAt - now)}，绝不能再用「现在 + delayMs」。
 */
public class DelayTask {

	private String id;

	private String bizType;

	private String bizId;

	private String payload;

	/** 创建时约定的延时毫秒（相对 createdAt） */
	private long delayMs;

	/** 计划执行绝对时间（epoch ms）= createdAt + delayMs（首次）；重试时会更新 */
	private long executeAt;

	private DelayTaskStatus status;

	private int version;

	private long createdAt;

	private long updatedAt;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getBizType() {
		return bizType;
	}

	public void setBizType(String bizType) {
		this.bizType = bizType;
	}

	public String getBizId() {
		return bizId;
	}

	public void setBizId(String bizId) {
		this.bizId = bizId;
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

	public long getDelayMs() {
		return delayMs;
	}

	public void setDelayMs(long delayMs) {
		this.delayMs = delayMs;
	}

	public long getExecuteAt() {
		return executeAt;
	}

	public void setExecuteAt(long executeAt) {
		this.executeAt = executeAt;
	}

	public DelayTaskStatus getStatus() {
		return status;
	}

	public void setStatus(DelayTaskStatus status) {
		this.status = status;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public long getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(long createdAt) {
		this.createdAt = createdAt;
	}

	public long getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(long updatedAt) {
		this.updatedAt = updatedAt;
	}

	/**
	 * 按「创建时间 + 延时」得到真实执行点（首次调度语义）。
	 */
	public long resolveExecuteAtFromCreate() {
		return createdAt + delayMs;
	}

}

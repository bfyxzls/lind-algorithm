package com.lind.qps.web.job;

/**
 * Web 表单绑定：发起一次压测。
 */
public class LoadForm {

	private String url = "http://127.0.0.1:8080/ping";

	private String method = "GET";

	/** 每行一个 Header：Name:Value 或 Name=Value */
	private String headersText = "";

	/** 每行一个 Query：key=value */
	private String queryParamsText = "";

	/** 每行一个 Form：key=value */
	private String formParamsText = "";

	private String body = "";

	private int concurrency = 10;

	private long durationSeconds = 10;

	private long timeoutMs = 5000;

	private LoadMode mode = LoadMode.FIXED;

	private int startConcurrency = 10;

	private int step = 10;

	private int maxConcurrency = 200;

	private double minSuccessRatePercent = 99.0;

	private long maxAllowedLatencyMillis = 3000;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getHeadersText() {
		return headersText;
	}

	public void setHeadersText(String headersText) {
		this.headersText = headersText;
	}

	public String getQueryParamsText() {
		return queryParamsText;
	}

	public void setQueryParamsText(String queryParamsText) {
		this.queryParamsText = queryParamsText;
	}

	public String getFormParamsText() {
		return formParamsText;
	}

	public void setFormParamsText(String formParamsText) {
		this.formParamsText = formParamsText;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public int getConcurrency() {
		return concurrency;
	}

	public void setConcurrency(int concurrency) {
		this.concurrency = concurrency;
	}

	public long getDurationSeconds() {
		return durationSeconds;
	}

	public void setDurationSeconds(long durationSeconds) {
		this.durationSeconds = durationSeconds;
	}

	public long getTimeoutMs() {
		return timeoutMs;
	}

	public void setTimeoutMs(long timeoutMs) {
		this.timeoutMs = timeoutMs;
	}

	public LoadMode getMode() {
		return mode;
	}

	public void setMode(LoadMode mode) {
		this.mode = mode;
	}

	public int getStartConcurrency() {
		return startConcurrency;
	}

	public void setStartConcurrency(int startConcurrency) {
		this.startConcurrency = startConcurrency;
	}

	public int getStep() {
		return step;
	}

	public void setStep(int step) {
		this.step = step;
	}

	public int getMaxConcurrency() {
		return maxConcurrency;
	}

	public void setMaxConcurrency(int maxConcurrency) {
		this.maxConcurrency = maxConcurrency;
	}

	public double getMinSuccessRatePercent() {
		return minSuccessRatePercent;
	}

	public void setMinSuccessRatePercent(double minSuccessRatePercent) {
		this.minSuccessRatePercent = minSuccessRatePercent;
	}

	public long getMaxAllowedLatencyMillis() {
		return maxAllowedLatencyMillis;
	}

	public void setMaxAllowedLatencyMillis(long maxAllowedLatencyMillis) {
		this.maxAllowedLatencyMillis = maxAllowedLatencyMillis;
	}

}

package com.lind.qps.web.catalog;

import com.lind.qps.web.job.LoadForm;
import com.lind.qps.web.job.LoadMode;
import java.time.Instant;

/**
 * 可复用压测用例。
 */
public final class TestCase {

	private final String id;

	private String folderId;

	private String name;

	private String description;

	private String url;

	private String method;

	private LoadMode mode;

	private int concurrency;

	private long durationSeconds;

	private long timeoutMs;

	private String headersText;

	private String queryParamsText;

	private String formParamsText;

	private String body;

	private int startConcurrency;

	private int step;

	private int maxConcurrency;

	private double minSuccessRatePercent;

	private long maxAllowedLatencyMillis;

	private Instant createdAt;

	private Instant updatedAt;

	public TestCase(String id) {
		this.id = id;
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
		this.method = "GET";
		this.mode = LoadMode.FIXED;
		this.concurrency = 10;
		this.durationSeconds = 10;
		this.timeoutMs = 5000;
		this.headersText = "";
		this.queryParamsText = "";
		this.formParamsText = "";
		this.body = "";
		this.description = "";
		this.startConcurrency = 10;
		this.step = 10;
		this.maxConcurrency = 200;
		this.minSuccessRatePercent = 99.0;
		this.maxAllowedLatencyMillis = 3000;
	}

	public String id() {
		return id;
	}

	public String folderId() {
		return folderId;
	}

	public void folderId(String folderId) {
		this.folderId = folderId;
	}

	public String name() {
		return name;
	}

	public void name(String name) {
		this.name = name;
	}

	public String description() {
		return description;
	}

	public void description(String description) {
		this.description = description == null ? "" : description;
	}

	public String url() {
		return url;
	}

	public void url(String url) {
		this.url = url;
	}

	public String method() {
		return method;
	}

	public void method(String method) {
		this.method = method;
	}

	public LoadMode mode() {
		return mode;
	}

	public void mode(LoadMode mode) {
		this.mode = mode;
	}

	public int concurrency() {
		return concurrency;
	}

	public void concurrency(int concurrency) {
		this.concurrency = concurrency;
	}

	public long durationSeconds() {
		return durationSeconds;
	}

	public void durationSeconds(long durationSeconds) {
		this.durationSeconds = durationSeconds;
	}

	public long timeoutMs() {
		return timeoutMs;
	}

	public void timeoutMs(long timeoutMs) {
		this.timeoutMs = timeoutMs;
	}

	public String headersText() {
		return headersText;
	}

	public void headersText(String headersText) {
		this.headersText = headersText == null ? "" : headersText;
	}

	public String queryParamsText() {
		return queryParamsText;
	}

	public void queryParamsText(String queryParamsText) {
		this.queryParamsText = queryParamsText == null ? "" : queryParamsText;
	}

	public String formParamsText() {
		return formParamsText;
	}

	public void formParamsText(String formParamsText) {
		this.formParamsText = formParamsText == null ? "" : formParamsText;
	}

	public String body() {
		return body;
	}

	public void body(String body) {
		this.body = body == null ? "" : body;
	}

	public int startConcurrency() {
		return startConcurrency;
	}

	public void startConcurrency(int startConcurrency) {
		this.startConcurrency = startConcurrency;
	}

	public int step() {
		return step;
	}

	public void step(int step) {
		this.step = step;
	}

	public int maxConcurrency() {
		return maxConcurrency;
	}

	public void maxConcurrency(int maxConcurrency) {
		this.maxConcurrency = maxConcurrency;
	}

	public double minSuccessRatePercent() {
		return minSuccessRatePercent;
	}

	public void minSuccessRatePercent(double minSuccessRatePercent) {
		this.minSuccessRatePercent = minSuccessRatePercent;
	}

	public long maxAllowedLatencyMillis() {
		return maxAllowedLatencyMillis;
	}

	public void maxAllowedLatencyMillis(long maxAllowedLatencyMillis) {
		this.maxAllowedLatencyMillis = maxAllowedLatencyMillis;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public void createdAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant updatedAt() {
		return updatedAt;
	}

	public void updatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	public void touch() {
		this.updatedAt = Instant.now();
	}

	public LoadForm toLoadForm() {
		LoadForm form = new LoadForm();
		form.setUrl(url);
		form.setMethod(method);
		form.setHeadersText(headersText);
		form.setQueryParamsText(queryParamsText);
		form.setFormParamsText(formParamsText);
		form.setBody(body);
		form.setConcurrency(concurrency);
		form.setDurationSeconds(durationSeconds);
		form.setTimeoutMs(timeoutMs);
		form.setMode(mode);
		form.setStartConcurrency(startConcurrency);
		form.setStep(step);
		form.setMaxConcurrency(maxConcurrency);
		form.setMinSuccessRatePercent(minSuccessRatePercent);
		form.setMaxAllowedLatencyMillis(maxAllowedLatencyMillis);
		return form;
	}

	public static TestCase fromLoadForm(String id, String folderId, String name, String description, LoadForm form) {
		TestCase testCase = new TestCase(id);
		testCase.folderId(folderId);
		testCase.name(name);
		testCase.description(description);
		testCase.applyForm(form);
		return testCase;
	}

	public void applyForm(LoadForm form) {
		this.url = form.getUrl();
		this.method = form.getMethod();
		this.headersText = form.getHeadersText();
		this.queryParamsText = form.getQueryParamsText();
		this.formParamsText = form.getFormParamsText();
		this.body = form.getBody();
		this.concurrency = form.getConcurrency();
		this.durationSeconds = form.getDurationSeconds();
		this.timeoutMs = form.getTimeoutMs();
		this.mode = form.getMode() == null ? LoadMode.FIXED : form.getMode();
		this.startConcurrency = form.getStartConcurrency();
		this.step = form.getStep();
		this.maxConcurrency = form.getMaxConcurrency();
		this.minSuccessRatePercent = form.getMinSuccessRatePercent();
		this.maxAllowedLatencyMillis = form.getMaxAllowedLatencyMillis();
		touch();
	}

}

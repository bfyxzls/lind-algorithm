package com.lind.qps.web.catalog;

import com.lind.qps.web.job.LoadForm;

/**
 * 用例编辑表单（含元数据 + 压测参数）。
 */
public class CaseForm extends LoadForm {

	private String id;

	private String folderId;

	private String name = "";

	private String description = "";

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFolderId() {
		return folderId;
	}

	public void setFolderId(String folderId) {
		this.folderId = folderId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public static CaseForm from(TestCase testCase) {
		CaseForm form = new CaseForm();
		form.setId(testCase.id());
		form.setFolderId(testCase.folderId());
		form.setName(testCase.name());
		form.setDescription(testCase.description());
		LoadForm base = testCase.toLoadForm();
		form.setUrl(base.getUrl());
		form.setMethod(base.getMethod());
		form.setHeadersText(base.getHeadersText());
		form.setQueryParamsText(base.getQueryParamsText());
		form.setFormParamsText(base.getFormParamsText());
		form.setBody(base.getBody());
		form.setConcurrency(base.getConcurrency());
		form.setDurationSeconds(base.getDurationSeconds());
		form.setTimeoutMs(base.getTimeoutMs());
		form.setMode(base.getMode());
		form.setStartConcurrency(base.getStartConcurrency());
		form.setStep(base.getStep());
		form.setMaxConcurrency(base.getMaxConcurrency());
		form.setMinSuccessRatePercent(base.getMinSuccessRatePercent());
		form.setMaxAllowedLatencyMillis(base.getMaxAllowedLatencyMillis());
		return form;
	}

}

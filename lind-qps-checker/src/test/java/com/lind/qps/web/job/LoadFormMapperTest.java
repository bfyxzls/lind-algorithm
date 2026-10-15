package com.lind.qps.web.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lind.qps.engine.LoadConfig;
import org.junit.jupiter.api.Test;

class LoadFormMapperTest {

	@Test
	void mapsHeadersQueryFormAndBody() {
		LoadForm form = new LoadForm();
		form.setUrl("http://localhost/login");
		form.setMethod("POST");
		form.setHeadersText("Authorization:Bearer t\nX-Trace=1");
		form.setQueryParamsText("from=app\nlang=zh");
		form.setFormParamsText("username=admin\npassword=123");
		form.setConcurrency(5);
		form.setDurationSeconds(3);
		form.setTimeoutMs(2000);

		LoadConfig config = LoadFormMapper.toLoadConfig(form);
		assertThat(config.method()).isEqualTo("POST");
		assertThat(config.headers()).containsEntry("Authorization", "Bearer t").containsEntry("X-Trace", "1");
		assertThat(config.queryParams()).containsEntry("from", "app").containsEntry("lang", "zh");
		assertThat(config.formParams()).containsEntry("username", "admin").containsEntry("password", "123");
		assertThat(config.requestBody()).isEqualTo("username=admin&password=123");
	}

	@Test
	void rejectsBlankUrl() {
		LoadForm form = new LoadForm();
		form.setUrl(" ");
		assertThatThrownBy(() -> LoadFormMapper.toLoadConfig(form)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void ignoresCommentAndBlankLines() {
		assertThat(LoadFormMapper.splitLines("# comment\n\na=1\n  \nb=2")).containsExactly("a=1", "b=2");
	}

}

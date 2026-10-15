package com.lind.qps.web.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lind.qps.web.job.LoadForm;
import org.junit.jupiter.api.Test;

class CurlImporterTest {

	@Test
	void parsesChromeDevtoolsStylePostJson() {
		String curl = """
				curl 'https://api.example.com/v1/orders?lang=zh' \\
				  -H 'Authorization: Bearer tok' \\
				  -H 'Content-Type: application/json' \\
				  -H 'Accept-Encoding: gzip' \\
				  --data-raw '{"sku":"A1"}'
				""";
		CurlImporter.Result result = CurlImporter.parse(curl);
		LoadForm form = result.form();
		assertThat(form.getMethod()).isEqualTo("POST");
		assertThat(form.getUrl()).isEqualTo("https://api.example.com/v1/orders");
		assertThat(form.getQueryParamsText()).isEqualTo("lang=zh");
		assertThat(form.getHeadersText()).contains("Authorization: Bearer tok")
				.contains("Content-Type: application/json").doesNotContain("Accept-Encoding");
		assertThat(form.getBody()).isEqualTo("{\"sku\":\"A1\"}");
		assertThat(result.suggestedName()).isEqualTo("POST orders");
	}

	@Test
	void parsesFormUrlEncodedAsFormParams() {
		String curl = "curl -X POST http://localhost/login -d 'username=admin&password=123'";
		LoadForm form = CurlImporter.parse(curl).form();
		assertThat(form.getMethod()).isEqualTo("POST");
		assertThat(form.getFormParamsText()).contains("username=admin").contains("password=123");
		assertThat(form.getBody()).isBlank();
	}

	@Test
	void parsesGetWithQuery() {
		String curl = "curl -G 'http://127.0.0.1/search' -d 'q=java' -d 'page=1'";
		LoadForm form = CurlImporter.parse(curl).form();
		assertThat(form.getMethod()).isEqualTo("GET");
		assertThat(form.getQueryParamsText()).contains("q=java").contains("page=1");
	}

	@Test
	void rejectsBlank() {
		assertThatThrownBy(() -> CurlImporter.parse("  ")).isInstanceOf(IllegalArgumentException.class);
	}

}

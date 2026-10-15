package com.lind.qps.web.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lind.qps.web.job.LoadForm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CaseCatalogServiceTest {

	@Autowired
	private CaseCatalogService catalogService;

	@Test
	void createFolderAndSaveCase() {
		TestFolder folder = catalogService.createFolder("订单接口-" + System.nanoTime(), null);
		assertThat(catalogService.listFolders()).extracting(TestFolder::id).contains(folder.id());

		LoadForm form = new LoadForm();
		form.setUrl("http://127.0.0.1/api/order");
		form.setMethod("POST");
		form.setBody("{\"id\":1}");
		form.setConcurrency(5);
		form.setHeadersText("X-Token:abc");

		TestCase saved = catalogService.saveCase(null, folder.id(), "创建订单", "smoke", form);
		assertThat(saved.id()).isNotBlank();
		assertThat(catalogService.listCases(folder.id())).extracting(TestCase::name).contains("创建订单");

		TestCase loaded = catalogService.findCase(saved.id()).orElseThrow();
		assertThat(loaded.url()).isEqualTo("http://127.0.0.1/api/order");
		assertThat(loaded.headersText()).contains("X-Token:abc");
		assertThat(loaded.toLoadForm().getBody()).isEqualTo("{\"id\":1}");

		catalogService.deleteCase(saved.id());
		assertThat(catalogService.findCase(saved.id())).isEmpty();
		catalogService.deleteFolder(folder.id());
	}

	@Test
	void refuseDeleteFolderWithCases() {
		TestFolder folder = catalogService.createFolder("不可删-" + System.nanoTime(), null);
		LoadForm form = new LoadForm();
		form.setUrl("http://127.0.0.1/x");
		catalogService.saveCase(null, folder.id(), "case-a", "", form);
		assertThatThrownBy(() -> catalogService.deleteFolder(folder.id())).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("测试用例");
	}

}

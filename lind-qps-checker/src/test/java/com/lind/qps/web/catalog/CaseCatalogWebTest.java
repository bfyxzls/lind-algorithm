package com.lind.qps.web.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CaseCatalogWebTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CaseCatalogService catalogService;

	@Test
	void casesPageAndCreateFolder() throws Exception {
		mockMvc.perform(get("/cases")).andExpect(status().isOk()).andExpect(view().name("case-list"));
		mockMvc.perform(post("/folders").param("name", "web-folder-" + System.nanoTime()))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("/cases?folderId=*"));
	}

	@Test
	void saveCaseFromForm() throws Exception {
		String folderId = catalogService.defaultFolderId();
		mockMvc.perform(post("/cases").param("folderId", folderId).param("name", "web-case-" + System.nanoTime())
				.param("url", "http://127.0.0.1/ping").param("method", "GET").param("mode", "FIXED")
				.param("concurrency", "2").param("durationSeconds", "1").param("timeoutMs", "1000"))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("/cases?folderId=*"));
	}

	@Test
	void importCurlIntoFolder() throws Exception {
		String folderId = catalogService.defaultFolderId();
		String curl = "curl -X POST 'http://127.0.0.1/api/ping' -H 'X-Test:1' --data-raw '{\"ok\":true}'";
		mockMvc.perform(post("/cases/import-curl").param("folderId", folderId).param("curl", curl).param("name",
				"curl-import-" + System.nanoTime())).andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern("/cases?folderId=*"));
	}

}

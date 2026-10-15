package com.lind.qps.web.job;

import static org.assertj.core.api.Assertions.assertThat;

import com.lind.qps.engine.FailureSample;
import com.lind.qps.engine.LoadConfig;
import com.lind.qps.engine.LoadReport;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class JdbcLoadJobStoreTest {

	@Autowired
	private LoadJobStore store;

	@Test
	void insertAndUpdateFinishedPersistsReport() {
		LoadConfig config = LoadConfig.builder().url("http://127.0.0.1/ping").method("GET").concurrency(3)
				.duration(Duration.ofSeconds(2)).requestTimeout(Duration.ofMillis(1500)).header("X-Test", "1")
				.queryParam("q", "v").build();
		LoadJob job = new LoadJob("persist01", LoadMode.FIXED, config);
		job.status(LoadJobStatus.RUNNING);
		store.insert(job);

		LoadReport report = new LoadReport("http://127.0.0.1/ping", 3, 2000, 10, 8, 2, 1, 5, 20, 15, 18, 5.0,
				List.of(new FailureSample(500, "{\"e\":1}", "unexpected status 500", 12)));
		job.report(report);
		job.status(LoadJobStatus.SUCCESS);
		job.message("done");
		job.finishedAt(Instant.now());
		store.updateFinished(job);

		LoadJob loaded = store.findById("persist01").orElseThrow();
		assertThat(loaded.status()).isEqualTo(LoadJobStatus.SUCCESS);
		assertThat(loaded.config().headers()).containsEntry("X-Test", "1");
		assertThat(loaded.config().queryParams()).containsEntry("q", "v");
		assertThat(loaded.report()).isNotNull();
		assertThat(loaded.report().failureCount()).isEqualTo(2);
		assertThat(loaded.report().failureSamples()).hasSize(1);
		assertThat(loaded.report().failureSamples().get(0).statusCode()).isEqualTo(500);
		assertThat(loaded.report().failureSamples().get(0).responseBody()).contains("{\"e\":1}");
		assertThat(store.findRecent(10).stream().anyMatch(j -> j.id().equals("persist01"))).isTrue();
	}

}

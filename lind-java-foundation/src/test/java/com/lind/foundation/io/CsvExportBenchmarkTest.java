package com.lind.foundation.io;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CsvExportBenchmarkTest {

	@TempDir
	Path dir;

	@Test
	void benchmark_report_buffered_beats_flushPerLine_and_fewer_writes() throws Exception {
		var report = CsvExportBenchmark.run(dir, 8_000);

		assertThat(report.rows()).isEqualTo(8_000);
		assertThat(report.bufferedBytes()).isEqualTo(report.unbufferedBytes());
		assertThat(report.flushPerLineBytes()).isEqualTo(report.bufferedBytes());

		assertThat(report.bufferedDownstreamWrites()).isLessThan(report.unbufferedDownstreamWrites());
		assertThat(report.bufferedDownstreamWrites()).isLessThan(report.flushPerLineDownstreamWrites());

		// 每行 flush 通常明显慢于 BufferedWriter（允许机器抖动，只要求不极端反常）
		assertThat(report.flushPerLineMillis()).isGreaterThanOrEqualTo(report.bufferedMillis());

		assertThat(Files.exists(report.reportFile())).isTrue();
		String md = Files.readString(report.reportFile());
		assertThat(md).contains("CSV 导出性能对比报告").contains("BufferedWriter");
	}

}

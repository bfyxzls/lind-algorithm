package com.lind.foundation.io;

import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 关于缓冲区与流的测试
 */
class ProbeWriterTest {

	@Test
	void testProbeWriter_counts_io_interactions_when_writing_large_text_to_disk() throws IOException {
		String payload = buildLargeParagraph();
		Path directFile = Files.createTempFile("probe-direct", ".txt");
		Path bufferedFile = Files.createTempFile("probe-buffered", ".txt");

		ProbeWriter directProbe = new ProbeWriter(
				new OutputStreamWriter(Files.newOutputStream(directFile), StandardCharsets.UTF_8));
		try (Writer out = directProbe) {
			for (int start = 0; start < payload.length(); start += 16) {
				int end = Math.min(start + 16, payload.length());
				out.write(payload.substring(start, end));
			}
		}

		ProbeWriter bufferedProbe = new ProbeWriter(
				new OutputStreamWriter(Files.newOutputStream(bufferedFile), StandardCharsets.UTF_8));
		try (BufferedWriter out = new BufferedWriter(bufferedProbe, 128)) {
			for (int start = 0; start < payload.length(); start += 16) {
				int end = Math.min(start + 16, payload.length());
				out.write(payload.substring(start, end));
			}
			out.flush();
		}

		assertThat(Files.readString(directFile, StandardCharsets.UTF_8)).isEqualTo(payload);
		assertThat(Files.readString(bufferedFile, StandardCharsets.UTF_8)).isEqualTo(payload);
		assertThat(directProbe.writeCalls()).isGreaterThan(1);
		assertThat(bufferedProbe.writeCalls()).isGreaterThan(0);
		assertThat(bufferedProbe.writeCalls()).isLessThan(directProbe.writeCalls());
		assertThat(directProbe.charsWritten()).isEqualTo(payload.length());
		assertThat(bufferedProbe.charsWritten()).isEqualTo(payload.length());
	}

	private static String buildLargeParagraph() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 300; i++) {
			sb.append(
					"This is a large paragraph used to verify that BufferedWriter coalesces many small writes into fewer IO interactions when storing data to disk. ");
		}
		return sb.toString();
	}

}

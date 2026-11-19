package com.lind.foundation.io;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BufferedFileCopyDemoTest {

	@TempDir
	Path dir;

	@Test
	void writeThenRead_roundTripsUtf8Lines() throws Exception {
		Path file = dir.resolve("notes.txt");
		List<String> lines = List.of("hello", "中文行", "line-3");

		BufferedFileCopyDemos.writeLinesToFile(file, lines);
		assertThat(Files.exists(file)).isTrue();
		assertThat(BufferedFileCopyDemos.readLinesFromFile(file)).isEqualTo(lines);
		assertThat(Files.readString(file, StandardCharsets.UTF_8)).contains("中文行");
	}

	@Test
	void copyFileLineByLine_preservesContent() throws Exception {
		Path src = dir.resolve("src.txt");
		Path dst = dir.resolve("dst.txt");
		BufferedFileCopyDemos.writeLinesToFile(src, List.of("a", "b", "拷贝"));

		int n = BufferedFileCopyDemos.copyFileLineByLine(src, dst);
		assertThat(n).isEqualTo(3);
		assertThat(BufferedFileCopyDemos.readLinesFromFile(dst)).containsExactly("a", "b", "拷贝");
	}

	@Test
	void bufferedWriter_reducesDownstreamWrites_toFile() throws Exception {
		var cmp = BufferedFileCopyDemos.compareWriteCallsToFile(dir.resolve("raw.txt"), dir.resolve("buf.txt"), 100,
				64);
		assertThat(cmp.withBufferDownstreamWrites()).isLessThan(cmp.withoutBufferDownstreamWrites());
		assertThat(cmp.withBufferDownstreamWrites()).isLessThan(cmp.lines());
		// 两边最终文件内容一致（忽略换行符差异时至少都非空且含首行）
		assertThat(Files.readString(dir.resolve("buf.txt"), StandardCharsets.UTF_8)).contains("L0");
		assertThat(Files.readString(dir.resolve("raw.txt"), StandardCharsets.UTF_8)).contains("L0");
	}

}

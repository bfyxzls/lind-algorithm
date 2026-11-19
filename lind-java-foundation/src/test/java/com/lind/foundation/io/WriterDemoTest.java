package com.lind.foundation.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WriterDemoTest {

	@Test
	void stringWriter_collectsCharsInMemory() throws Exception {
		assertThat(WriterDemos.demoStringWriter()).isEqualTo("hello Writer");
	}

	@Test
	void countingWriter_decoratesWithoutChangingPayload() throws Exception {
		assertThat(WriterDemos.demoCountingWriter()).isEqualTo("status=OPEN".length());
	}

	@Test
	void bufferedFileWriter_usesUtf8(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("out.txt");
		String content = WriterDemos.demoBufferedFileWriter(file);
		assertThat(content).contains("{\"ok\":true}").contains("中文必须走 UTF-8");
	}

	@Test
	void outputStreamWriter_encodesUtf8Bytes() throws Exception {
		byte[] bytes = WriterDemos.demoOutputStreamWriter();
		assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo("café");
		assertThat(bytes).isNotEqualTo("café".getBytes(StandardCharsets.ISO_8859_1));
	}

	@Test
	void jsonMapper_writesThroughWriterAbstraction() throws Exception {
		assertThat(WriterDemos.demoJsonMapperToWriter()).contains("\"id\":1").contains("Writer 抽象");
	}

	@Test
	void bufferedWriter_coalesces_many_small_writes() throws Exception {
		var cmp = WriterDemos.demoBufferedWriterCoalesce();
		// 100 次 write("tok")：直连下游 ≈ 100 次；经 BufferedWriter(64) ≈ ceil(300/64)=5 次
		assertThat(cmp.directWriteCalls()).isEqualTo(cmp.chunks());
		assertThat(cmp.bufferedWriteCalls()).isLessThan(cmp.directWriteCalls());
		assertThat(cmp.bufferedWriteCalls()).isEqualTo(5);
		assertThat(cmp.totalChars()).isEqualTo(300);
	}

	@Test
	void socket_request_with_bufferedWriter_roundTrip() throws Exception {
		var ex = WriterDemos.demoSocketRequestWithBufferedWriter();
		assertThat(ex.requestLine()).isEqualTo("GET /toy HTTP/1.0");
		assertThat(ex.responseLine()).isEqualTo("ACK GET /toy HTTP/1.0");
		// 多次 write + 一次 flush：触达 socket 的 write 远少于「每个字段一次」
		assertThat(ex.clientUnderlyingWrites()).isGreaterThan(0).isLessThan(10);
	}

	@Test
	void socket_without_flush_peer_cannot_read() throws Exception {
		assertThat(WriterDemos.demoSocketWithoutFlushBlocksPeer()).isTrue();
	}

	@Test
	void check_from_index_size() {
		char[] cbuf = new char[100];
		Objects.checkFromIndexSize(10, 20, cbuf.length);
		Objects.checkFromIndexSize(90, 10, cbuf.length);

		assertThrows(IndexOutOfBoundsException.class, () -> Objects.checkFromIndexSize(90, 20, cbuf.length));
		assertThrows(IndexOutOfBoundsException.class, () -> Objects.checkFromIndexSize(-1, 10, cbuf.length));
	}

}

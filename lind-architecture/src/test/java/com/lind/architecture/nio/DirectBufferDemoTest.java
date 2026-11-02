package com.lind.architecture.nio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DirectBufferDemoTest {

	@Test
	void heap_hasArray_direct_doesNot() {
		assertThat(DirectBufferDemos.demoHeapVsDirect()).contains("heap.isDirect=false").contains("hasArray=true")
				.contains("direct.isDirect=true").contains("hasArray=false");
	}

	@Test
	void flip_then_clear_reuses_same_direct_buffer() {
		assertThat(DirectBufferDemos.demoFlipClear()).isEqualTo("ping->pong");

		ByteBuffer buf = ByteBuffer.allocateDirect(32);
		buf.put("ab".getBytes(StandardCharsets.UTF_8));
		assertThat(buf.position()).isEqualTo(2);
		assertThat(buf.limit()).isEqualTo(32);

		buf.flip();
		assertThat(buf.position()).isEqualTo(0);
		assertThat(buf.limit()).isEqualTo(2);

		buf.clear();
		assertThat(buf.position()).isEqualTo(0);
		assertThat(buf.limit()).isEqualTo(32);
	}

	@Test
	void coalesce_reduces_flush_count_vs_per_chunk() {
		var stats = DirectBufferDemos.demoCoalesce(20, 100, 1400);
		// 20 * 100 = 2000 bytes → 阈值 1400 → 约 2 次 flush，远少于 20
		assertThat(stats.perChunkFlushes()).isEqualTo(20);
		assertThat(stats.coalescedFlushes()).isLessThan(stats.perChunkFlushes());
		assertThat(stats.coalescedFlushes()).isEqualTo(2);
	}

	@Test
	void transferTo_copies_file_payload(@TempDir Path dir) throws Exception {
		Path src = dir.resolve("src.txt");
		Path dst = dir.resolve("dst.txt");
		long n = DirectBufferDemos.demoTransferTo(src, dst);
		assertThat(n).isEqualTo(Files.size(src));
		assertThat(Files.readString(dst, StandardCharsets.UTF_8))
				.isEqualTo(Files.readString(src, StandardCharsets.UTF_8));
	}

	@Test
	void tokens_into_direct_flush_by_threshold() {
		List<Integer> sizes = DirectBufferDemos.demoTokensIntoDirect(List.of("ABCDEFGH", "IJK"), 8);
		assertThat(sizes).isNotEmpty();
		assertThat(sizes.stream().mapToInt(Integer::intValue).sum())
				.isEqualTo("ABCDEFGHIJK".getBytes(StandardCharsets.UTF_8).length);
		assertThat(sizes.get(0)).isEqualTo(8);
	}

}

package com.lind.depth.mmap;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 场景二对照实验：堆内暴力索引 vs IVF+PQ+mmap。
 *
 * <p>
 * 读前建议先看 {@code docs/工程化分析.md} 场景二，以及 {@link HeapVectorIndex} /
 * {@link QuantizedMmapIndex} 类注释。
 */
class QuantizedMmapIndexTest {

	@TempDir
	Path tempDir;

	/**
	 * 验证三条工程语义：
	 * <ol>
	 * <li>mmap 文件存在且可按行读</li>
	 * <li>量化后「热数据」（码本）远小于原始 float 全量估算</li>
	 * <li>近似检索能命中与暴力检索相近的近邻（小数据、同分布下）</li>
	 * </ol>
	 */
	@Test
	@DisplayName("mmap+PQ+IVF：冷数据落盘、热数据更小，且能找回近邻")
	void quantizedMmapFindsNeighborsWithLessHotMemory() throws Exception {
		int dim = 32;
		int n = 200;
		Random rnd = new Random(42);
		float[][] data = new float[n][];
		for (int i = 0; i < n; i++) {
			data[i] = Vectors.randomUnit(dim, rnd);
		}

		// ---------- 玩具：全进堆 ----------
		HeapVectorIndex heap = new HeapVectorIndex(dim);
		for (float[] v : data) {
			heap.add(v);
		}
		long heapBytes = heap.estimatedHeapBytes();

		// ---------- 工程：PQ 码本驻内存 + 编码 mmap ----------
		Path codeFile = tempDir.resolve("codes.bin");
		try (QuantizedMmapIndex index = QuantizedMmapIndex.build(data, codeFile, 8, 16, 8, 5, 7L)) {
			assertThat(codeFile).exists();
			assertThat(index.coldFileBytes()).isEqualTo((long) n * 8); // m=8 → 每向量 8 字节
			// 热数据（码本）应远小于「全量 float 进堆」
			assertThat(index.hotHeapBytes()).isLessThan(heapBytes / 4);

			float[] query = data[0];
			List<SearchHit> exact = heap.search(query, 5);
			List<SearchHit> approx = index.search(query, 5, 4);

			Set<Integer> exactIds = new HashSet<>();
			exact.forEach(h -> exactIds.add(h.id()));
			Set<Integer> approxIds = new HashSet<>();
			approx.forEach(h -> approxIds.add(h.id()));

			// 小数据 + 同分布：近似 Top5 与精确 Top5 至少应有交集（教学断言，非工业精度保证）
			exactIds.retainAll(approxIds);
			assertThat(exactIds).isNotEmpty();
			// 查询自身应非常靠前（id=0）
			assertThat(approx.get(0).id()).isEqualTo(0);
		}
	}

	@Test
	@DisplayName("MmapFloatMatrix：写入后可读回原向量")
	void mmapFloatMatrixRoundTrip() throws Exception {
		float[][] rows = { { 1f, 2f, 3f }, { 4f, 5f, 6f } };
		Path file = tempDir.resolve("mat.bin");
		try (MmapFloatMatrix mat = MmapFloatMatrix.create(file, rows)) {
			assertThat(mat.getRow(0)).containsExactly(1f, 2f, 3f);
			assertThat(mat.getRow(1)).containsExactly(4f, 5f, 6f);
		}
		// 重新 open，证明数据在文件里而不依赖原堆数组
		try (MmapFloatMatrix mat = MmapFloatMatrix.open(file, 2, 3)) {
			assertThat(mat.getRow(1)[0]).isEqualTo(4f);
		}
	}

}

package com.lind.depth.mmap;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;

/**
 * <b>工程组合</b>：IVF 分桶 + PQ 压缩码 + mmap 存放编码。
 *
 * <p>
 * 热数据：PQ 码本（很小）驻堆；冷数据：每条向量的 m 字节编码在 mmap 文件中。 查询：先 IVF 选桶 → 对查询建 PQ 距离表 → 只扫候选桶内的 mmap
 * 编码行。
 *
 * <p>
 * 对照 {@link HeapVectorIndex}：后者把原始 float 全塞堆；本类刻意不持有 {@code float[][]}。
 */
public final class QuantizedMmapIndex implements AutoCloseable {

	private final ProductQuantizer pq;

	private final IvfIndex ivf;

	private final MmapCodeMatrix codes;

	private QuantizedMmapIndex(ProductQuantizer pq, IvfIndex ivf, MmapCodeMatrix codes) {
		this.pq = pq;
		this.ivf = ivf;
		this.codes = codes;
	}

	/**
	 * 训练 PQ + IVF，编码全部向量写入 {@code codePath} 并映射。
	 */
	public static QuantizedMmapIndex build(float[][] vectors, Path codePath, int m, int ksub, int nlist,
			int nprobeTrainIters, long seed) throws IOException {
		Objects.requireNonNull(vectors, "vectors");
		ProductQuantizer pq = ProductQuantizer.train(vectors, m, ksub, 8, seed);
		IvfIndex ivf = IvfIndex.trainAndAssign(vectors, nlist, nprobeTrainIters, seed + 1);
		byte[][] encoded = new byte[vectors.length][];
		for (int i = 0; i < vectors.length; i++) {
			encoded[i] = pq.encode(vectors[i]);
		}
		MmapCodeMatrix codes = MmapCodeMatrix.create(codePath, encoded);
		return new QuantizedMmapIndex(pq, ivf, codes);
	}

	public ProductQuantizer pq() {
		return pq;
	}

	public IvfIndex ivf() {
		return ivf;
	}

	public int size() {
		return codes.rows();
	}

	/** 码本堆占用（热）——应远小于原始 float 全量。 */
	public long hotHeapBytes() {
		return pq.codebookHeapBytes();
	}

	/** 编码文件大小（冷，mmap，不计入堆常驻）。 */
	public long coldFileBytes() {
		return (long) codes.rows() * codes.codeSize();
	}

	/**
	 * 近似 Top-K 检索。
	 * @param nprobe 探测多少个 IVF 桶
	 */
	public List<SearchHit> search(float[] query, int topK, int nprobe) {
		Objects.requireNonNull(query, "query");
		float[][] table = pq.distanceTable(query);
		int[] buckets = ivf.probe(query, nprobe);
		byte[] codeBuf = new byte[pq.m()];
		PriorityQueue<SearchHit> heap = new PriorityQueue<>(Collections.reverseOrder());
		for (int b : buckets) {
			for (int id : ivf.list(b)) {
				codes.getRow(id, codeBuf);
				float dist = ProductQuantizer.approximateDistance(table, codeBuf);
				SearchHit hit = new SearchHit(id, dist);
				if (heap.size() < topK) {
					heap.offer(hit);
				}
				else if (hit.distance() < heap.peek().distance()) {
					heap.poll();
					heap.offer(hit);
				}
			}
		}
		List<SearchHit> out = new ArrayList<>(heap);
		Collections.sort(out);
		return out;
	}

	@Override
	public void close() throws IOException {
		codes.close();
	}

}

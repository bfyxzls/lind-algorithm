package com.lind.depth.mmap;

import java.util.Objects;
import java.util.Random;

/**
 * <b>乘积量化（PQ）教学实现</b>。
 *
 * <p>
 * 思想（对应文档）：把 {@code dim} 维向量切成 {@code m} 段，每段在自己的码本里找最近中心， 只存 1 个字节的中心编号 → 整向量从
 * {@code dim * 4} 字节压到 {@code m} 字节。
 *
 * <p>
 * 例：dim=32, m=8 → 每段 4 维，编码后 8 字节（原 128 字节）。
 *
 * <p>
 * 检索用 ADC（非对称距离）：查询向量保留 float，对每个子空间预计算「查询子段 ↔ 各码字」距离表， 再按库内码字编号查表累加——不必还原完整 float 向量。
 */
public final class ProductQuantizer {

	/** 子空间数量 = 编码后字节数。 */
	private final int m;

	/** 每个子空间的码本大小（教学默认 ≤256，以便用 1 byte 表示）。 */
	private final int ksub;

	private final int dim;

	private final int dsub;

	/** codebooks[sub][centroid][dsub] */
	private final float[][][] codebooks;

	public ProductQuantizer(int dim, int m, int ksub, float[][][] codebooks) {
		if (dim <= 0 || m <= 0 || ksub <= 0) {
			throw new IllegalArgumentException("dim/m/ksub 必须 > 0");
		}
		if (dim % m != 0) {
			throw new IllegalArgumentException("dim 必须能被 m 整除");
		}
		if (ksub > 256) {
			throw new IllegalArgumentException("教学实现 ksub 不能超过 256（单字节编码）");
		}
		this.dim = dim;
		this.m = m;
		this.ksub = ksub;
		this.dsub = dim / m;
		this.codebooks = Objects.requireNonNull(codebooks, "codebooks");
	}

	public int dim() {
		return dim;
	}

	public int m() {
		return m;
	}

	public int ksub() {
		return ksub;
	}

	/**
	 * 在训练向量上用极简 K-Means（每子空间独立）得到码本。
	 */
	public static ProductQuantizer train(float[][] train, int m, int ksub, int iters, long seed) {
		Objects.requireNonNull(train, "train");
		if (train.length == 0) {
			throw new IllegalArgumentException("训练集为空");
		}
		int dim = train[0].length;
		if (dim % m != 0) {
			throw new IllegalArgumentException("dim 必须能被 m 整除");
		}
		int dsub = dim / m;
		Random rnd = new Random(seed);
		float[][][] books = new float[m][ksub][dsub];
		for (int s = 0; s < m; s++) {
			// 随机抽 ksub 个样本子段做初始中心
			for (int c = 0; c < ksub; c++) {
				float[] src = train[rnd.nextInt(train.length)];
				System.arraycopy(src, s * dsub, books[s][c], 0, dsub);
			}
			int[] assign = new int[train.length];
			for (int it = 0; it < iters; it++) {
				for (int i = 0; i < train.length; i++) {
					assign[i] = nearest(books[s], train[i], s * dsub, dsub);
				}
				float[][] sum = new float[ksub][dsub];
				int[] cnt = new int[ksub];
				for (int i = 0; i < train.length; i++) {
					int a = assign[i];
					cnt[a]++;
					for (int j = 0; j < dsub; j++) {
						sum[a][j] += train[i][s * dsub + j];
					}
				}
				for (int c = 0; c < ksub; c++) {
					if (cnt[c] == 0) {
						float[] src = train[rnd.nextInt(train.length)];
						System.arraycopy(src, s * dsub, books[s][c], 0, dsub);
						continue;
					}
					for (int j = 0; j < dsub; j++) {
						books[s][c][j] = sum[c][j] / cnt[c];
					}
				}
			}
		}
		return new ProductQuantizer(dim, m, ksub, books);
	}

	/** 编码为 m 字节（每字节一个码字下标）。 */
	public byte[] encode(float[] vector) {
		Objects.requireNonNull(vector, "vector");
		if (vector.length != dim) {
			throw new IllegalArgumentException("维度不一致");
		}
		byte[] codes = new byte[m];
		for (int s = 0; s < m; s++) {
			codes[s] = (byte) nearest(codebooks[s], vector, s * dsub, dsub);
		}
		return codes;
	}

	/**
	 * 预计算查询相对各子空间码本的距离表：{@code table[s][c]} = 查询第 s 段与码字 c 的 L2 平方。
	 */
	public float[][] distanceTable(float[] query) {
		Objects.requireNonNull(query, "query");
		if (query.length != dim) {
			throw new IllegalArgumentException("维度不一致");
		}
		float[][] table = new float[m][ksub];
		for (int s = 0; s < m; s++) {
			for (int c = 0; c < ksub; c++) {
				float sum = 0;
				for (int j = 0; j < dsub; j++) {
					float d = query[s * dsub + j] - codebooks[s][c][j];
					sum += d * d;
				}
				table[s][c] = sum;
			}
		}
		return table;
	}

	/** 用距离表估计编码向量与查询的近似 L2 平方。 */
	public static float approximateDistance(float[][] table, byte[] codes) {
		float sum = 0;
		for (int s = 0; s < codes.length; s++) {
			int c = codes[s] & 0xff;
			sum += table[s][c];
		}
		return sum;
	}

	private static int nearest(float[][] codebook, float[] vector, int offset, int dsub) {
		int best = 0;
		float bestDist = Float.POSITIVE_INFINITY;
		for (int c = 0; c < codebook.length; c++) {
			float sum = 0;
			for (int j = 0; j < dsub; j++) {
				float d = vector[offset + j] - codebook[c][j];
				sum += d * d;
			}
			if (sum < bestDist) {
				bestDist = sum;
				best = c;
			}
		}
		return best;
	}

	/** 码本常驻堆的粗略字节数（热数据）。 */
	public long codebookHeapBytes() {
		return (long) m * ksub * dsub * Float.BYTES;
	}

}

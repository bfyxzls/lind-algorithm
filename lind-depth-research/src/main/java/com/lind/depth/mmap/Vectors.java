package com.lind.depth.mmap;

import java.util.Objects;
import java.util.Random;

/**
 * 向量小工具：拷贝、L2 距离、随机生成（测试造数用）。
 */
public final class Vectors {

	private Vectors() {
	}

	public static float[] copy(float[] v) {
		Objects.requireNonNull(v, "v");
		float[] out = new float[v.length];
		System.arraycopy(v, 0, out, 0, v.length);
		return out;
	}

	/** 欧氏距离平方（检索排序用平方即可，少一次开方）。 */
	public static float l2Squared(float[] a, float[] b) {
		if (a.length != b.length) {
			throw new IllegalArgumentException("维度不一致");
		}
		float s = 0;
		for (int i = 0; i < a.length; i++) {
			float d = a[i] - b[i];
			s += d * d;
		}
		return s;
	}

	public static float[] randomUnit(int dim, Random rnd) {
		float[] v = new float[dim];
		float norm = 0;
		for (int i = 0; i < dim; i++) {
			v[i] = (float) rnd.nextGaussian();
			norm += v[i] * v[i];
		}
		norm = (float) Math.sqrt(norm);
		if (norm < 1e-8f) {
			v[0] = 1;
			return v;
		}
		for (int i = 0; i < dim; i++) {
			v[i] /= norm;
		}
		return v;
	}

}

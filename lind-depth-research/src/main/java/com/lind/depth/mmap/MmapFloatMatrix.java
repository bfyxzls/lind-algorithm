package com.lind.depth.mmap;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * 行主序 float 矩阵的 <b>mmap</b> 视图：数据在文件里，不整表加载进堆。
 *
 * <p>
 * 对应文档：
 *
 * <pre>
 * MappedByteBuffer index = fileChannel.map(READ_ONLY, 0, fileSize);
 * </pre>
 *
 * OS 按页调入；没访问到的冷页不占你的堆。本类负责建文件、映射、按行读取。
 *
 * <p>
 * 文件布局：连续存放 {@code rows * cols} 个 little-endian float32。
 */
public final class MmapFloatMatrix implements AutoCloseable {

	private final Path path;

	private final int rows;

	private final int cols;

	private final FileChannel channel;

	private final MappedByteBuffer map;

	private MmapFloatMatrix(Path path, int rows, int cols, FileChannel channel, MappedByteBuffer map) {
		this.path = path;
		this.rows = rows;
		this.cols = cols;
		this.channel = channel;
		this.map = map;
	}

	/**
	 * 把内存中的向量列表写成文件并映射为只读 mmap。
	 * @param path 目标文件（可覆盖）
	 * @param vectors 每行一条向量，维度必须一致
	 */
	public static MmapFloatMatrix create(Path path, float[][] vectors) throws IOException {
		Objects.requireNonNull(path, "path");
		Objects.requireNonNull(vectors, "vectors");
		if (vectors.length == 0) {
			throw new IllegalArgumentException("vectors 为空");
		}
		int cols = vectors[0].length;
		int rows = vectors.length;
		for (float[] row : vectors) {
			if (row.length != cols) {
				throw new IllegalArgumentException("向量维度不一致");
			}
		}
		long bytes = (long) rows * cols * Float.BYTES;
		Files.createDirectories(path.getParent() == null ? Path.of(".") : path.getParent());
		MappedByteBuffer buf = null;
		try (FileChannel writeCh = FileChannel.open(path, StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE, StandardOpenOption.READ)) {
			buf = writeCh.map(FileChannel.MapMode.READ_WRITE, 0, bytes);
			buf.order(ByteOrder.LITTLE_ENDIAN);
			for (float[] row : vectors) {
				for (float v : row) {
					buf.putFloat(v);
				}
			}
			buf.force();
		}
		finally {
			MmapUnmapper.unmap(buf);
		}
		return open(path, rows, cols);
	}

	/** 打开已有矩阵文件（调用方需已知 rows/cols，或自行约定元数据文件）。 */
	public static MmapFloatMatrix open(Path path, int rows, int cols) throws IOException {
		FileChannel channel = FileChannel.open(path, StandardOpenOption.READ);
		long expect = (long) rows * cols * Float.BYTES;
		if (channel.size() != expect) {
			channel.close();
			throw new IllegalArgumentException("文件大小与 rows*cols*4 不符: " + channel.size() + " vs " + expect);
		}
		MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_ONLY, 0, expect);
		map.order(ByteOrder.LITTLE_ENDIAN);
		return new MmapFloatMatrix(path, rows, cols, channel, map);
	}

	public int rows() {
		return rows;
	}

	public int cols() {
		return cols;
	}

	public Path path() {
		return path;
	}

	/**
	 * 读取第 {@code row} 行到调用方提供的数组（或新建）。
	 * <p>
	 * 注意：每次仍会在堆上得到一个 {@code float[]} 副本——这是「读一行」的代价； 与「千万行全部常驻堆」不是一个量级。量化索引会进一步让冷数据以 byte
	 * 码形式存在 mmap 中。
	 */
	public float[] getRow(int row, float[] reuse) {
		checkRow(row);
		float[] out = reuse != null && reuse.length == cols ? reuse : new float[cols];
		int base = row * cols * Float.BYTES;
		for (int j = 0; j < cols; j++) {
			out[j] = map.getFloat(base + j * Float.BYTES);
		}
		return out;
	}

	public float[] getRow(int row) {
		return getRow(row, null);
	}

	private void checkRow(int row) {
		if (row < 0 || row >= rows) {
			throw new IndexOutOfBoundsException("row=" + row + ", rows=" + rows);
		}
	}

	@Override
	public void close() throws IOException {
		MmapUnmapper.unmap(map);
		channel.close();
	}

}

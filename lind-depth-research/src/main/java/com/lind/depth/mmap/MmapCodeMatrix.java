package com.lind.depth.mmap;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * PQ 编码矩阵的 mmap：每行 {@code codeSize} 字节，不进堆常驻。
 *
 * <p>
 * 这是文档里「冷数据走 mmap」的落点——千万行编码可以很大，但只有被扫到的页才进 OS 页缓存。
 */
public final class MmapCodeMatrix implements AutoCloseable {

	private final Path path;

	private final int rows;

	private final int codeSize;

	private final FileChannel channel;

	private final MappedByteBuffer map;

	private MmapCodeMatrix(Path path, int rows, int codeSize, FileChannel channel, MappedByteBuffer map) {
		this.path = path;
		this.rows = rows;
		this.codeSize = codeSize;
		this.channel = channel;
		this.map = map;
	}

	public static MmapCodeMatrix create(Path path, byte[][] codes) throws IOException {
		Objects.requireNonNull(path, "path");
		Objects.requireNonNull(codes, "codes");
		if (codes.length == 0) {
			throw new IllegalArgumentException("codes 为空");
		}
		int codeSize = codes[0].length;
		for (byte[] row : codes) {
			if (row.length != codeSize) {
				throw new IllegalArgumentException("编码长度不一致");
			}
		}
		Files.createDirectories(path.getParent() == null ? Path.of(".") : path.getParent());
		long bytes = (long) codes.length * codeSize;
		MappedByteBuffer buf = null;
		try (FileChannel writeCh = FileChannel.open(path, StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE, StandardOpenOption.READ)) {
			buf = writeCh.map(FileChannel.MapMode.READ_WRITE, 0, bytes);
			for (byte[] row : codes) {
				buf.put(row);
			}
			buf.force();
		}
		finally {
			MmapUnmapper.unmap(buf);
		}
		return open(path, codes.length, codeSize);
	}

	public static MmapCodeMatrix open(Path path, int rows, int codeSize) throws IOException {
		FileChannel channel = FileChannel.open(path, StandardOpenOption.READ);
		long expect = (long) rows * codeSize;
		if (channel.size() != expect) {
			channel.close();
			throw new IllegalArgumentException("文件大小不符");
		}
		MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_ONLY, 0, expect);
		return new MmapCodeMatrix(path, rows, codeSize, channel, map);
	}

	public int rows() {
		return rows;
	}

	public int codeSize() {
		return codeSize;
	}

	public Path path() {
		return path;
	}

	public void getRow(int row, byte[] out) {
		if (row < 0 || row >= rows) {
			throw new IndexOutOfBoundsException("row=" + row);
		}
		if (out.length < codeSize) {
			throw new IllegalArgumentException("out 太短");
		}
		int base = row * codeSize;
		for (int i = 0; i < codeSize; i++) {
			out[i] = map.get(base + i);
		}
	}

	@Override
	public void close() throws IOException {
		MmapUnmapper.unmap(map);
		channel.close();
	}

}

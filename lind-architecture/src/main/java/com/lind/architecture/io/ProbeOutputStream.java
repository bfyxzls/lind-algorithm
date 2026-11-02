package com.lind.architecture.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 探针输出流：统计「真正触达下游」的 {@code write} 次数，用来对照 BufferedWriter 攒包效果。
 * <p>
 * 字节出口
 */
public final class ProbeOutputStream extends OutputStream {

	private final OutputStream downstream;

	private final AtomicInteger writeCalls = new AtomicInteger();

	private final AtomicInteger bytesWritten = new AtomicInteger();

	public ProbeOutputStream() {
		this(new ByteArrayOutputStream());
	}

	public ProbeOutputStream(OutputStream downstream) {
		this.downstream = Objects.requireNonNull(downstream, "downstream");
	}

	public int writeCalls() {
		return writeCalls.get();
	}

	public int bytesWritten() {
		return bytesWritten.get();
	}

	/**
	 * 将输出流转换为字节数组
	 * @return all bytes
	 */
	public byte[] toByteArray() {
		if (downstream instanceof ByteArrayOutputStream baos) {
			return baos.toByteArray();
		}
		throw new UnsupportedOperationException("downstream is not ByteArrayOutputStream");
	}

	@Override
	public void write(int b) throws IOException {
		writeCalls.incrementAndGet();
		bytesWritten.incrementAndGet();
		downstream.write(b);
	}

	/**
	 * write将节点写到输出流
	 * @param b the data.
	 * @param off the start offset in the data.
	 * @param len the number of bytes to write.
	 * @throws IOException
	 */
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		Objects.checkFromIndexSize(off, len, b.length);
		writeCalls.incrementAndGet();
		bytesWritten.addAndGet(len);
		downstream.write(b, off, len);
	}

	@Override
	public void flush() throws IOException {
		downstream.flush();
	}

	@Override
	public void close() throws IOException {
		downstream.close();
	}

}

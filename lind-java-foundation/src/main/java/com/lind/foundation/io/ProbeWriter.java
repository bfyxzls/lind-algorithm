package com.lind.foundation.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 探针 Writer：统计上游（如 {@link java.io.BufferedWriter}）触达本层的 {@code write} 次数。
 * <p>
 * 字符出口 用来证明攒包发生在「字符层」；不宜只探 {@link java.io.OutputStream}，因为
 * {@link java.io.OutputStreamWriter} 自己也有编码缓冲，会把对比干扰掉。
 */
public final class ProbeWriter extends Writer {

	private final static Logger logger = LoggerFactory.getLogger(ProbeWriter.class);

	private final Writer downstream;

	private final AtomicInteger writeCalls = new AtomicInteger();

	private final AtomicInteger charsWritten = new AtomicInteger();

	public ProbeWriter(Writer downstream) {
		this.downstream = Objects.requireNonNull(downstream, "downstream");
	}

	public int writeCalls() {
		return writeCalls.get();
	}

	public int charsWritten() {
		return charsWritten.get();
	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		Objects.checkFromIndexSize(off, len, cbuf.length);
		writeCalls.incrementAndGet();
		charsWritten.addAndGet(len);
		downstream.write(cbuf, off, len);
	}

	/**
	 * 只有上游显式 {@code flush()}，或 {@link java.io.BufferedWriter#flush()} 时才会进这里。
	 * <p>
	 * 注意：JDK {@code BufferedWriter.close()} <b>不会</b>调用下游 {@code flush()}，只把残余字符
	 * {@code write} 出去再 {@code close} 下游——所以
	 * {@code WriterDemos.demoBufferedWriterCoalesce()} 里若只靠 try-with-resources
	 * 关闭，断点打在本方法上是命不中的；请打在 {@link #write} 或显式 {@code bufferedWriter.flush()}。
	 */
	@Override
	public void flush() throws IOException {
		downstream.flush();
	}

	@Override
	public void close() throws IOException {
		// 装饰器惯例：先 flush 再关；但上游若是 BufferedWriter.close()，它根本不会走到这里的 flush，
		// 而是直接 close(this)，因此仍建议在协议边界显式 flush。
		flush();
		downstream.close();
	}

}

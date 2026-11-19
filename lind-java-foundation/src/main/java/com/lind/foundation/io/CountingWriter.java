package com.lind.foundation.io;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;

/**
 * 装饰器 Writer：统计写出的字符数，并把数据交给下游。
 * <p>
 * 用来理解「Writer 只是字符出口合同」——计数、加密、限流都可以做成一层装饰，不必改业务代码。
 */
public final class CountingWriter extends Writer {

	private final Writer downstream;

	private long charsWritten;

	private boolean closed;

	public CountingWriter(Writer downstream) {
		this.downstream = Objects.requireNonNull(downstream, "downstream");
	}

	public long charsWritten() {
		return charsWritten;
	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		ensureOpen();
		Objects.checkFromIndexSize(off, len, cbuf.length);
		downstream.write(cbuf, off, len);
		charsWritten += len;
	}

	@Override
	public void flush() throws IOException {
		ensureOpen();
		downstream.flush();
	}

	@Override
	public void close() throws IOException {
		if (closed) {
			return;
		}
		closed = true;
		downstream.close();
	}

	private void ensureOpen() throws IOException {
		if (closed) {
			throw new IOException("CountingWriter already closed");
		}
	}

}

package com.lind.architecture.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * 四大家族最小对照：同一段 {@code Hello} 走字节出口、字符出口、Charset 桥、再读回。
 * <p>
 * 概念见 {@code mental-model.md}。
 */
public final class StreamFamiliesDemo {

	public static final String PAYLOAD = "Hello";

	private StreamFamiliesDemo() {
	}

	/** 1) OutputStream：直接写 UTF-8 字节。 */
	public static byte[] viaOutputStream() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(PAYLOAD.getBytes(StandardCharsets.UTF_8));
		return out.toByteArray();
	}

	/** 2) Writer：字符落在内存（不经字节）。 */
	public static String viaWriter() throws IOException {
		try (StringWriter w = new StringWriter()) {
			w.write(PAYLOAD);
			return w.toString();
		}
	}

	/**
	 * 3) 桥：Writer → OutputStreamWriter(UTF-8) → OutputStream。
	 * <p>
	 * 业务仍写字符；真正出口是字节。可选套 {@link ProbeWriter} 观察触达下游的 write 次数。
	 */
	public static byte[] viaWriterBridgeToOutputStream() throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (Writer w = new OutputStreamWriter(bytes, StandardCharsets.UTF_8)) {
			w.write(PAYLOAD);
		}
		return bytes.toByteArray();
	}

	/**
	 * 同 (3)，中间插 ProbeWriter：业务 → ProbeWriter → OutputStreamWriter → bytes。
	 * @return [utf8Bytes, probeWriteCalls]
	 */
	public static BridgeWithProbe viaWriterBridgeWithProbe() throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ProbeWriter probe = new ProbeWriter(new OutputStreamWriter(bytes, StandardCharsets.UTF_8));
		try (Writer w = probe) {
			w.write(PAYLOAD);
		}
		return new BridgeWithProbe(bytes.toByteArray(), probe.writeCalls());
	}

	public record BridgeWithProbe(byte[] utf8, int probeWriteCalls) {
	}

	/** 4) 对称读回：字节 → InputStreamReader → Reader。 */
	public static String viaReaderBridgeFromBytes(byte[] utf8) throws IOException {
		try (Reader r = new InputStreamReader(new ByteArrayInputStream(utf8), StandardCharsets.UTF_8)) {
			char[] buf = new char[64];
			int n = r.read(buf);
			return n < 0 ? "" : new String(buf, 0, n);
		}
	}

	/** 一次跑通：写出桥 + 读回，确认往返一致。 */
	public static String roundTripThroughCharsetBridge() throws IOException {
		byte[] utf8 = viaWriterBridgeToOutputStream();
		return viaReaderBridgeFromBytes(utf8);
	}

}

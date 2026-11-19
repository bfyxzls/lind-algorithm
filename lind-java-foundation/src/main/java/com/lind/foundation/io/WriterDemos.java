package com.lind.foundation.io;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * {@link java.io.Writer} 说明性 demo。详细概念见同包 {@code readme.md}。
 * <p>
 * 可直接 {@code main} 跑；断言版见测试 {@code WriterDemoTest}。
 */
public final class WriterDemos {

	private WriterDemos() {
	}

	/** 1) 内存出口：单测 / 小结果最常用。 */
	public static String demoStringWriter() throws IOException {
		try (StringWriter sw = new StringWriter()) {
			sw.write("hello");
			sw.write(' ');
			sw.append("Writer");
			return sw.toString();
		}
	}

	/** 2) 装饰器：同一段业务写出，顺带计量。 */
	public static long demoCountingWriter() throws IOException {
		try (StringWriter sink = new StringWriter(); CountingWriter counting = new CountingWriter(sink)) {
			counting.write("status");
			counting.write('=');
			counting.write("OPEN");
			counting.flush();
			return counting.charsWritten();
		}
	}

	/**
	 * 3) 文件出口：显式 UTF-8 + 缓冲（现代写法）。
	 * @return 读回的文件内容
	 */
	public static String demoBufferedFileWriter(Path file) throws IOException {
		try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
			w.write("{\"ok\":true}");
			w.newLine();
			w.write("第二行：中文必须走 UTF-8");
		}
		return Files.readString(file, StandardCharsets.UTF_8);
	}

	/**
	 * 4) 字节出口适配：先有 OutputStream，再包成 Writer（Socket / 框架流常见）。
	 */
	public static byte[] demoOutputStreamWriter() throws IOException {
		java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
		try (Writer w = new OutputStreamWriter(bytes, StandardCharsets.UTF_8)) {
			w.write("café");
		}
		return bytes.toByteArray();
	}

	/**
	 * 5) 序列化只认 Writer：同一套写出可落到 StringWriter / 文件 / Socket。
	 */
	public static String demoJsonMapperToWriter() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> doc = new LinkedHashMap<>();
		doc.put("id", 1);
		doc.put("title", "Writer 抽象");

		try (StringWriter sw = new StringWriter()) {
			mapper.writeValue(sw, doc);
			return sw.toString();
		}
	}

	public static CoalesceCompare demoBufferedWriterCoalesce() throws IOException {
		final int chunks = 100;
		final String piece = "tok"; // 3 字符；100 次共 300 字符
		final int bufferSize = 64; // 故意调小，便于观察「攒满再刷」

		ProbeWriter directProbe = new ProbeWriter(new StringWriter());
		try (Writer w = directProbe) {
			for (int i = 0; i < chunks; i++) {
				w.write(piece); // Writer.write(String) → 每次都会 write(char[],...)
			}
		}

		ProbeWriter bufferedProbe = new ProbeWriter(new StringWriter());
		try (BufferedWriter w = new BufferedWriter(bufferedProbe, bufferSize)) {
			for (int i = 0; i < chunks; i++) {
				w.write(piece); // 先攒进 64 字符缓冲，满了才打到 ProbeWriter.write
			}
			// BufferedWriter.close() 只会把残余 write 给下游，不会调用下游 flush()。
			// 若要命中 ProbeWriter.flush()，必须显式：
			w.flush();
		}

		return new CoalesceCompare(chunks, directProbe.writeCalls(), bufferedProbe.writeCalls(), bufferSize,
				directProbe.charsWritten());
	}

	public static SocketExchange demoSocketRequestWithBufferedWriter() throws Exception {
		try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
			int port = server.getLocalPort();

			CompletableFuture<String> accepted = CompletableFuture.supplyAsync(() -> {
				try (Socket peer = server.accept();
						BufferedReader in = new BufferedReader(
								new InputStreamReader(peer.getInputStream(), StandardCharsets.UTF_8));
						BufferedWriter out = new BufferedWriter(
								new OutputStreamWriter(peer.getOutputStream(), StandardCharsets.UTF_8))) {
					String line = in.readLine();
					out.write("ACK ");
					out.write(line == null ? "" : line);
					out.newLine();
					out.flush(); // 服务端同样：不 flush，客户端读不到
					return line;
				}
				catch (IOException e) {
					throw new IllegalStateException(e);
				}
			});

			String request = "GET /toy HTTP/1.0";
			ProbeOutputStream probe;
			try (Socket client = new Socket(InetAddress.getLoopbackAddress(), port)) {
				probe = new ProbeOutputStream(client.getOutputStream());
				try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(probe, StandardCharsets.UTF_8),
						8192);
						BufferedReader in = new BufferedReader(
								new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))) {
					// 多次小 write：先攒在 BufferedWriter，未必立刻进 TCP
					out.write("GET ");
					out.write("/toy");
					out.write(" HTTP/1.0");
					out.newLine();
					out.flush(); // 协议边界：请求行发完，必须推给对端

					String response = in.readLine();
					String seenByServer = accepted.get(3, TimeUnit.SECONDS);
					if (!request.equals(seenByServer)) {
						throw new IllegalStateException("server saw: " + seenByServer);
					}
					return new SocketExchange(request, response, probe.writeCalls());
				}
			}
		}
	}

	/**
	 * 8) 反例：Socket 上写完不 flush —— 对端读不到（短超时后放弃）。
	 * @return true 表示在超时内没有读到行（证明数据仍卡在缓冲里）
	 */
	public static boolean demoSocketWithoutFlushBlocksPeer() throws Exception {
		try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
			int port = server.getLocalPort();

			CompletableFuture<Boolean> peerGotLine = CompletableFuture.supplyAsync(() -> {
				try (Socket peer = server.accept();
						BufferedReader in = new BufferedReader(
								new InputStreamReader(peer.getInputStream(), StandardCharsets.UTF_8))) {
					peer.setSoTimeout(200);
					try {
						return in.readLine() != null;
					}
					catch (java.net.SocketTimeoutException timeout) {
						return false; // 200ms 内没有完整行 → 仍堵在客户端缓冲
					}
				}
				catch (IOException e) {
					throw new IllegalStateException(e);
				}
			});

			try (Socket client = new Socket(InetAddress.getLoopbackAddress(), port);
					BufferedWriter out = new BufferedWriter(
							new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8))) {
				out.write("never-flushed");
				out.newLine();
				// 故意不 flush、先等对端超时结论，再 close（close 会 flush，故必须先 join）
				boolean got = peerGotLine.get(2, TimeUnit.SECONDS);
				return !got;
			}
		}
	}

	/**
	 * 6) BufferedWriter 攒包对照：同样 100 次短 write，看触达「下一层 Writer」的次数。
	 * <p>
	 * 探针挂在 BufferedWriter 与下游之间（{@link ProbeWriter}），而不是 OutputStream——因为
	 * {@link OutputStreamWriter} 自己也有编码缓冲，会把对比干扰成「两边都是 1 次」。
	 * <p>
	 * 链路：业务 write → BufferedWriter(char[]) → ProbeWriter → StringWriter。
	 */
	public record CoalesceCompare(int chunks, int directWriteCalls, int bufferedWriteCalls, int bufferSizeChars,
			int totalChars) {
	}

	/**
	 * 7) Socket 文本请求：本机 ServerSocket 回显一行。
	 * <p>
	 * 标准栈： {@code BufferedWriter → OutputStreamWriter(UTF-8) → socket.getOutputStream()}。
	 * <p>
	 * <b>关键：</b>写完一行后必须 {@code flush()}，否则数据可能仍在 BufferedWriter 的 char[] 里，对端
	 * {@code readLine} 会一直阻塞。
	 */
	public record SocketExchange(String requestLine, String responseLine, int clientUnderlyingWrites) {
	}

}

package com.lind.architecture.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link BufferedReader} / {@link BufferedWriter} 读写<strong>物理文件</strong> demo。
 * <p>
 * 概念见同包 {@code buffered-file.md}。
 */
public final class BufferedFileCopyDemos {

	private BufferedFileCopyDemos() {
	}

	/**
	 * 1) 用 BufferedWriter 把多行文本写到物理文件（UTF-8）。
	 * <p>
	 * 栈：业务字符 → BufferedWriter →（Files 内部）OutputStreamWriter → 文件字节。
	 */
	public static void writeLinesToFile(Path file, List<String> lines) throws IOException {
		try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
			for (String line : lines) {
				out.write(line);
				out.newLine(); // 按平台换行符写入，比手写 "\n" 更稳妥
			}
			// 一般不必每行 flush；close 时会刷出残余缓冲
		}
	}

	/**
	 * 2) 用 BufferedReader 从物理文件按行读回。
	 * <p>
	 * 栈：文件字节 → InputStreamReader(UTF_8) → BufferedReader 预读 → readLine。
	 */
	public static List<String> readLinesFromFile(Path file) throws IOException {
		List<String> lines = new ArrayList<>();
		try (BufferedReader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			String line;
			while ((line = in.readLine()) != null) {
				lines.add(line);
			}
		}
		return lines;
	}

	/**
	 * 3) 经典场景：从本机源文件读 → 写到另一个物理文件（流式拷贝，不整文件进内存）。
	 */
	public static int copyFileLineByLine(Path source, Path target) throws IOException {
		int lineCount = 0;
		try (BufferedReader in = Files.newBufferedReader(source, StandardCharsets.UTF_8);
				BufferedWriter out = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
			String line;
			while ((line = in.readLine()) != null) {
				out.write(line);
				out.newLine();
				lineCount++;
			}
		}
		return lineCount;
	}

	/**
	 * 4) 对照：同样「很多短行」写文件，有无 BufferedWriter 时触达下游 Writer 的次数。
	 * <p>
	 * 探针挂在缓冲层与「真正写文件的 Writer」之间，证明攒包减少下游 write。
	 */
	public record FileWriteCompare(int lines, int withoutBufferDownstreamWrites, int withBufferDownstreamWrites,
			int bufferSizeChars) {
	}

	public static FileWriteCompare compareWriteCallsToFile(Path withoutBufFile, Path withBufFile, int lines,
			int bufferSizeChars) throws IOException {
		List<String> payload = new ArrayList<>(lines);
		for (int i = 0; i < lines; i++) {
			payload.add("L" + i);
		}

		// 无 BufferedWriter：每个短行都可能直接打到下游（再经编码进文件）
		ProbeWriter directProbe = new ProbeWriter(
				new OutputStreamWriter(Files.newOutputStream(withoutBufFile), StandardCharsets.UTF_8));
		try (Writer out = directProbe) {
			for (String line : payload) {
				out.write(line);
				out.write('\n');
			}
		}

		// 有 BufferedWriter：先攒满 bufferSizeChars（或 close）再打到 ProbeWriter
		ProbeWriter bufferedProbe = new ProbeWriter(
				new OutputStreamWriter(Files.newOutputStream(withBufFile), StandardCharsets.UTF_8));
		try (BufferedWriter out = new BufferedWriter(bufferedProbe, bufferSizeChars)) {
			for (String line : payload) {
				out.write(line);
				out.newLine();
			}
			out.flush(); // 显式刷尾包，保证探针统计完整；也方便对照 Socket 协议边界习惯
		}

		return new FileWriteCompare(lines, directProbe.writeCalls(), bufferedProbe.writeCalls(), bufferSizeChars);
	}

	public static void main(String[] args) throws Exception {
		Path dir = Files.createTempDirectory("buf-file-demo");
		try {
			Path src = dir.resolve("source.txt");
			Path dst = dir.resolve("copy.txt");
			List<String> lines = List.of("第一行：BufferedWriter 写出", "第二行：中文 UTF-8", "第三行：再经 BufferedReader 读回");

			writeLinesToFile(src, lines);
			System.out.println("1) wrote -> " + src.toAbsolutePath());
			System.out.println("2) read  = " + readLinesFromFile(src));

			int n = copyFileLineByLine(src, dst);
			System.out.println("3) copied " + n + " lines -> " + dst.toAbsolutePath());
			System.out.println("   copy content = " + readLinesFromFile(dst));

			FileWriteCompare cmp = compareWriteCallsToFile(dir.resolve("raw.txt"), dir.resolve("buf.txt"), 100, 64);
			System.out.println("4) lines=" + cmp.lines() + ", downstreamWrites withoutBuf="
					+ cmp.withoutBufferDownstreamWrites() + ", withBuf=" + cmp.withBufferDownstreamWrites()
					+ " (bufChars=" + cmp.bufferSizeChars() + ")");
		}
		finally {
			Files.walk(dir).sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.deleteIfExists(p);
				}
				catch (IOException ignored) {
				}
			});
		}
	}

}

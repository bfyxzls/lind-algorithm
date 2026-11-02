package com.lind.architecture.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * 零拷贝 / DirectBuffer 说明性 demo。概念见同包 {@code readme.md}。
 */
public final class DirectBufferDemos {

	private DirectBufferDemos() {
	}

	/** 1) 堆内 vs 堆外：能力差异一眼可见。 */
	public static String demoHeapVsDirect() {
		ByteBuffer heap = ByteBuffer.allocate(16);
		ByteBuffer direct = ByteBuffer.allocateDirect(16);
		heap.put((byte) 1);
		direct.put((byte) 1);
		return "heap.isDirect=" + heap.isDirect() + ", hasArray=" + heap.hasArray() + "; direct.isDirect="
				+ direct.isDirect() + ", hasArray=" + direct.hasArray();
	}

	/**
	 * 2) flip / clear 状态机（和 CoalescingStreamWriter 相同）。
	 * @return 读出的 UTF-8 字符串
	 */
	public static String demoFlipClear() {
		ByteBuffer buf = ByteBuffer.allocateDirect(64);
		buf.put("ping".getBytes(StandardCharsets.UTF_8));
		// 写完：position=4, limit=64
		buf.flip(); // 可读：position=0, limit=4
		byte[] out = new byte[buf.remaining()];
		buf.get(out);
		buf.clear(); // 复用同一块堆外内存
		buf.put("pong".getBytes(StandardCharsets.UTF_8));
		buf.flip();
		byte[] out2 = new byte[buf.remaining()];
		buf.get(out2);
		return new String(out, StandardCharsets.UTF_8) + "->" + new String(out2, StandardCharsets.UTF_8);
	}

	/**
	 * 3) 攒包：同样 20 段 payload，对比「每段一刷」vs「满阈值再刷」的 flush 次数。
	 */
	public static record CoalesceStats(int perChunkFlushes, int coalescedFlushes, int threshold) {
	}

	public static CoalesceStats demoCoalesce(int chunkCount, int chunkBytes, int threshold) {
		byte[] chunk = new byte[chunkBytes];
		for (int i = 0; i < chunkBytes; i++) {
			chunk[i] = (byte) ('a' + (i % 26));
		}

		int perChunk = 0;
		for (int i = 0; i < chunkCount; i++) {
			perChunk++; // 每段立刻当一次 flush
		}

		ByteBuffer direct = ByteBuffer.allocateDirect(Math.max(threshold * 2, chunkBytes * 2));
		int coalesced = 0;
		for (int i = 0; i < chunkCount; i++) {
			if (direct.remaining() < chunkBytes) {
				direct.flip();
				coalesced++; // 模拟 channel.write
				direct.clear();
			}
			direct.put(chunk);
			if (direct.position() >= threshold) {
				direct.flip();
				coalesced++;
				direct.clear();
			}
		}
		if (direct.position() > 0) {
			direct.flip();
			coalesced++;
			direct.clear();
		}
		return new CoalesceStats(perChunk, coalesced, threshold);
	}

	/**
	 * 4) FileChannel.transferTo：API 形态上的「零拷贝传文件」（底层常走 sendfile）。
	 * @return 传输的字节数
	 */
	public static long demoTransferTo(Path src, Path dst) throws IOException {
		Files.writeString(src, "zero-copy-transfer-demo\n", StandardCharsets.UTF_8);
		try (FileChannel in = FileChannel.open(src, StandardOpenOption.READ);
				FileChannel out = FileChannel.open(dst, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
						StandardOpenOption.TRUNCATE_EXISTING)) {
			long size = in.size();
			long transferred = 0;
			while (transferred < size) {
				transferred += in.transferTo(transferred, size - transferred, out);
			}
			return transferred;
		}
	}

	/**
	 * 5) 从「字符」跨到 DirectBuffer：Writer 篇的下一跳（仍含一次堆内 getBytes，教学可接受）。
	 */
	public static List<Integer> demoTokensIntoDirect(List<String> tokens, int threshold) {
		ByteBuffer buf = ByteBuffer.allocateDirect(Math.max(threshold * 2, 256));
		List<Integer> flushSizes = new ArrayList<>();
		for (String token : tokens) {
			byte[] bytes = token.getBytes(StandardCharsets.UTF_8);
			int offset = 0;
			while (offset < bytes.length) {
				if (!buf.hasRemaining()) {
					flushSizes.add(drain(buf));
				}
				int n = Math.min(buf.remaining(), bytes.length - offset);
				buf.put(bytes, offset, n);
				offset += n;
				if (buf.position() >= threshold) {
					flushSizes.add(drain(buf));
				}
			}
		}
		if (buf.position() > 0) {
			flushSizes.add(drain(buf));
		}
		return flushSizes;
	}

	private static int drain(ByteBuffer buf) {
		buf.flip();
		int n = buf.remaining();
		buf.clear();
		return n;
	}

	public static void main(String[] args) throws Exception {
		System.out.println("1) " + demoHeapVsDirect());
		System.out.println("2) flip/clear = " + demoFlipClear());
		CoalesceStats s = demoCoalesce(20, 100, 1400);
		System.out.println("3) coalesce: perChunkFlushes=" + s.perChunkFlushes() + ", coalescedFlushes="
				+ s.coalescedFlushes() + " (threshold=" + s.threshold() + ")");
		Path dir = Files.createTempDirectory("zc-demo");
		try {
			long n = demoTransferTo(dir.resolve("a.txt"), dir.resolve("b.txt"));
			System.out.println("4) transferTo bytes = " + n);
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
		List<Integer> flushes = demoTokensIntoDirect(List.of("你好", "，", "Direct", "Buffer", "!"), 8);
		System.out.println("5) token→direct flush sizes = " + flushes);
	}

}

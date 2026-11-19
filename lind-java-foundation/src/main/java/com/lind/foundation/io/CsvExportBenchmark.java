package com.lind.foundation.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 模拟大 List 导出 CSV：对比「无缓冲 / 每行 flush / BufferedWriter」的耗时与下游 write 次数，并输出报告。
 * <p>
 * 说明见 {@code buffered-file.md} §8。
 */
public final class CsvExportBenchmark {

	public record OrderRow(long id, String userId, String amount, String status, long createdAtMillis) {

		String toCsvLine() {
			return id + "," + userId + "," + amount + "," + status + "," + createdAtMillis;
		}

	}

	public record Report(int rows, long generateMillis, long unbufferedMillis, long flushPerLineMillis,
			long bufferedMillis, long unbufferedBytes, long flushPerLineBytes, long bufferedBytes,
			int unbufferedDownstreamWrites, int flushPerLineDownstreamWrites, int bufferedDownstreamWrites,
			Path reportFile) {

		public String render() {
			double speedupVsUnbuf = unbufferedMillis == 0 ? 0 : (double) unbufferedMillis / Math.max(bufferedMillis, 1);
			double speedupVsFlush = flushPerLineMillis == 0 ? 0
					: (double) flushPerLineMillis / Math.max(bufferedMillis, 1);
			StringBuilder sb = new StringBuilder();
			sb.append("# CSV 导出性能对比报告\n\n");
			sb.append("## 环境与数据\n\n");
			sb.append("| 项 | 值 |\n|---|---|\n");
			sb.append("| 行数 (List size) | ").append(String.format(Locale.ROOT, "%,d", rows)).append(" |\n");
			sb.append("| 造数耗时 | ").append(generateMillis).append(" ms |\n");
			sb.append("| CSV 表头 | id,userId,amount,status,createdAt |\n");
			sb.append("| 字符集 | UTF-8 |\n\n");
			sb.append("## 三种写出方式\n\n");
			sb.append("| 方式 | 含义 |\n|---|---|\n");
			sb.append("| A 无 BufferedWriter | `OutputStreamWriter` 直接写文件（仍可能有编码器小缓冲） |\n");
			sb.append("| B 每行 flush | 在 A 基础上每写一行就 `flush()`（最差实践） |\n");
			sb.append("| C BufferedWriter | `BufferedWriter` → `OutputStreamWriter` → 文件（推荐） |\n\n");
			sb.append("## 结果\n\n");
			sb.append("| 方式 | 耗时 (ms) | 文件字节 | 触达下游 Writer 次数* |\n|---|---:|---:|---:|\n");
			sb.append("| A 无缓冲 | ").append(unbufferedMillis).append(" | ").append(unbufferedBytes).append(" | ")
					.append(unbufferedDownstreamWrites).append(" |\n");
			sb.append("| B 每行 flush | ").append(flushPerLineMillis).append(" | ").append(flushPerLineBytes)
					.append(" | ").append(flushPerLineDownstreamWrites).append(" |\n");
			sb.append("| C BufferedWriter | ").append(bufferedMillis).append(" | ").append(bufferedBytes).append(" | ")
					.append(bufferedDownstreamWrites).append(" |\n\n");
			sb.append("\\* 下游 write 次数用 `ProbeWriter` 统计（字符层触达 `OutputStreamWriter` 的次数）。\n\n");
			sb.append("## 结论\n\n");
			sb.append("- C 相对 A 约 **").append(String.format(Locale.ROOT, "%.2fx", speedupVsUnbuf)).append("**\n");
			sb.append("- C 相对 B 约 **").append(String.format(Locale.ROOT, "%.2fx", speedupVsFlush)).append("**\n");
			sb.append("- 文件大小应基本一致；若 B/A 远慢于 C，说明小写+频繁刷盘/刷流在拖垮导出。\n");
			sb.append("- 生产导出：用 `Files.newBufferedWriter` 或显式 `BufferedWriter`，**不要每行 flush**。\n");
			if (reportFile != null) {
				sb.append("\n报告文件: `").append(reportFile.toAbsolutePath()).append("`\n");
			}
			return sb.toString();
		}

	}

	private CsvExportBenchmark() {
	}

	/** 构造模拟订单 List（先造数，再单独计时导出，避免把造数算进 IO）。 */
	public static List<OrderRow> generateOrders(int rows) {
		if (rows <= 0) {
			throw new IllegalArgumentException("rows must be > 0");
		}
		List<OrderRow> list = new ArrayList<>(rows);
		long base = 1_700_000_000_000L;
		String[] statuses = { "CREATED", "PAID", "SHIPPED", "CLOSED", "REFUND" };
		for (int i = 0; i < rows; i++) {
			list.add(new OrderRow(10_000_000L + i, "U" + (i % 10_000),
					String.format(Locale.ROOT, "%.2f", (i % 999) + 0.5), statuses[i % statuses.length],
					base + i * 1000L));
		}
		return list;
	}

	public static void exportUnbuffered(Path file, List<OrderRow> rows) throws IOException {
		try (Writer out = new OutputStreamWriter(Files.newOutputStream(file), StandardCharsets.UTF_8)) {
			writeCsv(out, rows);
		}
	}

	public static void exportFlushPerLine(Path file, List<OrderRow> rows) throws IOException {
		try (Writer out = new OutputStreamWriter(Files.newOutputStream(file), StandardCharsets.UTF_8)) {
			out.write("id,userId,amount,status,createdAt");
			out.write('\n');
			out.flush();
			for (OrderRow row : rows) {
				out.write(row.toCsvLine());
				out.write('\n');
				out.flush(); // 反例：每行都刷，缓冲形同虚设
			}
		}
	}

	public static void exportBuffered(Path file, List<OrderRow> rows) throws IOException {
		try (BufferedWriter out = new BufferedWriter(
				new OutputStreamWriter(Files.newOutputStream(file), StandardCharsets.UTF_8))) {
			writeCsv(out, rows);
		}
	}

	private static void writeCsv(Writer out, List<OrderRow> rows) throws IOException {
		out.write("id,userId,amount,status,createdAt");
		out.write('\n');
		for (OrderRow row : rows) {
			out.write(row.toCsvLine());
			out.write('\n');
		}
	}

	/** 仅统计「字符层触达下游」次数（小样本即可，避免探针拖垮主计时）。 */
	public static int countDownstreamWrites(Path file, List<OrderRow> rows, boolean buffered, boolean flushPerLine)
			throws IOException {
		ProbeWriter probe = new ProbeWriter(
				new OutputStreamWriter(Files.newOutputStream(file), StandardCharsets.UTF_8));
		if (buffered) {
			try (BufferedWriter out = new BufferedWriter(probe)) {
				writeCsv(out, rows);
				out.flush();
			}
		}
		else {
			try (Writer out = probe) {
				if (flushPerLine) {
					out.write("id,userId,amount,status,createdAt");
					out.write('\n');
					out.flush();
					for (OrderRow row : rows) {
						out.write(row.toCsvLine());
						out.write('\n');
						out.flush();
					}
				}
				else {
					writeCsv(out, rows);
				}
			}
		}
		return probe.writeCalls();
	}

	/**
	 * 跑完整对比并写出 Markdown 报告。
	 * @param rows 建议演示用 100_000～500_000；单测用较小值
	 */
	public static Report run(Path workDir, int rows) throws IOException {
		Files.createDirectories(workDir);

		long t0 = System.nanoTime();
		List<OrderRow> data = generateOrders(rows);
		long generateMillis = (System.nanoTime() - t0) / 1_000_000L;

		Path a = workDir.resolve("export-unbuffered.csv");
		Path b = workDir.resolve("export-flush-per-line.csv");
		Path c = workDir.resolve("export-buffered.csv");

		// 轻量预热，减少 JIT 对首跑的干扰
		List<OrderRow> warm = data.subList(0, Math.min(2000, data.size()));
		exportUnbuffered(workDir.resolve("warm-a.csv"), warm);
		exportFlushPerLine(workDir.resolve("warm-b.csv"), warm);
		exportBuffered(workDir.resolve("warm-c.csv"), warm);

		long u0 = System.nanoTime();
		exportUnbuffered(a, data);
		long unbufferedMillis = (System.nanoTime() - u0) / 1_000_000L;

		long f0 = System.nanoTime();
		exportFlushPerLine(b, data);
		long flushPerLineMillis = (System.nanoTime() - f0) / 1_000_000L;

		long b0 = System.nanoTime();
		exportBuffered(c, data);
		long bufferedMillis = (System.nanoTime() - b0) / 1_000_000L;

		// 探针用较小子集，避免改变「大 List 导出」主结论的观感
		int probeRows = Math.min(5_000, rows);
		List<OrderRow> probeData = data.subList(0, probeRows);
		int writesA = countDownstreamWrites(workDir.resolve("probe-a.csv"), probeData, false, false);
		int writesB = countDownstreamWrites(workDir.resolve("probe-b.csv"), probeData, false, true);
		int writesC = countDownstreamWrites(workDir.resolve("probe-c.csv"), probeData, true, false);

		Path reportFile = workDir.resolve("csv-export-benchmark-report.md");
		Report report = new Report(rows, generateMillis, unbufferedMillis, flushPerLineMillis, bufferedMillis,
				Files.size(a), Files.size(b), Files.size(c), writesA, writesB, writesC, reportFile);
		Files.writeString(reportFile, report.render(), StandardCharsets.UTF_8);
		return report;
	}

	public static void main(String[] args) throws Exception {
		int rows = 100_000;
		if (args.length > 0) {
			rows = Integer.parseInt(args[0]);
		}
		Path dir = Files.createTempDirectory("csv-export-bench");
		Report report = run(dir, rows);
		System.out.println(report.render());
		System.out.println("工作目录: " + dir.toAbsolutePath());
	}

}

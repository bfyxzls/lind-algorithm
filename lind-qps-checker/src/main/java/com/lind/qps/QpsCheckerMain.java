package com.lind.qps;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * lind-qps-checker 控制台入口。
 */
public final class QpsCheckerMain {

	private QpsCheckerMain() {
	}

	public static void main(String[] args) {
		ensureUtf8Console();
		int code = run(args);
		if (code != 0) {
			System.exit(code);
		}
	}

	static void ensureUtf8Console() {
		System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
		System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
	}

	static int run(String[] args) {
		try {
			if (args.length == 0) {
				System.out.println(CliArgs.usage());
				return 1;
			}
			CliArgs cli = CliArgs.parse(args);
			if (cli.help()) {
				System.out.println(CliArgs.usage());
				return 0;
			}
			LoadConfig config = cli.toLoadConfig();
			if (cli.findMax()) {
				CapacityFinder.CapacityResult result = cli.toCapacityFinder(config).find();
				System.out.print(result.format());
				if (result.bestPassingReport() != null) {
					System.out.println();
					System.out.print(result.bestPassingReport().format());
				}
			}
			else {
				LoadReport report = new LoadRunner(config).run();
				System.out.print(report.format());
			}
			return 0;
		}
		catch (IllegalArgumentException ex) {
			System.err.println("参数错误: " + ex.getMessage());
			System.err.println();
			System.err.println(CliArgs.usage());
			return 2;
		}
		catch (Exception ex) {
			System.err.println("执行失败: " + ex.getMessage());
			ex.printStackTrace(System.err);
			return 3;
		}
	}

}

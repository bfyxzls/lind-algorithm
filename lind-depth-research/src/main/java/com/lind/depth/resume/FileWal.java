package com.lind.depth.resume;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Optional;

/**
 * <b>预写日志（WAL）</b>：记录某个会话「已经生成到哪个 event id」。
 *
 * <p>
 * 对应文档：「先记状态，再对外承诺」——类比数据库 redo log。 崩溃 / 断连后，服务端读 WAL 就知道进度，而不是从 0 重来。
 *
 * <p>
 * 教学实现：每个会话一个文本文件，写入最新 {@code sessionId=...&lastEventId=...}。
 */
public final class FileWal {

	private final Path dir;

	public FileWal(Path dir) throws IOException {
		this.dir = Objects.requireNonNull(dir, "dir");
		Files.createDirectories(dir);
	}

	/**
	 * 在「写出 Stream / 推给客户端」之前调用，持久化最新进度。
	 */
	public void appendProgress(String sessionId, long lastEventId) throws IOException {
		Objects.requireNonNull(sessionId, "sessionId");
		if (lastEventId < 0) {
			throw new IllegalArgumentException("lastEventId 不能为负");
		}
		Path file = fileOf(sessionId);
		String line = "sessionId=" + sessionId + "&lastEventId=" + lastEventId + "\n";
		try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
			w.write(line);
			w.flush();
		}
	}

	/** 读取会话最后一次写入 WAL 的 event id。 */
	public Optional<Long> lastEventId(String sessionId) throws IOException {
		Path file = fileOf(sessionId);
		if (!Files.exists(file)) {
			return Optional.empty();
		}
		String line = Files.readString(file, StandardCharsets.UTF_8).trim();
		int idx = line.indexOf("lastEventId=");
		if (idx < 0) {
			return Optional.empty();
		}
		return Optional.of(Long.parseLong(line.substring(idx + "lastEventId=".length()).trim()));
	}

	private Path fileOf(String sessionId) {
		String safe = sessionId.replaceAll("[^a-zA-Z0-9_-]", "_");
		return dir.resolve(safe + ".wal");
	}

}

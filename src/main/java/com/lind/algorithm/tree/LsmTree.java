package com.lind.algorithm.tree;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 磁盘版 LSM 树（Log-Structured Merge-Tree），面向真实落地场景。
 * <p>
 * 数据目录布局： <pre>
 *   dataDir/
 *     MANIFEST      # SSTable 文件名列表，首行为最新
 *     wal.log       # Write-Ahead Log
 *     sst-N.sst     # 不可变有序表文件
 * </pre> 写路径：先追加 WAL，再写入 MemTable；MemTable 达到阈值后 Flush 为 SSTable 并截断 WAL。 读路径：MemTable →
 * 新到旧 SSTable。打开目录时加载 Manifest 并回放 WAL，支持进程重启恢复。
 * </p>
 */
public class LsmTree implements AutoCloseable {

	public static final int DEFAULT_MEMTABLE_THRESHOLD = 64;

	private static final String MANIFEST_NAME = "MANIFEST";

	private static final String WAL_NAME = "wal.log";

	private static final String SST_PREFIX = "sst-";

	private static final String SST_SUFFIX = ".sst";

	private final Path dataDir;

	private final int memTableThreshold;

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	private final AtomicLong nextSstId = new AtomicLong(1);

	private TreeMap<String, LsmEntry> memTable = new TreeMap<>();

	/** 逻辑 SSTable 文件名，下标 0 为最新。 */
	private final List<String> sstFiles = new ArrayList<>();

	private LsmWal wal;

	private boolean closed;

	public LsmTree(Path dataDir) throws IOException {
		this(dataDir, DEFAULT_MEMTABLE_THRESHOLD);
	}

	public LsmTree(Path dataDir, int memTableThreshold) throws IOException {
		Objects.requireNonNull(dataDir, "dataDir");
		if (memTableThreshold <= 0) {
			throw new IllegalArgumentException("memTableThreshold must be > 0");
		}
		this.dataDir = dataDir.toAbsolutePath().normalize();
		this.memTableThreshold = memTableThreshold;
		Files.createDirectories(this.dataDir);
		recover();
		this.wal = LsmWal.open(walPath());
	}

	public Path getDataDir() {
		return dataDir;
	}

	public int getMemTableThreshold() {
		return memTableThreshold;
	}

	/**
	 * 写入或覆盖。值不可为 null。
	 */
	public void put(String key, String value) throws IOException {
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(value, "value");
		ensureOpen();
		lock.writeLock().lock();
		try {
			LsmEntry entry = LsmEntry.ofValue(value);
			wal.append(key, entry);
			memTable.put(key, entry);
			flushIfNeeded();
		}
		finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * 删除：写墓碑到 WAL + MemTable。
	 */
	public void delete(String key) throws IOException {
		Objects.requireNonNull(key, "key");
		ensureOpen();
		lock.writeLock().lock();
		try {
			LsmEntry entry = LsmEntry.ofTombstone();
			wal.append(key, entry);
			memTable.put(key, entry);
			flushIfNeeded();
		}
		finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * 查找：MemTable → 磁盘 SSTable（新到旧）。
	 */
	public Optional<String> get(String key) throws IOException {
		Objects.requireNonNull(key, "key");
		ensureOpen();
		lock.readLock().lock();
		try {
			LsmEntry fromMem = memTable.get(key);
			if (fromMem != null) {
				return fromMem.asOptional();
			}
			for (String name : sstFiles) {
				Optional<LsmEntry> found = LsmSsTable.get(dataDir.resolve(name), key);
				if (found.isPresent()) {
					return found.get().asOptional();
				}
			}
			return Optional.empty();
		}
		finally {
			lock.readLock().unlock();
		}
	}

	public boolean containsKey(String key) throws IOException {
		return get(key).isPresent();
	}

	/**
	 * 将 MemTable Flush 为磁盘 SSTable，并截断 WAL。
	 */
	public void flush() throws IOException {
		ensureOpen();
		lock.writeLock().lock();
		try {
			doFlush();
		}
		finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * 合并所有 SSTable 为一个新文件，删除旧文件与墓碑。
	 * @return compact 后 SSTable 数量
	 */
	public int compact() throws IOException {
		ensureOpen();
		lock.writeLock().lock();
		try {
			if (sstFiles.isEmpty()) {
				return 0;
			}
			TreeMap<String, LsmEntry> merged = new TreeMap<>();
			// 旧 → 新覆盖
			for (int i = sstFiles.size() - 1; i >= 0; i--) {
				merged.putAll(LsmSsTable.load(dataDir.resolve(sstFiles.get(i))));
			}
			pruneTombstones(merged);

			List<String> oldFiles = new ArrayList<>(sstFiles);
			sstFiles.clear();
			if (!merged.isEmpty()) {
				String name = nextSstFileName();
				LsmSsTable.write(dataDir.resolve(name), merged);
				sstFiles.add(name);
			}
			writeManifest();
			for (String old : oldFiles) {
				Files.deleteIfExists(dataDir.resolve(old));
			}
			return sstFiles.size();
		}
		finally {
			lock.writeLock().unlock();
		}
	}

	public int memTableSize() {
		lock.readLock().lock();
		try {
			return memTable.size();
		}
		finally {
			lock.readLock().unlock();
		}
	}

	public int ssTableCount() {
		lock.readLock().lock();
		try {
			return sstFiles.size();
		}
		finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * 有效键数量（扫描 MemTable + 全部 SSTable，成本较高）。
	 */
	public int size() throws IOException {
		lock.readLock().lock();
		try {
			TreeMap<String, LsmEntry> view = new TreeMap<>();
			for (int i = sstFiles.size() - 1; i >= 0; i--) {
				view.putAll(LsmSsTable.load(dataDir.resolve(sstFiles.get(i))));
			}
			view.putAll(memTable);
			int count = 0;
			for (LsmEntry e : view.values()) {
				if (!e.isDeleted()) {
					count++;
				}
			}
			return count;
		}
		finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * 合并视图快照（调试用）。
	 */
	public Map<String, String> snapshot() throws IOException {
		lock.readLock().lock();
		try {
			TreeMap<String, LsmEntry> view = new TreeMap<>();
			for (int i = sstFiles.size() - 1; i >= 0; i--) {
				view.putAll(LsmSsTable.load(dataDir.resolve(sstFiles.get(i))));
			}
			view.putAll(memTable);
			TreeMap<String, String> result = new TreeMap<>();
			for (Map.Entry<String, LsmEntry> e : view.entrySet()) {
				if (!e.getValue().isDeleted()) {
					result.put(e.getKey(), e.getValue().value());
				}
			}
			return Collections.unmodifiableMap(result);
		}
		finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * 关闭前 Flush MemTable，保证数据落盘。
	 */
	@Override
	public void close() throws IOException {
		lock.writeLock().lock();
		try {
			if (closed) {
				return;
			}
			doFlush();
			if (wal != null) {
				wal.close();
				wal = null;
			}
			closed = true;
		}
		finally {
			lock.writeLock().unlock();
		}
	}

	private void recover() throws IOException {
		loadManifestOrScan();
		Path wal = walPath();
		if (Files.exists(wal) && Files.size(wal) > 0) {
			memTable.putAll(LsmWal.replay(wal));
		}
	}

	private void loadManifestOrScan() throws IOException {
		Path manifest = manifestPath();
		sstFiles.clear();
		long maxId = 0;
		if (Files.exists(manifest)) {
			List<String> lines = Files.readAllLines(manifest, StandardCharsets.UTF_8);
			for (String line : lines) {
				String name = line.trim();
				if (!name.isEmpty() && Files.exists(dataDir.resolve(name))) {
					sstFiles.add(name);
					maxId = Math.max(maxId, parseSstId(name));
				}
			}
		}
		else {
			try (Stream<Path> stream = Files.list(dataDir)) {
				List<String> found = stream.map(p -> p.getFileName().toString())
						.filter(n -> n.startsWith(SST_PREFIX) && n.endsWith(SST_SUFFIX))
						.sorted((a, b) -> Long.compare(parseSstId(b), parseSstId(a))).collect(Collectors.toList());
				sstFiles.addAll(found);
				for (String n : found) {
					maxId = Math.max(maxId, parseSstId(n));
				}
			}
		}
		nextSstId.set(maxId + 1);
	}

	private void flushIfNeeded() throws IOException {
		if (memTable.size() >= memTableThreshold) {
			doFlush();
		}
	}

	private void doFlush() throws IOException {
		if (memTable.isEmpty()) {
			return;
		}
		String name = nextSstFileName();
		LsmSsTable.write(dataDir.resolve(name), memTable);
		sstFiles.add(0, name);
		writeManifest();
		memTable = new TreeMap<>();
		wal.truncate();
	}

	private void writeManifest() throws IOException {
		Path tmp = dataDir.resolve(MANIFEST_NAME + ".tmp");
		Files.write(tmp, sstFiles, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
		atomicReplace(tmp, manifestPath());
	}

	private static void atomicReplace(Path tmp, Path target) throws IOException {
		try {
			Files.move(tmp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
					java.nio.file.StandardCopyOption.ATOMIC_MOVE);
		}
		catch (java.nio.file.AtomicMoveNotSupportedException e) {
			Files.move(tmp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private String nextSstFileName() {
		return SST_PREFIX + nextSstId.getAndIncrement() + SST_SUFFIX;
	}

	private static long parseSstId(String name) {
		try {
			String id = name.substring(SST_PREFIX.length(), name.length() - SST_SUFFIX.length());
			return Long.parseLong(id);
		}
		catch (Exception e) {
			return 0L;
		}
	}

	private Path manifestPath() {
		return dataDir.resolve(MANIFEST_NAME);
	}

	private Path walPath() {
		return dataDir.resolve(WAL_NAME);
	}

	private void ensureOpen() {
		if (closed) {
			throw new IllegalStateException("LsmTree is closed");
		}
	}

	private static void pruneTombstones(TreeMap<String, LsmEntry> table) {
		Iterator<Map.Entry<String, LsmEntry>> it = table.entrySet().iterator();
		while (it.hasNext()) {
			if (it.next().getValue().isDeleted()) {
				it.remove();
			}
		}
	}

	/**
	 * LSM 条目。
	 */
	static final class LsmEntry {

		private final String value;

		private final boolean deleted;

		private LsmEntry(String value, boolean deleted) {
			this.value = value;
			this.deleted = deleted;
		}

		static LsmEntry ofValue(String value) {
			return new LsmEntry(value, false);
		}

		static LsmEntry ofTombstone() {
			return new LsmEntry(null, true);
		}

		String value() {
			return value;
		}

		boolean isDeleted() {
			return deleted;
		}

		Optional<String> asOptional() {
			return deleted ? Optional.empty() : Optional.ofNullable(value);
		}

	}

	/**
	 * SSTable 文件编解码。
	 */
	static final class LsmSsTable {

		private static final long MAGIC = 0x4C534D5353543031L; // LSMSS T01

		private LsmSsTable() {
		}

		static void write(Path file, TreeMap<String, LsmEntry> data) throws IOException {
			Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
			try (FileChannel ch = FileChannel.open(tmp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
					StandardOpenOption.WRITE)) {
				ByteBuffer header = ByteBuffer.allocate(12);
				header.putLong(MAGIC);
				header.putInt(data.size());
				header.flip();
				ch.write(header);
				for (Map.Entry<String, LsmEntry> e : data.entrySet()) {
					writeRecord(ch, e.getKey(), e.getValue());
				}
				ch.force(true);
			}
			try {
				Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
						java.nio.file.StandardCopyOption.ATOMIC_MOVE);
			}
			catch (java.nio.file.AtomicMoveNotSupportedException e) {
				Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			}
		}

		static TreeMap<String, LsmEntry> load(Path file) throws IOException {
			TreeMap<String, LsmEntry> map = new TreeMap<>();
			try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
				ByteBuffer header = ByteBuffer.allocate(12);
				readFully(ch, header);
				header.flip();
				long magic = header.getLong();
				if (magic != MAGIC) {
					throw new IOException("Invalid SST magic: " + file);
				}
				int count = header.getInt();
				for (int i = 0; i < count; i++) {
					Map.Entry<String, LsmEntry> rec = readRecord(ch);
					map.put(rec.getKey(), rec.getValue());
				}
			}
			return map;
		}

		static Optional<LsmEntry> get(Path file, String key) throws IOException {
			// 简化实现：加载文件后查找；后续可加稀疏索引 / BloomFilter 优化
			return Optional.ofNullable(load(file).get(key));
		}

		static void writeRecord(FileChannel ch, String key, LsmEntry entry) throws IOException {
			byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
			byte[] valBytes = entry.isDeleted() || entry.value() == null ? new byte[0]
					: entry.value().getBytes(StandardCharsets.UTF_8);
			ByteBuffer buf = ByteBuffer.allocate(1 + 4 + keyBytes.length + 4 + valBytes.length);
			buf.put((byte) (entry.isDeleted() ? 1 : 0));
			buf.putInt(keyBytes.length);
			buf.put(keyBytes);
			buf.putInt(valBytes.length);
			buf.put(valBytes);
			buf.flip();
			ch.write(buf);
		}

		static Map.Entry<String, LsmEntry> readRecord(FileChannel ch) throws IOException {
			ByteBuffer flagBuf = ByteBuffer.allocate(1);
			readFully(ch, flagBuf);
			flagBuf.flip();
			boolean deleted = flagBuf.get() == 1;

			ByteBuffer keyLenBuf = ByteBuffer.allocate(4);
			readFully(ch, keyLenBuf);
			keyLenBuf.flip();
			int keyLen = keyLenBuf.getInt();
			byte[] keyBytes = new byte[keyLen];
			readFully(ch, ByteBuffer.wrap(keyBytes));

			ByteBuffer valLenBuf = ByteBuffer.allocate(4);
			readFully(ch, valLenBuf);
			valLenBuf.flip();
			int valLen = valLenBuf.getInt();
			byte[] valBytes = new byte[valLen];
			if (valLen > 0) {
				readFully(ch, ByteBuffer.wrap(valBytes));
			}

			String key = new String(keyBytes, StandardCharsets.UTF_8);
			LsmEntry entry = deleted ? LsmEntry.ofTombstone()
					: LsmEntry.ofValue(new String(valBytes, StandardCharsets.UTF_8));
			return Map.entry(key, entry);
		}

		static void readFully(FileChannel ch, ByteBuffer buf) throws IOException {
			while (buf.hasRemaining()) {
				int n = ch.read(buf);
				if (n < 0) {
					throw new IOException("Unexpected EOF in SST/WAL");
				}
			}
		}

	}

	/**
	 * Write-Ahead Log。
	 */
	static final class LsmWal implements AutoCloseable {

		private final Path path;

		private FileChannel channel;

		private LsmWal(Path path, FileChannel channel) {
			this.path = path;
			this.channel = channel;
		}

		static LsmWal open(Path path) throws IOException {
			FileChannel ch = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
					StandardOpenOption.APPEND);
			return new LsmWal(path, ch);
		}

		static TreeMap<String, LsmEntry> replay(Path path) throws IOException {
			TreeMap<String, LsmEntry> map = new TreeMap<>();
			try (FileChannel ch = FileChannel.open(path, StandardOpenOption.READ)) {
				while (ch.position() < ch.size()) {
					Map.Entry<String, LsmEntry> rec = LsmSsTable.readRecord(ch);
					map.put(rec.getKey(), rec.getValue());
				}
			}
			return map;
		}

		synchronized void append(String key, LsmEntry entry) throws IOException {
			LsmSsTable.writeRecord(channel, key, entry);
			channel.force(true);
		}

		synchronized void truncate() throws IOException {
			channel.truncate(0);
			channel.force(true);
		}

		@Override
		public synchronized void close() throws IOException {
			if (channel != null) {
				channel.force(true);
				channel.close();
				channel = null;
			}
		}

	}

}

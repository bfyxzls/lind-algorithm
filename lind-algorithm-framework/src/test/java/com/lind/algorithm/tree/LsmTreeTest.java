package com.lind.algorithm.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 磁盘版 {@link LsmTree} 单元测试。
 */
public class LsmTreeTest {

	@TempDir
	Path tempDir;

	@Test
	void putGetAndOverwrite() throws IOException {
		try (LsmTree tree = new LsmTree(tempDir.resolve("db1"), 100)) {
			tree.put("a", "1");
			tree.put("b", "2");
			tree.put("a", "11");

			assertEquals(Optional.of("11"), tree.get("a"));
			assertEquals(Optional.of("2"), tree.get("b"));
			assertEquals(Optional.empty(), tree.get("c"));
			assertEquals(2, tree.size());
		}
	}

	@Test
	void deleteWritesTombstone() throws IOException {
		try (LsmTree tree = new LsmTree(tempDir.resolve("db2"), 100)) {
			tree.put("k", "1");
			tree.delete("k");
			assertFalse(tree.containsKey("k"));
			assertEquals(Optional.empty(), tree.get("k"));
			assertEquals(0, tree.size());
		}
	}

	@Test
	void flushCreatesSstFileOnDisk() throws IOException {
		Path dir = tempDir.resolve("db3");
		try (LsmTree tree = new LsmTree(dir, 3)) {
			tree.put("1", "a");
			tree.put("2", "b");
			assertEquals(0, tree.ssTableCount());
			assertEquals(2, tree.memTableSize());

			tree.put("3", "c");
			assertEquals(1, tree.ssTableCount());
			assertEquals(0, tree.memTableSize());
			assertTrue(countSstFiles(dir) >= 1);

			assertEquals(Optional.of("a"), tree.get("1"));
			assertEquals(Optional.of("c"), tree.get("3"));
		}
	}

	@Test
	void newerSsTableOverridesOlder() throws IOException {
		try (LsmTree tree = new LsmTree(tempDir.resolve("db4"), 2)) {
			tree.put("k", "v1");
			tree.put("x", "1");
			tree.put("k", "v2");
			tree.put("y", "2");

			assertEquals(2, tree.ssTableCount());
			assertEquals(Optional.of("v2"), tree.get("k"));
		}
	}

	@Test
	void compactMergesAndDropsTombstones() throws IOException {
		Path dir = tempDir.resolve("db5");
		try (LsmTree tree = new LsmTree(dir, 2)) {
			tree.put("a", "1");
			tree.put("b", "2");
			tree.delete("a");
			tree.put("c", "3");
			assertTrue(tree.ssTableCount() >= 2);

			int after = tree.compact();
			assertEquals(1, after);
			assertFalse(tree.containsKey("a"));
			assertEquals(Optional.of("2"), tree.get("b"));
			assertEquals(Optional.of("3"), tree.get("c"));
			assertEquals(2, tree.size());
			assertEquals(1, countSstFiles(dir));
		}
	}

	@Test
	void recoverAfterReopen() throws IOException {
		Path dir = tempDir.resolve("db6");
		try (LsmTree tree = new LsmTree(dir, 100)) {
			tree.put("hello", "world");
			tree.put("foo", "bar");
			tree.flush();
			tree.put("only-wal", "yes");
		}

		try (LsmTree reopened = new LsmTree(dir, 100)) {
			assertEquals(Optional.of("world"), reopened.get("hello"));
			assertEquals(Optional.of("bar"), reopened.get("foo"));
			assertEquals(Optional.of("yes"), reopened.get("only-wal"));
			assertEquals(3, reopened.size());
		}
	}

	@Test
	void recoverDeleteFromWal() throws IOException {
		Path dir = tempDir.resolve("db7");
		try (LsmTree tree = new LsmTree(dir, 100)) {
			tree.put("k", "v");
			tree.flush();
			tree.delete("k");
		}
		try (LsmTree reopened = new LsmTree(dir, 100)) {
			assertEquals(Optional.empty(), reopened.get("k"));
		}
	}

	@Test
	void rejectInvalidArgs() throws IOException {
		assertThrows(IllegalArgumentException.class, () -> new LsmTree(tempDir.resolve("bad"), 0));
		try (LsmTree tree = new LsmTree(tempDir.resolve("db8"))) {
			assertThrows(NullPointerException.class, () -> tree.put(null, "v"));
			assertThrows(NullPointerException.class, () -> tree.put("k", null));
			assertThrows(NullPointerException.class, () -> tree.get(null));
		}
	}

	private static long countSstFiles(Path dir) throws IOException {
		try (Stream<Path> stream = Files.list(dir)) {
			return stream.filter(p -> p.getFileName().toString().endsWith(".sst")).count();
		}
	}

}

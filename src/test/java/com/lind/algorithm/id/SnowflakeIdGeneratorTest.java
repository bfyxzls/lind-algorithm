package com.lind.algorithm.id;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@link SnowflakeIdGenerator} 单元测试。
 */
public class SnowflakeIdGeneratorTest {

	@Test
	void generatesUniqueIncreasingIds() {
		SnowflakeIdGenerator gen = new SnowflakeIdGenerator(1);
		Set<Long> ids = new HashSet<>();
		long prev = -1;
		for (int i = 0; i < 2000; i++) {
			long id = gen.nextId();
			assertTrue(id > prev);
			assertTrue(ids.add(id));
			prev = id;
		}
		assertEquals(1, gen.workerId());
	}

	@Test
	void rejectInvalidWorkerId() {
		assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(-1));
		assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(1024));
	}

}

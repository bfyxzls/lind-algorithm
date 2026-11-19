package com.lind.foundation.concurrent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LostUpdateDemoTest {

	@Test
	void unsafe_loses_updates_under_contention() throws Exception {
		var r = LostUpdateDemos.runUnsafe(8, 50_000);
		assertThat(r.actual()).isLessThan(r.expected());
	}

	@Test
	void synchronized_keeps_exact_count() throws Exception {
		var r = LostUpdateDemos.runSynchronized(8, 50_000);
		assertThat(r.actual()).isEqualTo(r.expected());
	}

	@Test
	void reentrantLock_keeps_exact_count() throws Exception {
		var r = LostUpdateDemos.runReentrantLock(8, 50_000);
		assertThat(r.actual()).isEqualTo(r.expected());
	}

}

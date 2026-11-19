package com.lind.foundation.concurrent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BoundedBufferTest {

	@Test
	void put_take_fifo_within_capacity() throws Exception {
		BoundedBuffer<String> buf = new BoundedBuffer<>(2);
		buf.put("a");
		buf.put("b");
		assertThat(buf.size()).isEqualTo(2);
		assertThat(buf.take()).isEqualTo("a");
		assertThat(buf.take()).isEqualTo("b");
		assertThat(buf.size()).isEqualTo(0);
	}

	@Test
	void multi_producer_consumer_conserves_items() throws Exception {
		var r = BoundedBufferDemos.pump(8, 4, 4, 2_000);
		assertThat(r.conserved()).isTrue();
		assertThat(r.produced()).isEqualTo(8_000L);
	}

	@Test
	void blocking_put_waits_until_space() throws Exception {
		BoundedBuffer<Integer> buf = new BoundedBuffer<>(1);
		buf.put(1);
		Thread producer = new Thread(() -> {
			try {
				buf.put(2);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		producer.start();
		Thread.sleep(80);
		assertThat(producer.isAlive()).isTrue();
		assertThat(buf.take()).isEqualTo(1);
		producer.join(2_000);
		assertThat(producer.isAlive()).isFalse();
		assertThat(buf.take()).isEqualTo(2);
	}

	@Test
	void sequential_round_trip_values() throws Exception {
		BoundedBuffer<Integer> buf = new BoundedBuffer<>(3);
		List<Integer> out = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			buf.put(i);
			if (i >= 2) {
				out.add(buf.take());
			}
		}
		while (buf.size() > 0) {
			out.add(buf.take());
		}
		assertThat(out).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
	}

}

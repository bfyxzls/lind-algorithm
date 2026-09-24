package com.lind.algorithm.heap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

/**
 * {@link BinaryHeap} 单元测试。
 */
public class BinaryHeapTest {

	@Test
	void naturalOrderPollsAscending() {
		BinaryHeap<Integer> heap = BinaryHeap.naturalOrder();
		heap.offer(5);
		heap.offer(1);
		heap.offer(3);
		assertEquals(1, heap.peek());
		assertEquals(1, heap.poll());
		assertEquals(3, heap.poll());
		assertEquals(5, heap.poll());
		assertTrue(heap.isEmpty());
	}

	@Test
	void maxHeapWithReverseOrder() {
		BinaryHeap<Integer> heap = new BinaryHeap<>(Comparator.reverseOrder());
		heap.offer(1);
		heap.offer(9);
		heap.offer(4);
		assertEquals(9, heap.poll());
		assertEquals(4, heap.poll());
		assertEquals(1, heap.poll());
	}

	@Test
	void emptyThrows() {
		BinaryHeap<Integer> heap = BinaryHeap.naturalOrder();
		assertThrows(NoSuchElementException.class, heap::poll);
		assertThrows(NoSuchElementException.class, heap::peek);
	}

}

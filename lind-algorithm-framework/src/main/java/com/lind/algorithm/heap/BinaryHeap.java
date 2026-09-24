package com.lind.algorithm.heap;

import java.util.Arrays;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * 二叉堆优先队列。堆序由 {@link Comparator} 决定（自然序即最小堆）。
 *
 * @param <T> 元素类型
 */
public final class BinaryHeap<T> {

	private static final int DEFAULT_CAPACITY = 16;

	private final Comparator<? super T> comparator;

	private Object[] elements;

	private int size;

	public BinaryHeap(Comparator<? super T> comparator) {
		this.comparator = Objects.requireNonNull(comparator, "comparator");
		this.elements = new Object[DEFAULT_CAPACITY];
	}

	@SuppressWarnings("unchecked")
	public static <E extends Comparable<? super E>> BinaryHeap<E> naturalOrder() {
		return new BinaryHeap<>((Comparator<? super E>) Comparator.naturalOrder());
	}

	public void offer(T value) {
		Objects.requireNonNull(value, "value");
		ensureCapacity(size + 1);
		elements[size] = value;
		siftUp(size);
		size++;
	}

	@SuppressWarnings("unchecked")
	public T poll() {
		if (size == 0) {
			throw new NoSuchElementException("heap is empty");
		}
		T root = (T) elements[0];
		size--;
		elements[0] = elements[size];
		elements[size] = null;
		if (size > 0) {
			siftDown(0);
		}
		return root;
	}

	@SuppressWarnings("unchecked")
	public T peek() {
		if (size == 0) {
			throw new NoSuchElementException("heap is empty");
		}
		return (T) elements[0];
	}

	public int size() {
		return size;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	private void ensureCapacity(int min) {
		if (min <= elements.length) {
			return;
		}
		elements = Arrays.copyOf(elements, elements.length * 2);
	}

	@SuppressWarnings("unchecked")
	private void siftUp(int index) {
		T value = (T) elements[index];
		while (index > 0) {
			int parent = (index - 1) >>> 1;
			T parentValue = (T) elements[parent];
			if (comparator.compare(value, parentValue) >= 0) {
				break;
			}
			elements[index] = parentValue;
			index = parent;
		}
		elements[index] = value;
	}

	@SuppressWarnings("unchecked")
	private void siftDown(int index) {
		T value = (T) elements[index];
		int half = size >>> 1;
		while (index < half) {
			int child = (index << 1) + 1;
			int right = child + 1;
			T childValue = (T) elements[child];
			if (right < size && comparator.compare(childValue, (T) elements[right]) > 0) {
				child = right;
				childValue = (T) elements[right];
			}
			if (comparator.compare(value, childValue) <= 0) {
				break;
			}
			elements[index] = childValue;
			index = child;
		}
		elements[index] = value;
	}

}

package com.lind.algorithm.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 简化 R 树（二维）：用最小外接矩形（MBR）对空间对象建索引，支持矩形相交查询。
 * <p>
 * 节点溢出时采用线性分裂；适合 GIS 点选、地图标注碰撞、二维范围检索等教学与轻量场景。
 * </p>
 *
 * @param <T> 空间对象载荷
 */
public class RTree<T> {

	private final int maxEntries;

	private Node<T> root;

	private int size;

	public RTree() {
		this(4);
	}

	public RTree(int maxEntries) {
		if (maxEntries < 2) {
			throw new IllegalArgumentException("maxEntries must be >= 2");
		}
		this.maxEntries = maxEntries;
		this.root = Node.leaf();
	}

	public int size() {
		return size;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * 插入带矩形边界的对象。
	 */
	public void insert(Rectangle mbr, T value) {
		Objects.requireNonNull(mbr, "mbr");
		Objects.requireNonNull(value, "value");
		Entry<T> entry = new Entry<>(mbr, value, null);
		Node<T> split = root.insert(entry, maxEntries);
		size++;
		if (split != null) {
			Node<T> newRoot = Node.internal();
			newRoot.children.add(new Entry<>(root.mbr(), null, root));
			newRoot.children.add(new Entry<>(split.mbr(), null, split));
			root = newRoot;
		}
	}

	/**
	 * 查询与给定矩形相交的全部对象。
	 */
	public List<T> search(Rectangle query) {
		Objects.requireNonNull(query, "query");
		List<T> result = new ArrayList<>();
		root.search(query, result);
		return Collections.unmodifiableList(result);
	}

	/**
	 * 轴对齐矩形。
	 */
	public record Rectangle(double minX, double minY, double maxX, double maxY) {

		public Rectangle {
			if (minX > maxX || minY > maxY) {
				throw new IllegalArgumentException("invalid rectangle");
			}
		}

		public static Rectangle ofPoint(double x, double y) {
			return new Rectangle(x, y, x, y);
		}

		public boolean intersects(Rectangle other) {
			return minX <= other.maxX && maxX >= other.minX && minY <= other.maxY && maxY >= other.minY;
		}

		public Rectangle union(Rectangle other) {
			return new Rectangle(Math.min(minX, other.minX), Math.min(minY, other.minY), Math.max(maxX, other.maxX),
					Math.max(maxY, other.maxY));
		}

		public double area() {
			return (maxX - minX) * (maxY - minY);
		}

		public double enlargement(Rectangle other) {
			return union(other).area() - area();
		}

	}

	private static final class Entry<T> {

		private Rectangle mbr;

		private final T value;

		private final Node<T> child;

		private Entry(Rectangle mbr, T value, Node<T> child) {
			this.mbr = mbr;
			this.value = value;
			this.child = child;
		}

	}

	private static final class Node<T> {

		private final boolean leaf;

		private final List<Entry<T>> children = new ArrayList<>();

		private Node(boolean leaf) {
			this.leaf = leaf;
		}

		static <T> Node<T> leaf() {
			return new Node<>(true);
		}

		static <T> Node<T> internal() {
			return new Node<>(false);
		}

		Rectangle mbr() {
			Rectangle box = null;
			for (Entry<T> e : children) {
				box = box == null ? e.mbr : box.union(e.mbr);
			}
			return box;
		}

		Node<T> insert(Entry<T> entry, int maxEntries) {
			if (leaf) {
				children.add(entry);
				if (children.size() <= maxEntries) {
					return null;
				}
				return splitLinear();
			}
			Entry<T> best = chooseSubtree(entry.mbr);
			Node<T> splitChild = best.child.insert(entry, maxEntries);
			best.mbr = best.child.mbr();
			if (splitChild == null) {
				return null;
			}
			children.add(new Entry<>(splitChild.mbr(), null, splitChild));
			if (children.size() <= maxEntries) {
				return null;
			}
			return splitLinear();
		}

		private Entry<T> chooseSubtree(Rectangle mbr) {
			Entry<T> best = children.get(0);
			double bestEnlargement = best.mbr.enlargement(mbr);
			double bestArea = best.mbr.area();
			for (int i = 1; i < children.size(); i++) {
				Entry<T> candidate = children.get(i);
				double enlargement = candidate.mbr.enlargement(mbr);
				double area = candidate.mbr.area();
				if (enlargement < bestEnlargement || (enlargement == bestEnlargement && area < bestArea)) {
					best = candidate;
					bestEnlargement = enlargement;
					bestArea = area;
				}
			}
			return best;
		}

		private Node<T> splitLinear() {
			int seed1 = 0;
			int seed2 = 1;
			double worst = -1;
			for (int i = 0; i < children.size(); i++) {
				for (int j = i + 1; j < children.size(); j++) {
					double waste = children.get(i).mbr.union(children.get(j).mbr).area() - children.get(i).mbr.area()
							- children.get(j).mbr.area();
					if (waste > worst) {
						worst = waste;
						seed1 = i;
						seed2 = j;
					}
				}
			}
			Node<T> right = new Node<>(leaf);
			Entry<T> e1 = children.get(seed1);
			Entry<T> e2 = children.get(seed2);
			List<Entry<T>> remaining = new ArrayList<>();
			for (int i = 0; i < children.size(); i++) {
				if (i != seed1 && i != seed2) {
					remaining.add(children.get(i));
				}
			}
			children.clear();
			children.add(e1);
			right.children.add(e2);
			for (Entry<T> e : remaining) {
				double leftCost = mbr().enlargement(e.mbr);
				double rightCost = right.mbr().enlargement(e.mbr);
				if (leftCost < rightCost || (leftCost == rightCost && children.size() <= right.children.size())) {
					children.add(e);
				}
				else {
					right.children.add(e);
				}
			}
			return right;
		}

		void search(Rectangle query, List<T> out) {
			for (Entry<T> e : children) {
				if (!e.mbr.intersects(query)) {
					continue;
				}
				if (leaf) {
					out.add(e.value);
				}
				else {
					e.child.search(query, out);
				}
			}
		}

	}

}

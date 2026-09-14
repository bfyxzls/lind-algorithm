package com.lind.algorithm.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * {@link Tree} 业务层级树单元测试。
 */
public class TreeTest {

	@Test
	void fillChildrenBuildsHierarchy() {
		List<NormalTree> all = sampleData();
		List<NormalTree> roots = all.stream().filter(o -> "0".equals(o.getParentId())).collect(Collectors.toList());

		new NormalTree().fillChildren(roots, all);

		assertEquals(1, roots.size());
		assertEquals("1", roots.get(0).getId());
		assertEquals(2, roots.get(0).getSons().size());
	}

	@Test
	void convertBuildsDeepCopyTree() {
		List<NormalTree> all = sampleData();
		List<NormalTree> trees = all.stream().filter(item -> "0".equals(item.getParentId()))
				.map(item -> item.convert(all)).collect(Collectors.toList());

		assertEquals(1, trees.size());
		NormalTree root = trees.get(0);
		assertEquals("1", root.getId());
		assertNotNull(root.getSons());
		assertEquals(2, root.getSons().size());
	}

	private static List<NormalTree> sampleData() {
		List<NormalTree> list = new ArrayList<>();
		list.add(node("1", "0", "1", 0));
		list.add(node("2", "1", "2", 0));
		list.add(node("3", "1", "3", 0));
		list.add(node("4", "2", "4", 0));
		list.add(node("5", "2", "5", 0));
		return list;
	}

	private static NormalTree node(String id, String parentId, String title, Integer type) {
		NormalTree tree = new NormalTree();
		tree.setId(id);
		tree.setParentId(parentId);
		tree.setTitle(title);
		tree.setType(type);
		return tree;
	}

	static class NormalTree implements Tree {

		private String id;

		private String parentId;

		private String title;

		private Integer type;

		private List<? extends Tree> sons;

		@Override
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@Override
		public String getParentId() {
			return parentId;
		}

		public void setParentId(String parentId) {
			this.parentId = parentId;
		}

		@Override
		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		@Override
		public Integer getType() {
			return type;
		}

		public void setType(Integer type) {
			this.type = type;
		}

		@Override
		public List<? extends Tree> getSons() {
			return sons;
		}

		@Override
		public void setSons(List<? extends Tree> sons) {
			this.sons = sons;
		}

	}

}

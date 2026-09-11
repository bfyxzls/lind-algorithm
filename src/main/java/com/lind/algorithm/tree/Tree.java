package com.lind.algorithm.tree;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 层级树节点契约（菜单 / 组织等业务树），由扁平列表组装成父子结构。
 * <p>
 * 与 {@link Trie}、{@link BinaryTreeNode}、{@link RedBlackTree} 等算法树不同，本接口面向业务领域树形数据。
 * </p>
 */
public interface Tree {

	String getId();

	String getParentId();

	String getTitle();

	Integer getType();

	List<? extends Tree> getSons();

	void setSons(List<? extends Tree> sons);

	/**
	 * 为一级节点递归填充子节点（仅 {@code type == 0} 的节点参与挂载，兼容历史菜单数据）。
	 * @param roots 根或当前层节点
	 * @param all 全部扁平节点
	 */
	default void fillChildren(List<? extends Tree> roots, List<? extends Tree> all) {
		if (isEmpty(roots) || isEmpty(all)) {
			return;
		}
		for (Tree tree : roots) {
			if (tree == null || tree.getId() == null) {
				continue;
			}
			List<Tree> sons = all.stream().filter(o -> o != null && o.getParentId() != null
					&& o.getParentId().equals(tree.getId()) && Objects.equals(o.getType(), 0))
					.collect(Collectors.toList());
			if (!isEmpty(sons)) {
				tree.setSons(sons);
				fillChildren(sons, all);
			}
		}
	}

	/**
	 * @deprecated 使用 {@link #fillChildren(List, List)}
	 */
	@Deprecated
	default void fillTreeRzSons(List<? extends Tree> ones, List<? extends Tree> all) {
		fillChildren(ones, all);
	}

	/**
	 * 将当前节点深拷贝为带完整子树的新实例。
	 * @param allList 全部扁平节点
	 * @return 以当前节点为根的树
	 */
	@SuppressWarnings("unchecked")
	default <T extends Tree> T convert(List<T> allList) {
		if (allList == null) {
			throw new IllegalArgumentException("allList must not be null");
		}
		try {
			Class<T> clazz = (Class<T>) this.getClass();
			T node = clazz.getDeclaredConstructor().newInstance();
			copyBasicFields(this, node);
			List<Tree> children = allList.stream()
					.filter(subItem -> subItem.getParentId() != null && subItem.getParentId().equals(this.getId()))
					.map(subItem -> subItem.convert(allList)).collect(Collectors.toList());
			node.setSons(children);
			return node;
		}
		catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
			throw new IllegalStateException("Failed to create tree node of type " + this.getClass().getName(), e);
		}
	}

	/**
	 * @deprecated 使用 {@link #convert(List)}
	 */
	@Deprecated
	default <T extends Tree> T covert(List<T> allList) {
		return convert(allList);
	}

	private static boolean isEmpty(List<?> list) {
		return list == null || list.isEmpty();
	}

	/**
	 * 拷贝业务树基础字段（id / parentId / title / type），不依赖 Spring BeanUtils。
	 */
	private static void copyBasicFields(Object source, Object target) {
		String[] fields = { "Id", "ParentId", "Title", "Type" };
		for (String field : fields) {
			try {
				Method getter = source.getClass().getMethod("get" + field);
				Object value = getter.invoke(source);
				Method setter = target.getClass().getMethod("set" + field, getter.getReturnType());
				setter.invoke(target, value);
			}
			catch (NoSuchMethodException ignored) {
				// 实现类若无某字段 setter 则跳过
			}
			catch (IllegalAccessException | InvocationTargetException e) {
				throw new IllegalStateException("Failed to copy field: " + field, e);
			}
		}
	}

}

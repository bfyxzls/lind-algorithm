# tree 包（com.lind.algorithm.tree）

本包提供一组常用树结构工具，**每种结构只保留一个实现**。

| 类 | 类型 | 说明 |
|---|---|---|
| [`Trie`](Trie.java) | 字典树 / 前缀树 | 整词/前缀查询；中英文分词；文本关键词扫描 |
| [`BinaryTreeNode`](BinaryTreeNode.java) | 二叉树 | 前序 / 中序 / 后序遍历 |
| [`RedBlackTree`](RedBlackTree.java) | 红黑树 | 自平衡 BST |
| [`LsmTree`](LsmTree.java) | **磁盘 LSM 树** | MemTable + WAL + SSTable 文件；flush / compact / 重启恢复 |
| [`Tree`](Tree.java) | 业务层级树 | 菜单/组织等扁平列表组装 |

## LSM 树（磁盘版）

真实存储引擎（LevelDB / RocksDB / Cassandra / HBase）的 LSM 都将 SSTable **持久化到磁盘**，并用 WAL 保证崩溃不丢写。本模块 `LsmTree` 同样按此模型实现：

```
dataDir/
  MANIFEST     # SSTable 列表（新→旧）
  wal.log      # 写前日志
  sst-N.sst    # 不可变有序表
```

```java
try (LsmTree lsm = new LsmTree(Path.of("/data/lsm"), 64)) {
    lsm.put("user:1", "{\"name\":\"lind\"}");
    lsm.get("user:1");
    lsm.delete("user:1");
    lsm.flush();
    lsm.compact();
}
// 再次打开同一目录即可恢复数据
```

后续可增强：BloomFilter、稀疏索引、分级（Level）压缩、批量写降低 fsync 频率等。

## Trie 示例

```java
Trie trie = new Trie();
trie.insert("apple");
trie.search("apple");
```

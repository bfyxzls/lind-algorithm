# tree 包（com.lind.algorithm.tree）

本包提供一组常用树结构工具，**每种结构只保留一个实现**。

| 类 | 类型 | 一句话定位 |
|---|---|---|
| [`Trie`](Trie.java) | 字典树 / 前缀树 | 字符串前缀检索、词典分词、关键词扫描 |
| [`BinaryTreeNode`](BinaryTreeNode.java) | 二叉树 | 遍历与算法题基础结构 |
| [`RedBlackTree`](RedBlackTree.java) | 红黑树 | 内存有序集合，近似 O(log n) 查找 |
| [`BPlusTree`](BPlusTree.java) | B+ 树 | 有序 KV、范围扫描（数据库索引模型） |
| [`RTree`](RTree.java) | R 树 | 二维空间矩形相交查询 |
| [`Ast`](Ast.java) | 抽象语法树 | 表达式语义树，可求值 |
| [`Cst`](Cst.java) | 具体语法树 | 保留括号等词法细节的解析树 |
| [`DecisionTree`](DecisionTree.java) | 决策树 | 按特征分支的规则分类 |
| [`ProcessTree`](ProcessTree.java) | 流程树 | 审批 / 编排类业务流程 |
| [`MultiWayTree`](MultiWayTree.java) | 多叉树 | 组织架构等纯业务 N 叉层级 |
| [`LsmTree`](LsmTree.java) | 磁盘 LSM 树 | 写多读少的持久化 KV |
| [`Tree`](Tree.java) | 业务菜单树契约 | 菜单扁平列表组装（含 type 过滤） |

---

## 使用场景总览

| 结构 | 适合 | 不适合 |
|---|---|---|
| **Trie** | 前缀匹配、自动补全、敏感词扫描、分词词典 | 数值范围查询、磁盘超大词库 |
| **BinaryTreeNode** | 教学演示、遍历练习 | 大规模动态插入（无平衡） |
| **RedBlackTree** | 内存有序集合 / 索引 | 磁盘落盘、字符串前缀 |
| **BPlusTree** | 有序点查 + 范围扫描、页式索引教学 | 空间查询（用 R 树）、前缀字符串（用 Trie） |
| **RTree** | GIS、地图标注、二维范围检索 | 一维有序 KV（用 B+ / 红黑树） |
| **Ast** | 编译器中端、表达式引擎、公式求值 | 需要还原源码格式时（用 CST） |
| **Cst** | 解析器前端、语法高亮、格式保持 | 直接做优化 / 求值（先投影 AST） |
| **DecisionTree** | 风控分流、规则引擎、可解释分类 | 需要从样本自动训练的复杂 ML（可接专用库） |
| **ProcessTree** | 审批流、简易工作流编排 | 分布式并行 BPMN 引擎 |
| **MultiWayTree** | 组织架构、汇报线、通用 N 叉业务树 | 菜单 type 过滤场景（用 `Tree`） |
| **LsmTree** | 嵌入式 KV、写吞吐、崩溃恢复 | 复杂 SQL / 强随机读优先 |
| **Tree** | 后台菜单 / 权限树组装 | 算法检索、通用组织树（用 MultiWayTree） |

---

## Trie（字典树 / 前缀树）

**核心能力**：整词插入 / 查询 / 前缀判断 / 词频；文本关键词扫描；英文空白分词、中文正向最大匹配分词。

| 场景 | 说明 |
|---|---|
| 搜索自动补全 | `startsWith` |
| 敏感词 / 实体词统计 | `scan` |
| 中文分词词典 | `insertAll` + 正向最大匹配 |
| URL / 路由前缀 | 按字符逐级匹配 |

```java
Trie trie = new Trie();
trie.insert("apple");
trie.search("apple");
trie.startsWith("app");
```

---

## BinaryTreeNode（二叉树）

**核心能力**：前序 / 中序 / 后序遍历。

| 场景 | 说明 |
|---|---|
| 算法学习 / 面试题 | 最小可测树结构 |
| 表达式树骨架 | 自建左右子树后遍历 |

---

## RedBlackTree（红黑树）

**核心能力**：自平衡 BST，查找约 O(log n)。

| 场景 | 说明 |
|---|---|
| 有序集合底层 | 对齐 `TreeMap` / `TreeSet` 思想 |
| 内存动态索引 | 避免普通 BST 退化 |

---

## BPlusTree（B+ 树）

**核心能力**：数据全在叶子；叶子有序链表；`put` / `get` / 闭区间 `range`。

| 场景 | 说明 |
|---|---|
| 数据库二级索引模型 | MySQL InnoDB 等经典结构 |
| 有序 KV + 范围扫描 | `[from, to]` 顺序读 |
| 存储引擎教学 | 理解页分裂与分隔键 |

```java
BPlusTree<Integer, String> idx = new BPlusTree<>(4);
idx.put(10, "a");
idx.range(5, 20);
```

---

## RTree（R 树）

**核心能力**：MBR 索引；插入；矩形相交查询；节点溢出线性分裂。

| 场景 | 说明 |
|---|---|
| GIS / 地图 POI | 视窗内对象检索 |
| 碰撞 / 标注避让 | 矩形相交粗筛 |
| 二维范围查询 | 比暴力扫全表更快 |

```java
RTree<String> spatial = new RTree<>();
spatial.insert(new RTree.Rectangle(0, 0, 1, 1), "park");
spatial.search(new RTree.Rectangle(0, 0, 2, 2));
```

---

## Ast（抽象语法树）

**核心能力**：解析 `+ - * /` 与括号；`evaluate` 求值；结构只保留语义。

| 场景 | 说明 |
|---|---|
| 表达式 / 规则引擎 | 配置公式计算 |
| 编译器中端 | 语义分析与优化的输入 |
| 脚本片段求值 | 去掉括号噪音后的树 |

```java
Ast.AstNode ast = Ast.parse("(1+2)*3");
ast.evaluate(); // 9
```

---

## Cst（具体语法树）

**核心能力**：保留括号与运算符词法；`lexicalTokens`；`Cst.toAst` 投影。

| 场景 | 说明 |
|---|---|
| 解析器 / 语法高亮 | 需要源码位置与符号 |
| 代码格式化 | 保留括号等表层结构 |
| 教学：CST → AST | 对比具体树与抽象树 |

```java
Cst.CstNode cst = Cst.parse("(1+2)*3");
cst.lexicalTokens();          // [(, 1, +, 2, ), *, 3]
Cst.toAst(cst).evaluate();    // 9
```

---

## DecisionTree（决策树）

**核心能力**：离散特征等值分支 + 默认分支；`predict`。

| 场景 | 说明 |
|---|---|
| 风控 / 信贷分流 | 收入、信用等级 → 通过/拒绝 |
| 可解释规则引擎 | 业务可直接读树 |
| 客服 / 工单路由 | 按类型、等级分派 |

```java
DecisionTree tree = new DecisionTree(
    DecisionTree.Node.branch("income")
        .when("high", DecisionTree.Node.leaf("approve"))
        .when("low", DecisionTree.Node.leaf("reject"))
        .build());
tree.predict(Map.of("income", "high"));
```

---

## ProcessTree（流程树）

**核心能力**：START / TASK / XOR / AND / END；条件分支；执行轨迹。

| 场景 | 说明 |
|---|---|
| 审批流 | 通过 / 驳回 XOR |
| 简易工作流编排 | 顺序任务 + 汇合 |
| 流程演示 / 单测 | 断言经过的节点 id |

```java
ProcessTree flow = new ProcessTree(
    ProcessTree.ProcessNode.start("s")
        .child(ProcessTree.ProcessNode.task("review")
            .child(ProcessTree.ProcessNode.end("e").build())
            .build())
        .build());
flow.execute(Map.of());
```

---

## MultiWayTree（多叉树 / 组织架构）

**核心能力**：`fromFlat` 组装；查找；根到节点路径；DFS / BFS；高度与规模。

| 场景 | 说明 |
|---|---|
| 公司组织架构 | CEO → 部门 → 组 |
| 汇报线 / 祖先路径 | `pathTo` |
| 通用 N 叉业务树 | 类目、地区（无菜单 type 约束） |

与 [`Tree`](Tree.java) 区别：`Tree` 面向菜单契约（含 `type == 0` 过滤）；`MultiWayTree` 是通用组织/业务多叉树。

```java
MultiWayTree<String> org = MultiWayTree.fromFlat(List.of(
    MultiWayTree.FlatNode.root("ceo", "CEO"),
    MultiWayTree.FlatNode.of("eng", "ceo", "Engineering")));
org.pathTo("eng");
```

---

## LsmTree（磁盘 LSM 树）

真实存储引擎（LevelDB / RocksDB / Cassandra / HBase）的 LSM 都将 SSTable **持久化到磁盘**，并用 WAL 保证崩溃不丢写：

```
dataDir/
  MANIFEST     # SSTable 列表（新→旧）
  wal.log      # 写前日志
  sst-N.sst    # 不可变有序表
```

| 场景 | 说明 |
|---|---|
| 嵌入式本地 KV | 配置 / 状态落盘 |
| 写多读少 | 随机写转顺序写 |
| 崩溃可恢复 | 再打开同目录即可 |

```java
try (LsmTree lsm = new LsmTree(Path.of("/data/lsm"), 64)) {
    lsm.put("user:1", "{\"name\":\"lind\"}");
    lsm.get("user:1");
}
```

---

## Tree（业务菜单树契约）

**核心能力**：`fillChildren` / `convert`；扁平菜单 → 父子树。

| 场景 | 说明 |
|---|---|
| 后台菜单 / 权限树 | DB 行 → 前端 JSON |
| 历史菜单 type 过滤 | 仅 `type == 0` 挂载 |

---

## 如何选型（简表）

```text
字符串前缀 / 分词 / 敏感词？          → Trie
有序内存集合？                        → RedBlackTree
有序 KV + 范围扫描（索引模型）？      → BPlusTree
二维空间矩形查询？                    → RTree
表达式求值 / 语义树？                 → Ast
要保留括号等源码结构？                → Cst
规则分流 / 可解释分类？               → DecisionTree
审批流 / 简易编排？                   → ProcessTree
组织架构 / 通用 N 叉业务树？          → MultiWayTree
菜单扁平组装（含 type）？             → Tree
落盘写多 KV？                         → LsmTree
遍历演示 / 算法题？                   → BinaryTreeNode
```

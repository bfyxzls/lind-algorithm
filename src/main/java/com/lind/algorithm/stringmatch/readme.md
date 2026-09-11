# stringmatch 包（com.lind.algorithm.stringmatch）

| 类 | 说明 |
|---|---|
| `KmpMatcher` | 单模式 KMP |
| `AhoCorasick` | 多模式 AC 自动机 |

相对 `Trie.scan`：AC 更适合大规模多关键词一次扫描。

```java
AhoCorasick ac = new AhoCorasick();
ac.addKeyword("foo");
ac.addKeyword("bar");
ac.findAll("foobar");
```

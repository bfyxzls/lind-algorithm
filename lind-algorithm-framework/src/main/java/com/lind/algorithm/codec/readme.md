# codec 包（com.lind.algorithm.codec）

| 类 | 说明 |
|---|---|
| `Base62` | 长整型 / 字节数组 ↔ Base62（短链 ID） |

```java
String shortId = Base62.encode(1_234_567_890L);
long id = Base62.decode(shortId);
String fromBytes = Base62.encode("hello".getBytes());
```

字母表 `0-9A-Za-z`，配合短链爬虫、分布式 ID 对外暴露可读短码。

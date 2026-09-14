# topk 包（com.lind.algorithm.topk）

| 类 | 说明 |
|---|---|
| `TopKCounter` | 精确频次 + 小顶堆 Top-K |
| `CountMinSketch` | 流式近似计数 |

```java
TopKCounter<String> top = new TopKCounter<>(10);
top.offer("api./order");
top.topK();
```

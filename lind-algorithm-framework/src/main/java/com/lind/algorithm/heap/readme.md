# heap 包（com.lind.algorithm.heap）

| 类 | 说明 |
|---|---|
| `BinaryHeap` | 二叉堆优先队列（Comparator 决定堆序） |

```java
BinaryHeap<Integer> min = BinaryHeap.naturalOrder();
min.offer(3);
min.offer(1);
min.poll(); // 1

BinaryHeap<Integer> max = new BinaryHeap<>(Comparator.reverseOrder());
```

适合 Top-K、定时调度、合并有序流等需要反复取最值的场景。

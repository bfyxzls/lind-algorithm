# bitmap 包（com.lind.algorithm.bitmap）

位图集合运算：签到、标签交集、人群包。

```java
Bitmap a = new Bitmap();
a.set(1);
Bitmap b = new Bitmap();
b.set(1);
a.and(b).cardinality(); // 1
```

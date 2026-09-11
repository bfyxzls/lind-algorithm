# id 包（com.lind.algorithm.id）

雪花算法分布式 ID（`SnowflakeIdGenerator`）。

```java
SnowflakeIdGenerator gen = new SnowflakeIdGenerator(1);
long id = gen.nextId();
```

注意：依赖机器时钟；多实例请分配不同 `workerId`（0..1023）。

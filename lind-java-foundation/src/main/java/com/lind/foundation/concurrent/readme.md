# Phase 3：锁与并发原语

对照模块根目录 [`README.md`](../../../README.md) **第三块：锁**。详解见 [`locks.md`](locks.md)。

| 顺序 | 主题 | 类 / 测试 | 状态 |
|---|---|---|---|
| 3.1 | `synchronized` 与丢失更新 | `LostUpdateDemos` / `LostUpdateDemoTest` | ✅ |
| 3.2 | `ReentrantLock` + `Condition` 有界队列 | `BoundedBuffer` / `BoundedBufferDemos` / `BoundedBufferTest` | ✅ |
| 3.3 | `ReadWriteLock` / StampedLock | — | 待建 |
| 3.4 | CAS / `Atomic*` | — | 待建 |
| 3.5 | 线程池与拒绝策略直觉 | — | 待建 |

```bash
mvn -pl lind-java-foundation -Dtest=LostUpdateDemoTest,BoundedBufferTest test

mvn -pl lind-java-foundation -q org.codehaus.mojo:exec-maven-plugin:3.5.0:java \
  "-Dexec.mainClass=com.lind.foundation.concurrent.LostUpdateDemos" "-Dexec.classpathScope=compile"

mvn -pl lind-java-foundation -q org.codehaus.mojo:exec-maven-plugin:3.5.0:java \
  "-Dexec.mainClass=com.lind.foundation.concurrent.BoundedBufferDemos" "-Dexec.classpathScope=compile"
```

学习口诀：先问「保护什么共享状态、读多还是写多」，再选锁；能用不可变 / 单线程局限域就不要加锁。

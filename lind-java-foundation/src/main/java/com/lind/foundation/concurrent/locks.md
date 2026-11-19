# 锁入门（3.1 / 3.2）

对照模块根目录 [`README.md`](../../../README.md) **第三块：锁**。  
可运行：`LostUpdateDemos`、`BoundedBuffer`；测试：`LostUpdateDemoTest`、`BoundedBufferTest`。

```bash
mvn -pl lind-java-foundation -Dtest=LostUpdateDemoTest,BoundedBufferTest test
```

---

## 3.1 先问：保护什么共享状态？

多线程同时改同一个变量（如计数器 `count++`），无同步时会出现 **丢失更新**：

```text
线程 A 读 count=0 → 加一得 1 → 写回
线程 B 读 count=0 → 加一得 1 → 写回   // A 的更新丢了
最终 count 小于「线程数 × 每线程次数」
```

`count++` 不是原子的：读 → 加 → 写，三步可交错。

| 做法 | 结果 |
|---|---|
| 无锁 | 经常少计（测试里断言会小于期望） |
| `synchronized` 包住临界区 | 等于期望 |
| `ReentrantLock` 包住同样临界区 | 等于期望 |

**可见性直觉：** 进/出 `synchronized`（或 Lock 的 unlock）会建立 happen-before，别的线程能看到你在临界区里写过的值。不是「加了锁就变快」，而是 **正确**。

业务映射：本地限流计数、简单统计、连接池里改共享集合前先想「有没有多线程在碰」。

---

## 3.2 ReentrantLock + Condition：有界队列

`synchronized` + `wait/notify` 能做生产者-消费者，但：

- 只有一个等待队列，要用 `notifyAll` 时容易惊群  
- 不能尝试锁、不能限时、不能公平锁开关  

`ReentrantLock` 可建 **多个** `Condition`：

```text
put 时若满 → await(notFull)；放入后 signal(notEmpty)
take 时若空 → await(notEmpty)；取出后 signal(notFull)
```

本包 `BoundedBuffer` 即此模型（容量固定的环形缓冲）。

| API | 含义 |
|---|---|
| `lock.lock()` / `unlock()` | 进/出临界区；**unlock 务必在 finally** |
| `notFull.await()` | 释放锁并等待；被 signal 后重新抢锁再往下跑 |
| `notEmpty.signal()` | 唤醒一个在 notEmpty 上等待的线程 |

业务映射：有界任务队列、流量整形、连接池里「池满等待 / 空闲归还」。

---

## synchronized vs ReentrantLock（何时选）

| | `synchronized` | `ReentrantLock` |
|---|---|---|
| 写法 | 简单，JVM 内置 | 显式 lock/unlock |
| 条件等待 | 单个 wait 集 | 多 Condition |
| 尝试 / 限时 | 无 | `tryLock` / `tryLock(timeout)` |
| 公平性 | 非公平为主 | 可公平 |
| 默认建议 | 简单互斥优先用它 | 要条件队列、尝试锁、公平时再用 |

口诀：**能简单就 synchronized；要精细控制再 ReentrantLock。**

---

## 跟练

1. 跑 `LostUpdateDemoTest`：看无锁少计、有锁正确。  
2. 改线程数 / 每线程次数，无锁「丢多少」更明显。  
3. 跑 `BoundedBufferTest`：多生产者多消费者，总数守恒。  
4. 读 `BoundedBuffer.put/take`，对照上面 put/take 两行伪代码。

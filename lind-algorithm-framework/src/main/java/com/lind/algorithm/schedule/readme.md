# schedule 包（com.lind.algorithm.schedule）

简化 5 域 Cron：`分 时 日 月 周`。

```java
CronExpression cron = new CronExpression("0 9 * * 1");
cron.next(LocalDateTime.now());
```

可与时间轮组合：算出下次触发点后 `newTimeout`。

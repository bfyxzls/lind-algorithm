# lind-delay-task-web

延时任务管理服务：MySQL 持久化（权威）+ `HashedWheelTimer`（内存加速）+ Thymeleaf 管理页。

## 运行

先在 MySQL 创建库：

```sql
CREATE DATABASE IF NOT EXISTS delay_task DEFAULT CHARACTER SET utf8mb4;
```

```bash
mvn -pl lind-delay-task-web -am spring-boot:run
# http://localhost:8090/admin/tasks
```

连接串见 `src/main/resources/application.yml`。

## 核心语义

- 创建时写入：`created_at`、`delay_ms`、`execute_at = created_at + delay_ms`
- 重启灌轮：剩余时间 = `max(0, execute_at - now)`，**不会**用「现在 + delay_ms」重新计时
- 列表/分页/删除均读库，不依赖时间轮快照

## 状态机

`PENDING → RUNNING → DONE`；取消 `CANCELLED`；失败可重试或 `DEAD`。

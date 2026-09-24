# Git 操作规范

面向互联网大厂协作习惯的分支与提交约定。目标：**主干干净、历史可追溯、 Code Review 可审、发布可回滚**。

---

## 1. 分支模型（推荐）

| 分支 | 用途 | 保护策略 |
|---|---|---|
| `main` / `master` | 生产对应主干，只接受 Review 后的合并 | 禁止直接 push；必须 PR/MR |
| `develop`（可选） | 集成分支，日构建 / 预发 | 同主干保护 |
| `feature/*` | 新功能，从主干拉取 | 短生命周期，合入后删除 |
| `bugfix/*` | 缺陷修复 | 同上 |
| `hotfix/*` | 线上紧急修复，从主干打出 | 合回主干（及 develop）后删除 |
| `release/*` | 发版冻结（可选） | 只收 bugfix |

命名示例：

```text
feature/user-login
bugfix/order-timeout
hotfix/pay-npe-20260924
```

原则：

- 一人一事一分支；避免多人长期共用同一 feature 分支。
- 分支尽量短（建议 1～3 天合入），减少冲突与 rebase 成本。
- 禁止在 `main` 上直接开发。

---

## 2. 提交信息（Commit Message）

推荐约定式提交（Conventional Commits）：

```text
<type>(<scope>): <subject>

[optional body]
[optional footer]
```

常见 `type`：

| type | 含义 |
|---|---|
| `feat` | 新功能 |
| `fix` | 缺陷修复 |
| `docs` | 文档 |
| `refactor` | 重构（不影响行为） |
| `test` | 测试 |
| `chore` | 构建 / 依赖 / 杂项 |
| `perf` | 性能 |

示例：

```text
feat(qps-checker): support POST form and query params
fix(delay-task): correct executeAt after restart
docs: add git workflow conventions
```

要求：

- 用 **现在时、祈使句**（add / fix，不要 added / fixed）。
- 主题一行说清「为什么 / 做了什么」，不要只写 `update`、`临时提交`。
- 关联需求或缺陷号写在 footer：`Refs: #123` / `Closes: #456`。
- **禁止**把密钥、密码、内网地址写进提交内容。

---

## 3. 日常操作约定

### 3.1 开始开发

```bash
git fetch origin
git checkout main
git pull --ff-only origin main
git checkout -b feature/xxx
```

优先 `--ff-only`，避免在主干上产生无意义的 merge commit。

> 从远程 origin 拉取 main 分支的最新提交，并只允许以“快进（fast-forward）”方式合并到当前分支。如果无法快进，就直接失败，不会自动创建合并提交。

--ff-only总结：
- 如果本地落后于远程，无分叉，就变成和远程一样 
  - origin a---b---c
  - 本地 a---b
  - 本地合并后 a---b---c
- 如果本地优先于远程
  - origin a---b
  - 本地 a---b---c
  - 本地合并后`Already up to date`，本地保持不变
- 如果本地和远程分叉：--ff-only 直接失败

### 3.2 推送与更新

```bash
# 首次推送
git push -u origin HEAD

# 本地已整理过历史再推（见 rebase 章节）
git push --force-with-lease
```

- 使用 `--force-with-lease`，**不要**用裸 `--force`（防止覆盖他人新推送）。
- **禁止**对 `main` / 共享集成分支 force push。

### 3.3 Code Review 与合入

- 所有合入主干走 Pull Request / Merge Request。
- CI 必须通过；至少 1 人 Approve（核心模块可要求 2 人）。
- PR 描述写清：背景、改动点、测试方式、风险与回滚。
- 合入后删除远程 feature 分支。

### 3.4 禁止事项

- 不在公共分支上 `git commit --amend` / rebase 已推送且他人已基于其上的提交（除非团队明确约定并通知全员）。
- 不提交 `target/`、`.idea/`、本地配置、超大二进制（用 Git LFS 或制品库）。
- 不把多个无关需求塞进同一个 PR。

---

## 4. `git rebase` 与 `git merge`：场景对照（重点）

两者都能「合并历史」，但产物不同：

| | `merge` | `rebase` |
|---|---|---|
| 历史形态 | 保留分支分叉，多一个 merge commit | 把提交「挪到」目标分支尖端，直线历史 |
| 冲突解决 | 集中在一次 merge | 可能按 commit 逐个解决 |
| 可追溯性 | 清晰看到「何时合入了哪条分支」 | 主干更干净，分叉过程被改写 |
| 是否改写已有 commit | 一般不改写 | **会改写**（hash 变了） |

```text
merge 后：

  o---o---o---o---M     main
       \         /
        a---b---c       feature


rebase 后：

  o---o---o---o---a'---b'---c'   main（或更新后的 feature）
```

### 4.1 什么时候用 `rebase`

**1）个人 feature 分支同步主干（最常用）**

主干有了新提交，你的 feature 落后时，用 rebase 让自己的提交接在最新主干后面，保持 PR 历史一条线，Review 更轻松：

```bash
git fetch origin
git checkout feature/xxx
git rebase origin/main
# 若已 push 过该 feature：
git push --force-with-lease
```

适用条件：

- 分支 **只有你自己**（或小组明确同意改写）。
- 目的是「更新基线」，不是「保留合入事件」。

**2）合入前整理本地提交（interactive rebase）**

把「wip / fix typo / 临时调试」收成逻辑清晰的少量 commit：

```bash
git rebase -i origin/main
```

在编辑器里 `pick` / `squash` / `reword`，让每个 commit 都可独立理解。

**3）主干策略为「Rebase and merge」或「Squash and merge」时**

很多大厂 GitHub/GitLab 对 PR 默认：

- **Squash and merge**：feature 上再乱，合入主干只留 1 个 commit（常用）。
- **Rebase and merge**：把 feature 的多个 commit 依次接到主干，主干无 merge commit。

此时开发者在 PR 打开期间仍应用 rebase 同步主干，减少合入冲突。

### 4.2 什么时候用 `merge`

**1）合入公共 / 长期分支（保留合入节点）**

例如 `release/*` 合回 `main`，或 `main` ← `develop`，需要明确「这次发版/集成」的边界：

```bash
git checkout main
git pull --ff-only
git merge --no-ff release/1.2.0
```

`--no-ff` 即使可以快进也生成 merge commit，方便日后按合并点回滚整次发版。

**2）分支已被多人共享**

别人已经基于你的分支继续开发时，**不要 rebase 改写历史**，用 merge 引入主干更新：

```bash
git checkout feature/shared
git merge origin/main
```

**3）需要保留真实协作时间线**

审计、合规、或需要证明「某天从主干分出、某天合入」时，merge 历史更直观。

**4）解决「已推送到共享远程」的集成冲突**

对共享分支，merge 是更安全的默认选项。

### 4.3 场景速查表

| 场景 | 推荐 | 原因 |
|---|---|---|
| 个人 feature 追上最新 `main` | **rebase** | 直线历史，PR 干净 |
| 合入前清理本地碎提交 | **rebase -i** | 便于 Review |
| PR 合入主干（平台配置） | Squash / Rebase merge | 主干不被噪音 commit 污染 |
| 多人共用的 feature / develop | **merge** | 不改写他人已拉取的 commit |
| release / hotfix 合回主干 | **merge --no-ff** | 保留发版合入点，便于回滚 |
| 已经 push 到 main 的历史 | **禁止 rebase** | 避免全员强推与协作事故 |

### 4.4 冲突处理注意

- rebase 冲突：改完后 `git add` → `git rebase --continue`；放弃则 `git rebase --abort`。
- merge 冲突：改完后 `git add` → `git commit`（完成合并）；放弃则 `git merge --abort`。
- 冲突解决后应再跑本地测试 / CI，不要「只求能合进去」。

### 4.5 大厂常见落地组合（可直接照做）

1. **日常开发**：`feature` 上开发；每天或开 PR 前 `rebase origin/main`。
2. **开 PR**：标题与描述规范；CI + Review。
3. **合入主干**：优先 **Squash and merge**（或团队统一的 Rebase and merge）。
4. **发版 / 热修**：`hotfix` → `main` 用 **merge --no-ff**；再同步回 `develop`（若有）。
5. **永远**：不对 `main` force push；对个人远程分支只用 `--force-with-lease`。

一句话记忆：

> **自己的分支整理用 rebase；公共分支合入用 merge（或平台提供的 squash/rebase merge）。**  
> **改写历史只限「尚未共享或仅自己使用」的提交。**

---

## 5. 与本仓库相关的建议

- 多模块改动尽量按模块拆 PR（如只改 `lind-qps-checker` 时不要夹带无关文档大改，除非同一需求）。
- 提交前本地执行：`mvn -pl <模块> -am test`。
- 生成代码 / 格式化由 CI 或 `spring-javaformat` 统一约束，避免无意义的「仅空格」提交。

---

## 6. 快速命令清单

```bash
# 同步主干到个人分支（推荐）
git fetch origin
git rebase origin/main

# 放弃本次 rebase
git rebase --abort

# 安全强推（仅个人分支）
git push --force-with-lease

# 公共分支合并主干更新
git merge origin/main

# 带合入节点的合并
git merge --no-ff feature/xxx

# 查看分叉与合入
git log --oneline --graph --decorate -20
```

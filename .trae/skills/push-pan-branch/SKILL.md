---
name: push-pan-branch
description: 把当前 lzj 分支的改动推送到 pan 集成分支（提交→推lzj→切pan→pull→merge→推pan→切回lzj→同步pan）。用户说 推送到 pan、提交到 pan、帮我推送 pan 分支、把项目推上去 时触发。不要用于其他分支或其他仓库。
---

# Push Pan Branch（完整合并流程）

## 分工

| 步骤 | 做什么 |
|------|--------|
| 你 | 在 `lzj` 上写代码，改完说「推送到 pan」 |
| 本 skill | 四步自动走完整流程：提交 → 推 lzj → 切 pan 拉取合并 → 推 pan → 切回 lzj 同步 |

## 前置检查

1. 工作目录固定 `d:\msjs`，命令加 `-cwd d:\msjs`
2. `git status -sb` 确认在 `lzj`，不是就 `git switch lzj`

## 流程

**所有命令必须关闭沙箱**（`dangerouslyDisableSandbox: true`）保证 SSH 可用。

### ① lzj 有改动 → 自动 commit + push 到 origin/lzj

```powershell
if ((git status --porcelain).Count -gt 0) {
    git add -A
    git -c core.quotepath=false commit -m "feat: 中文描述本次改动核心交付物"
}
git push origin lzj 2>&1
```

commit message 前缀：`feat` 新功能 / `fix` 修复 / `refactor` 重构 / `chore` 配置小改。

### ② 切 pan → pull origin pan（先拉别人的）

```powershell
git switch pan 2>&1
git pull origin pan 2>&1
```

### ③ merge lzj → push origin pan

```powershell
git merge lzj 2>&1
git push origin pan 2>&1
```

### ④ 切回 lzj → merge origin pan（把最新同步给你）

```powershell
git switch lzj 2>&1
git pull origin pan 2>&1
```

## 冲突解决

merge 时遇到冲突，按以下原则手动解决：

- **pan 是集成分支**：实体类、公共配置等 pan 上他人加的注释/字段保留，lzj 的业务改动保留，两边的路由**全部保留**（不是二选一）
- 解决后 `git add <文件列表>` → `git -c core.quotepath=false commit -m "merge: 合并lzj到pan，解决xxx冲突"` → push → 切回 lzj 同步
- 解决过程中可随时 `git merge --abort` 放弃回到 merge 前状态

## 常见陷阱

- **SQL 脚本不入库**：数据库是共享库，`sql/` 已在 `.gitignore`，新建 DDL/DML 只留本地文件，绝不提交推送
- 不要在 pan 上直接开发，pan 只做集成分支
- PowerShell 里 `git push` 进度写到 stderr 显示红色是正常的，退出码 0 即成功
- 运行时组件和构建产物已在 `.gitignore`，不会被 `git add -A` 收录
- 如果步骤 ③ 的 `git merge lzj` 被 `--no-ff` 阻止或 panic，加 `--no-ff` 参数：`git merge --no-ff lzj`

## 成功反馈

向用户报：
- 提交哈希 + 改动文件数
- 四步流程每一步的结果（成功 / 冲突解决说明）

---
name: push-pan-branch
description: 把当前 lzj 分支的改动推送到 pan 分支（只提交和推送，不切分支、不拉取、不合并、不处理冲突）。用户说 推送到 pan、提交到 pan、帮我推送 pan 分支、把项目推上去 时触发。不要用于其他分支或其他仓库。
---

# Push Pan Branch（只推不合并）

## 分工

| 角色 | 做什么 |
|------|--------|
| 你 | 在 `lzj` 上写代码，改完说「推送到 pan」 |
| 本 skill | 提交 `lzj` → push `origin/lzj` → push `lzj` 到 `origin/pan` |
| 你不管 | 切 pan、pull、merge、解决冲突、同步 pan 回 lzj —— 全部由合并负责人处理 |

## 前置检查

1. 工作目录固定 `d:\msjs`，命令加 `-cwd d:\msjs`
2. `git status -sb` 确认在 `lzj`，不是就 `git switch lzj`

## 流程

**所有命令必须关闭沙箱**（`dangerouslyDisableSandbox: true`）保证 SSH 可用。

```powershell
# 1. 有改动 → 提交
if ((git status --porcelain).Count -gt 0) {
    git add -A
    git -c core.quotepath=false commit -m "feat: 中文描述本次改动核心交付物"
}

# 2. 推自己的分支
git push origin lzj 2>&1

# 3. 把自己的分支推到 pan（只这一步，不切分支、不 pull、不 merge）
git push origin lzj:pan 2>&1
```

commit message 前缀：`feat` 新功能 / `fix` 修复 / `refactor` 重构 / `chore` 配置小改。

## 推送被拒

`rejected` / `non-fast-forward` / `fetch first` 说明 pan 已有他人提交：

- **不要** pull、**不要** merge、**不要** 切到 pan、**不要** 手动解决冲突
- 直接告知用户：推送被拒，需由合并负责人处理
- 可附 `git log --oneline origin/pan..lzj` 让用户知道领先的提交

## 常见陷阱

- 只做「提交 + 推 pan」，**切分支 / pull / merge / 解决冲突一律不做**
- 不要在 pan 上直接开发
- **SQL 脚本不入库**：数据库是共享库，`sql/` 已在 `.gitignore`，新建 DDL/DML 只留本地文件，绝不提交推送
- PowerShell 里 `git push` 进度写到 stderr 显示红色是正常的，退出码 0 即成功
- 运行时组件和构建产物已在 `.gitignore`，不会被 `git add -A` 收录

## 成功反馈

向用户报：
- 提交哈希 + 改动文件数
- `origin/lzj` 与 `origin/pan` 的推送结果（成功 / 被拒原因）

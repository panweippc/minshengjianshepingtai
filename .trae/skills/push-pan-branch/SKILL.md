---
name: push-pan-branch
description: 将当前 lzj 分支的改动合并推送到 pan 集成分支。用户说 推送到 pan、提交到 pan、帮我推送 pan 分支、把项目推上去 时触发。不要用于其他分支或其他仓库。
---

# Push Pan Branch（只做合并推 pan 这一件事）

## 分工

| 角色 | 做什么 |
|------|--------|
| 你 | 在 `lzj` 上写代码，改完说「推送到 pan」 |
| 本 skill | lzj 提交 → 切 pan → pull → merge lzj → push pan |
| 你不管 | 切回 lzj、同步 pan 到 lzj、main 合并——全砍 |

## 前置检查

1. 工作目录固定 `d:\msjs`，命令加 `-cwd d:\msjs`
2. `git status -sb` 确认在 `lzj` 分支，不是就 `git switch lzj`
3. `git fetch origin` 拉远端最新

## 流程

**所有命令必须关闭沙箱**（`dangerouslyDisableSandbox: true`）保证 SSH 可用。

```powershell
# 1. lzj 有改动 → 自动提交并推到 origin/lzj
if ((git status --porcelain).Count -gt 0) {
    git add -A
    git -c core.quotepath=false commit -m "feat: 中文描述本次改动核心交付物"
    git push origin lzj 2>&1
}

# 2. 切 pan，拉最新，合 lzj，推 pan
git switch pan
git pull origin pan 2>&1
git merge lzj 2>&1
git push origin pan 2>&1
```

commit message 前缀：`feat` 新功能 / `fix` 修复 / `refactor` 重构 / `chore` 配置小改。

## 合并冲突

`git merge lzj` 冲突时：
1. `git status` 看冲突文件
2. 手动处理 `<<<<<<< HEAD / ======= / >>>>>>> lzj` 标记（HEAD 是 pan 侧，lzj 是你的）
3. `git add <文件>` 标记已解决
4. `git commit`
5. 回到流程 `git push origin pan`

## 失败场景

| 报错 | 处理 |
|------|------|
| `denied` / `Could not read` | 仓库 Settings → Collaborators 加账号 Write 权限 |
| `non-fast-forward` | 重跑 `git pull origin pan` 再 `git push` |
| 合并冲突 | 按上面步骤手动解决 |

## 常见陷阱

- 只做「推 pan」，**切回 lzj / merge origin/pan 回 lzj 全部不做**，用户自己需要时自己切
- 不要在 pan 上直接开发
- PowerShell 里 `git push` 进度写到 stderr 显示红色是正常的，退出码 0 即成功
- 运行时组件和构建产物已在 `.gitignore`，不会被 `git add -A` 收录

## 成功反馈

推送完向用户报：
- 合并进 pan 的提交数 + 最新 commit 哈希
- 一句提醒「当前在 pan 分支，需要切回 lzj 自己切」

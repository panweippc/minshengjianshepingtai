---
name: push-pan-branch
description: 将当前 lzj 个人分支的改动合并推送到 pan 集成分支。用户说 推送到 pan、提交到 pan、帮我推送 pan 分支、把项目推上去 时触发。不要用于其他分支或其他仓库。
---

# Push Pan Branch（lzj → pan）

## 分工

| 角色 | 做什么 |
|------|--------|
| 你 | 只在 `lzj` 上写代码，改完触发本 skill 推 pan |
| 我（skill） | 自动完成合并 + 推 pan + 把 pan 最新同步回 lzj |
| 其他人 | 在 pan 上做后续集成/合并 main，你不管 |

## 日常拉取最新

每次开始写新功能前，先让自己的 `lzj` 跟上 pan 最新（含别人合进去的）：

```powershell
git fetch origin
git merge origin/pan
```

如果有冲突，手动解决后 `git add <冲突文件>` 再 `git commit`。

## 触发条件

- 用户说 **推送到 pan / 提交到 pan / 帮我推 pan / 把项目推上去** 等类似含义的话
- 不用于：切分支、推其他分支、创建分支

## 前置检查

1. 工作目录固定为 `d:\msjs`，命令加 `-cwd d:\msjs`
2. 执行 `git status -sb` 确认当前在 `lzj` 分支；若不是，`git switch lzj`
3. 执行 `git fetch origin` 拉远端最新元数据

## 推送流程（全部自动完成）

**所有命令都要关闭沙箱**（`dangerouslyDisableSandbox: true`）以保证 SSH 密钥可用。

```powershell
# ── Step 1: lzj 上有没有未提交/未推送的改动？有就先提交推到 origin/lzj ──

$dirty = git status --porcelain
if ($dirty) {
    git add -A
    git -c core.quotepath=false commit -m "feat: 中文描述本次改动的核心交付物"
    git push origin lzj 2>&1
}
# 已跟踪未提交 = 自动提交，commit message 前缀约定：
#   feat:     新功能模块
#   fix:      Bug 修复
#   refactor: 重构（不改行为）
#   chore:    构建/配置/小改动

# ── Step 2: 切到 pan，先拉远端最新 pan（别人的改动），避免非 fast-forward ──

git switch pan
git pull origin pan 2>&1

# ── Step 3: 把 lzj 合进 pan，再推上去 ──

git merge lzj 2>&1
git push origin pan 2>&1

# ── Step 4: 切回 lzj，把 pan 最新同步回来（含别人刚合的），继续开发 ──

git switch lzj
git merge origin/pan 2>&1

# ── Step 5: 校验 ──

git fetch origin 2>&1
git status -sb
git branch -vv
```

## 合并冲突处理

如果 Step 3 或 Step 4 的 `git merge` 提示冲突：

1. `git status` 看冲突文件（状态 `both modified`）
2. 打开文件，手动处理冲突标记：
   ```
   <<<<<<< HEAD      ← 当前分支内容（pan 侧或 origin/pan 侧）
   ...
   =======           ← 你的 lzj 内容
   ...
   >>>>>>> lzj
   ```
   删除所有标记，保留正确代码（两边都要就都留）
3. 每个处理完的文件 `git add <路径>`
4. 全部处理完后 `git commit`（merge 提交不用写 message，默认提示即可）
5. 回到主流程继续推送

## 失败场景

| 报错 | 原因 | 处理 |
|------|------|------|
| `denied to ...` / `Could not read from remote repository` | SSH 密钥账号无仓库写权限 | 仓库 Settings → Collaborators 添加账号 Write 权限，邮件接受后重试 |
| `rejected ... non-fast-forward` | pan 有别人新推送的提交未拉 | 回到 Step 2 重跑 `git pull origin pan` |
| `fatal: not a git repository` | 工作目录不对 | `Set-Location d:\msjs` 后重试 |
| `src refspec pan does not match any` | 本地还没 pan 分支 | `git fetch origin` 后 `git switch -c pan --track origin/pan` |

## 常见陷阱

- **不要在 pan 上直接开发**。pan 是集成分支，只通过合并更新。
- `git push -u` 只在首次建立跟踪时用，已跟踪的情况下不要加 `-u`（会打印噪音）。
- PowerShell 里 `git push` 把进度写到 stderr 显示成红色是正常的，退出码 0 就是成功。
- LF→CRLF warning 不影响推送，忽略即可。
- 运行时组件（`mysql-8.0.45-winx64/`、`Redis-7.4.5-Windows-x64-msys2-with-Service/`、`nginx-1.29.1/`、`minio/`）和构建产物已在 `.gitignore` 中，不会被 `git add -A` 收录。

## 成功反馈

推送完成后向用户报告：

- **lzj 新增的**：commit 短哈希 + message（如有自动提交）
- **合并进 pan 的**：`git log --oneline pan@{1}..pan` 列出的提交数 + 最新 commit 哈希
- **最终状态**：`git branch -vv` 两个分支都显示 `...origin/<同名>` 表示同步

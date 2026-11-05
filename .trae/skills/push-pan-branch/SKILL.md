---
name: push-pan-branch
description: 将当前项目所有改动提交并推送到 pan 集成分支。用户说 推送到 pan、提交到 pan、帮我推送 pan 分支、把项目推上去 时触发。不要用于其他分支或其他仓库。
---

# Push Pan Branch

把工作区所有变更（已跟踪的修改 + 未跟踪的新文件，受 `.gitignore` 约束）提交并推送到远端 `origin/pan`。

## 前置检查

1. 工作目录固定为 `d:\msjs`。如果不在该目录，用 `Set-Location d:\msjs` 或在命令里显式指定 `-cwd d:\msjs`。
2. 当前分支必须是 `pan`。执行：
   ```powershell
   git status -sb
   ```
   若开头不是 `## pan`，先 `git switch pan`。
3. 执行 `git fetch origin` 拉取远端最新元数据，再 `git status -sb` 查看是否 `behind` 远端。若有 `[behind N]`，先 `git pull --rebase origin pan` 再继续。
4. 若返回 `working tree clean` 且无 `ahead`，说明没有可推送的内容，直接告知用户"没有需要推送的改动"并结束。

## 提交与推送

按顺序执行以下命令，**所有命令都要关闭沙箱**（`dangerouslyDisableSandbox: true`）以保证 SSH 密钥可用。

```powershell
# 1. 暂存全部改动（.gitignore 中列出的文件不会被加入）
git add -A

# 2. 若要确认改动范围，可选执行：
git diff --cached --stat

# 3. 生成 commit message 并提交。强制 -c core.quotepath=false 避免中文路径被转义。
#    前缀约定：
#      feat:     新功能模块（新增 Entity/Controller/Service/前端页面/SQL 脚本）
#      fix:      Bug 修复
#      refactor: 重构（不改行为）
#      chore:    构建/配置/忽略项/小改动
#    描述用中文，概括本次改动的核心交付物。
git -c core.quotepath=false commit -m "feat: 中文描述本次改动的核心交付物"

# 4. 推送到 origin 的 pan 分支。
git push origin pan 2>&1

# 5. 同步校验。
git fetch origin 2>&1
git status -sb
```

## 推送失败处理

- `denied to ...` 或 `Could not read from remote repository` —— SSH 密钥对当前 GitHub 账号无仓库写权限。提示用户去仓库 Settings → Collaborators 添加对应账号为协作者（Write 权限），并在邮箱里接受邀请。
- `rejected ... non-fast-forward` —— 远端有新提交未在本地。执行 `git pull --rebase origin pan` 后重试 `git push`。
- `src refspec pan does not match any` —— 本地还没有 `pan` 分支。执行 `git fetch origin` 后 `git switch -c pan --track origin/pan`。

## 常见陷阱

- 不要写 `git push -u origin pan` 在 **已经** 有 upstream 的情况下，它会每次都打印 `branch 'pan' set up to track 'origin/pan'`，噪音大。只有首次建立跟踪关系时用 `-u`。
- PowerShell 里 `git push` 会把进度行走到 stderr，显示成红色/错误色是正常现象，只要退出码是 0 就是成功。
- 换行符 LF→CRLF 的 warning 不影响推送，可忽略；若要彻底消除，在仓库根新建 `.gitattributes` 写入 `* text=auto eol=lf`（不要自动加，用户未要求就保持原样）。
- 工作目录下的运行时组件（`mysql-8.0.45-winx64/`、`Redis-7.4.5-Windows-x64-msys2-with-Service/`、`nginx-1.29.1/`、`minio/`）和构建产物已在 `.gitignore` 中，不会被 `git add -A` 收录。

## 成功反馈

推送成功后向用户报告：
- commit 短哈希 + message（从 `git log --oneline -1` 拿到）
- 改动统计（从 `git diff --cached --stat` 或 `git show --stat HEAD` 的摘要行拿到，文件数 + 增删行数）
- 同步状态（`## pan...origin/pan` 表示干净、已同步）

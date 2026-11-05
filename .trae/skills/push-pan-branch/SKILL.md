---
name: push-pan-branch
description: 将当前 lzj 个人分支上的改动合并推送到 pan 集成分支。用户说 推送到 pan、提交到 pan、帮我推送 pan 分支、把项目推上去 时触发。不要用于其他分支或其他仓库。
---

# Push Pan Branch（lzj → pan）

多人协作模式下，开发者在个人分支 `lzj` 上提交，功能完成后合并到集成分支 `pan` 并推送远端。

## 分支模型

```
lzj（个人开发分支，日常提交）  →  合并到 pan（集成分支）  →  合并到 main（发布）
```

- **日常开发**：`git add -A → git commit -m "feat: xxx" → git push`（推到 `origin/lzj`）
- **推送 pan**（本 skill 触发场景）：切到 pan → 拉最新 → 合并 lzj → 推 pan → 切回 lzj

## 前置检查

1. 工作目录固定为 `d:\msjs`。用 `-cwd d:\msjs` 或 `Set-Location d:\msjs`。
2. 当前应该在 `lzj` 分支上。执行：
   ```powershell
   git status -sb
   ```
   若开头不是 `## lzj`，先 `git switch lzj`。
3. 执行 `git fetch origin` 拉远端最新元数据。
4. 确认 `pan` 没有未跟踪的改动（工作区干净才能切换分支）。若有改动，先在 `lzj` 上 `git add -A && git commit` 提交掉。

## lzj 有改动时的提交

合并 pan 之前，先确保 lzj 的改动已提交并推到远端。

```powershell
# 1. 暂存全部改动（.gitignore 中列出的文件不会被加入）
git add -A

# 2. 可选：确认改动范围
git diff --cached --stat

# 3. 生成 commit message 并提交。强制 -c core.quotepath=false 避免中文路径被转义。
#    前缀约定：
#      feat:     新功能模块（新增 Entity/Controller/Service/前端页面/SQL 脚本）
#      fix:      Bug 修复
#      refactor: 重构（不改行为）
#      chore:    构建/配置/忽略项/小改动
#    描述用中文，概括本次改动的核心交付物。
git -c core.quotepath=false commit -m "feat: 中文描述本次改动的核心交付物"

# 4. 推到个人分支
git push origin lzj 2>&1
```

## 合并到 pan 并推送

**所有命令都要关闭沙箱**（`dangerouslyDisableSandbox: true`）以保证 SSH 密钥可用。

```powershell
# 1. 切到 pan
git switch pan

# 2. 拉取远端最新 pan，避免落后导致非 fast-forward 拒绝
git pull origin pan 2>&1

# 3. 合并个人分支 lzj
git merge lzj 2>&1

# 4. 推送到 origin/pan
git push origin pan 2>&1

# 5. 切回个人分支，继续开发
git switch lzj

# 6. 同步校验
git fetch origin 2>&1
git status -sb
git branch -vv
```

## 合并冲突处理

如果 `git merge lzj` 提示冲突：

1. `git status` 查看冲突文件（状态为 `both modified`）
2. 打开冲突文件，手动处理 `<<<<<<< / ======= / >>>>>>>` 标记：
   - `<<<<<<< HEAD` 到 `=======` 之间是 pan（对方或别人的）内容
   - `=======` 到 `>>>>>>> lzj` 之间是你在 lzj 上的内容
   - 删除所有冲突标记，保留正确的代码（两者都要就都留）
3. 处理完一个文件，`git add <冲突文件路径>`
4. 全部处理完后：
   ```powershell
   git commit   # merge 不需要 -m，默认提示信息即可
   git push origin pan
   git switch lzj
   ```
5. 切回 lzj 后执行 `git merge pan` 把 pan 上的合并结果同步回来，避免下次再冲突

## 失败场景处理

| 报错 | 原因 | 处理 |
|------|------|------|
| `denied to ...` / `Could not read from remote repository` | 当前 SSH 密钥账号对仓库无写权限 | 仓库 Settings → Collaborators 添加账号 Write 权限，邮件接受邀请后重试 |
| `rejected ... non-fast-forward` | `origin/pan` 有别人新推送的提交 | `git pull --rebase origin pan` 后重试 `git push` |
| `src refspec pan does not match any` | 本地还没 pan 分支 | `git fetch origin` 后 `git switch -c pan --track origin/pan` |
| `fatal: not a git repository` | 工作目录不对 | `Set-Location d:\msjs` 后重试 |
| 合并冲突 | lzj 和 pan 改了同一处代码 | 按上面"合并冲突处理"流程手动解决 |

## 常见陷阱

- 不要在 pan 分支上直接开发。pan 是集成分支，应该只通过合并更新，避免冲突和覆盖别人的代码。
- 不要写 `git push -u origin pan` 在已跟踪的情况下，它会每次打印 `set up to track` 噪音。只有首次建立跟踪关系时用 `-u`。
- PowerShell 里 `git push` 会把进度行走到 stderr，显示成红色/错误色是正常现象，退出码 0 就是成功。
- 换行符 LF→CRLF 的 warning 不影响推送，可忽略。
- 运行时组件（`mysql-8.0.45-winx64/`、`Redis-7.4.5-Windows-x64-msys2-with-Service/`、`nginx-1.29.1/`、`minio/`）和构建产物已在 `.gitignore` 中，不会被 `git add -A` 收录。

## 成功反馈

推送成功后向用户报告：

- **合并前**：lzj 上新增的 commit 短哈希 + message
- **合并后**：pan 分支最新 commit 短哈希
- **改动统计**：`git log --oneline pan..lzj` 合并进去了几个提交
- **最终状态**：`git branch -vv` 显示两个分支都 `...origin/<同名>` 表示同步

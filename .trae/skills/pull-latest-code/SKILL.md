---
name: pull-latest-code
description: 拉取远端主分支最新代码并重启后端。用户说 拉取最新代码、同步主分支、拉一下代码、更新代码 时触发。固定流程：拉代码 → 停 8081 后端 → mvnw spring-boot:run 增量启动 → 前端 Vite 热更新。
---

# Pull Latest Code（拉代码 + 重启后端）

## 固定流程

```
① 拉取远端最新代码（默认 origin/main）
   ↓
② 停后端进程（按端口 8081 查找并 kill）
   ↓
③ mvnw.cmd spring-boot:run 增量编译启动（跳过 clean/package）
   ↓
④ 前端 Vite 自动热更新，无需操作
```

## 前置检查

1. 工作目录固定 `d:\msjs`，命令加 `-cwd d:\msjs`
2. `git status -sb` 确认在预期分支（默认 `lzj`）
3. 工作区有未提交改动时先告知用户，不要擅自丢弃

## ① 拉取代码

**必须关闭沙箱**（`dangerouslyDisableSandbox: true`）保证 SSH 可用。

```powershell
git fetch origin 2>&1
git merge origin/main --no-edit 2>&1
```

- 冲突时：`git status` 看文件 → 手动处理 `<<<<<<< HEAD / ======= / >>>>>>> origin/main` → `git add <文件>` → `git commit`
- `sql/` 已忽略，不会自动入库；合并带回的已跟踪 SQL 文件不要私自删除（会影响远端其他人）

## ② 停后端（端口 8081）

```powershell
Get-NetTCPConnection -LocalPort 8081 -State Listen -ErrorAction SilentlyContinue |
  Select-Object -ExpandProperty OwningProcess -Unique |
  ForEach-Object { Stop-Process -Id $_ -Force }
```

确认端口已释放：`Get-NetTCPConnection -LocalPort 8081 -ErrorAction SilentlyContinue`

## ③ 启动后端（增量编译）

`mvnw.cmd` 在仓库根目录，`pom.xml` 在 `backend/`，用 `-f` 指定：

```powershell
.\mvnw.cmd -f backend\pom.xml spring-boot:run
```

- 用 `run_in_background: true` 启动，避免长时间占用终端
- **禁止** `clean` / `package`，只做增量编译启动
- 启动后确认 8081 端口已监听、日志无 ERROR

## ④ 前端

Vite 已开启热更新，后端重启期间前端无需重启，代码变更自动生效。
仅当前端 dev server 未运行时才需要 `cd frontend; npm run dev`。

## 常见陷阱

- PowerShell 中 `git` / `mvnw` 输出写到 stderr 显示红色是正常的，退出码 0 即成功
- 旧后端进程未完全退出会导致 8081 端口占用、启动失败，先 kill 再启动
- 端口上没进程时 `Get-NetTCPConnection` 会报错，用 `-ErrorAction SilentlyContinue` 忽略
- 不要用 `mvn clean`，也不要重新 `npm install`，除非用户明确要求

## 成功反馈

向用户报：
- 拉取到的提交数 / 最新 commit 哈希
- 后端是否启动成功（端口 8081 监听状态）

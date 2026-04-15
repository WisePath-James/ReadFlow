# MCP 连接问题快速修复

## 🔧 问题诊断

根据截图显示，MCP 服务器连接失败。可能原因：
1. 环境变量未设置
2. MCP 服务器包未安装
3. Cursor 未重新加载配置

## ⚡ 快速修复步骤

### 步骤 1：设置环境变量（Windows PowerShell）

```powershell
# 临时设置（当前终端有效）
$env:SUPABASE_PROJECT_ID="your-project-id"
$env:SUPABASE_ACCESS_TOKEN="your-access-token"
$env:OPENAI_API_KEY="your-openai-key"

# 验证
echo $env:SUPABASE_PROJECT_ID
```

**永久设置：**
1. 搜索 "环境变量" → 编辑系统环境变量
2. 用户变量 / 系统变量 → 新建
3. 变量名：`SUPABASE_PROJECT_ID`，变量值：你的项目 ID
4. 重复添加其他变量

### 步骤 2：验证 MCP 包安装

```powershell
# 检查是否已安装
npm list -g @supabase/mcp-server
npm list -g @modelcontextprotocol/server-filesystem

# 如果未安装，使用本地方式（无需全局）
# Cursor 会使用 npx 自动下载，但首次可能需要网络
```

### 步骤 3：重启 Cursor

1. 完全退出 Cursor（任务管理器结束进程）
2. 重新打开 Cursor
3. 等待 MCP 服务器自动启动（约 10-30 秒）
4. 检查左侧边栏 MCP 图标是否变绿

### 步骤 4：检查 MCP 日志

在 Cursor 中：
- `Ctrl+Shift+P` → "Show MCP Logs"
- 或查看：`~/.cursor/mcp-logs/`

查看是否有错误信息，如：
- `ENOENT` - 文件路径错误
- `EACCES` - 权限不足
- `ECONNREFUSED` - 连接被拒绝

## 📝 配置文件说明

项目已提供 `.cursor/mcp.json`，内容：

```json
{
  "mcpServers": {
    "supabase": {
      "command": "npx",
      "args": ["-y", "@supabase/mcp-server@latest"],
      "env": {
        "SUPABASE_PROJECT_ID": "${SUPABASE_PROJECT_ID}",
        "SUPABASE_ACCESS_TOKEN": "${SUPABASE_ACCESS_TOKEN}"
      }
    },
    "filesystem": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-filesystem@latest", "E:\\ReadFlow"]
    },
    "sqlite": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-sqlite@latest", "--db-path", "E:\\ReadFlow\\dev.db"]
    },
    "postgres": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-postgres@latest"],
      "env": {
        "POSTGRES_CONNECTION_STRING": "${POSTGRES_CONNECTION_STRING}"
      }
    }
  }
}
```

**关键点：**
- 使用 `npx` 命令自动下载和运行 MCP 服务器
- `@latest` 确保使用最新版本
- 环境变量 `${VAR}` 从系统环境读取

## 🔍 故障排除清单

- [ ] 环境变量已设置（在终端中能 echo 出来）
- [ ] `.cursor/mcp.json` 文件存在且格式正确
- [ ] Cursor 已完全重启
- [ ] 网络连接正常（首次运行需下载 MCP 包）
- [ ] Windows 路径分隔符为 `\\` 或 `/`（已正确）
- [ ] Supabase 项目处于 Active 状态
- [ ] Access Token 具有 `project:admin` 权限

## 🎯 验证 MCP 工作

### 在 Cursor 聊天中测试：

```
使用 Supabase 查询：SELECT current_database();
```

**预期响应：**
```
✅ 查询成功
数据库：your-project-id
```

### 使用 Filesystem：
```
列出 backend 目录下的文件
```

**预期响应：**
```
backend/
├── src/
│   ├── index.ts
│   ├── types/
│   └── routes/
├── package.json
└── tsconfig.json
```

## 🆘 仍然失败？

### 查看详细日志

```powershell
# 查看 Cursor MCP 日志目录
Get-ChildItem "$env:APPDATA\Cursor\logs" -Recurse | Select-String -Pattern "mcp|error" -CaseSensitive
```

### 手动测试 MCP 服务器

```powershell
# 测试 Supabase MCP
npx -y @supabase/mcp-server@latest --help

# 测试 Filesystem MCP
npx -y @modelcontextprotocol/server-filesystem@latest --help
```

如果手动运行成功，说明是 Cursor 配置问题。

### 回退方案

如果 MCP 持续失败，可以：
1. 暂时使用传统方式（终端 + 手动查询）
2. 在 Cursor 中使用普通聊天功能，手动执行数据库操作
3. 等待 Cursor 团队修复 MCP 集成问题

## 📚 相关文档

- [MCP 官方文档](https://modelcontextprotocol.io)
- [Supabase MCP 文档](https://supabase.com/docs/guides/ai/mcp)
- [Cursor MCP 指南](https://cursor.sh/docs/mcp)

---

**最后更新：** 2026-04-15
**适用平台：** Windows PowerShell / Cursor IDE

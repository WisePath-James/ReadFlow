# MCP 服务器配置指南

## 已配置的 MCP 服务器

### 1. Supabase MCP
**包名：** `@supabase/mcp-server`

**功能：**
- 查询数据库（SELECT、INSERT、UPDATE、DELETE）
- 查看表结构（\d、\dt）
- 管理 Row Level Security 策略
- 操作 Storage 文件
- 执行数据库函数

**使用示例：**
```
-- 查看所有表
\d

-- 查询文档
SELECT * FROM documents LIMIT 10;

-- 插入测试文件夹
INSERT INTO folders (id, owner_id, name, color)
VALUES (gen_random_uuid(), 'user-id', '测试', '#3B82F6');
```

**环境变量：**
- `SUPABASE_PROJECT_ID` - myetilelfmmhrbohfwwp
- `SUPABASE_ACCESS_TOKEN` - sbp_0fdad66d008331cf59297e87437279aa5fd486e6

### 2. Filesystem MCP
**包名：** `@modelcontextprotocol/server-filesystem`

**功能：**
- 读取/写入文件
- 列出目录内容
- 创建/删除文件目录
- 文件搜索

**配置路径：** `E:\ReadFlow`（项目根目录）

**使用示例：**
```
# 查看项目结构
ls E:\ReadFlow

# 读取文件
cat E:\ReadFlow\README.md

# 创建新文件
touch E:\ReadFlow\backend\src\services\newService.ts
```

### 3. SQLite MCP
**包名：** `@modelcontextprotocol/server-sqlite`

**功能：**
- 本地 SQLite 数据库操作
- 适合开发调试和快速原型

**数据库路径：** `E:\ReadFlow\dev.db`

**使用示例：**
```
-- 创建测试表
CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);

-- 插入数据
INSERT INTO test (name) VALUES ('ReadFlow');

-- 查询
SELECT * FROM test;
```

### 4. Postgres MCP
**包名：** `@modelcontextprotocol/server-postgres`

**功能：**
- 直接连接 PostgreSQL 数据库
- 执行任意 SQL 查询
- 查看查询计划

**环境变量：**
- `POSTGRES_CONNECTION_STRING` - PostgreSQL 连接字符串

**连接字符串格式：**
```
postgres://postgres:[YOUR-PASSWORD]@db.[PROJECT-REF].supabase.co:5432/postgres
```

**使用示例：**
```
-- 查看所有用户文档
SELECT d.title, d.file_type, d.created_at
FROM documents d
WHERE d.owner_id = 'user-uuid'
ORDER BY d.created_at DESC;

-- 统计文档处理状态
SELECT processing_status, COUNT(*)
FROM documents
GROUP BY processing_status;
```

## MCP 安装与启用

### 方式一：通过 Cursor 内置 MCP（推荐）

1. **打开 Cursor Settings**
   - `Cmd/Ctrl + ,` → 搜索 "MCP"
   - 或访问：`Settings → Extensions → MCP`

2. **添加服务器**
   点击 "Add Server"，选择类型：
   - **Supabase** - 选择预置的 Supabase MCP
   - **Filesystem** - 选择 File System MCP
   - **SQLite** - 选择 SQLite MCP
   - **Postgres** - 选择 PostgreSQL MCP

3. **配置环境变量**
   在 Cursor 的 MCP 设置界面中填入：
   ```
   SUPABASE_PROJECT_ID = your_project_id
   SUPABASE_ACCESS_TOKEN = your_access_token
   POSTGRES_CONNECTION_STRING = your_connection_string
   ```

4. **重启 Cursor**
   关闭并重新打开 Cursor 以使配置生效

### 方式二：手动配置文件（已配置）

项目已包含 `.cursor/mcp.json` 文件，Cursor 启动时会自动加载。

**如果自动加载失败：**
1. 确保文件路径正确：`E:\ReadFlow\.cursor\mcp.json`
2. 重启 Cursor
3. 在 Settings 中检查 MCP 服务器列表

## 验证 MCP 是否正常工作

### 1. 检查 MCP 面板
在 Cursor 界面中：
- 左侧边栏应出现 MCP 图标（方块+箭头）
- 点击可查看已连接的服务器列表
- 绿色圆点��示连接成功，红色表示失败

### 2. 测试查询
在 Cursor 聊天中尝试：
```
使用 Supabase 查询：SELECT COUNT(*) FROM documents;
```

或使用 MCP 专用语法（如果支持）：
```
@supabase SELECT * FROM folders LIMIT 5;
```

### 3. 查看日志
打开 Cursor 开发者工具：
- `Help → Toggle Developer Tools`
- 查看 Console 中的 MCP 连接日志

## 常见问题解决

### Q1: "Connection failed" 或 "Unknown error"

**原因：** 环境变量未设置或 MCP 服务器未安装

**解决：**
```bash
# 1. 检查环境变量
echo $SUPABASE_PROJECT_ID
echo $OPENAI_API_KEY

# 2. 如果没有，设置环境变量（Windows PowerShell）
$env:SUPABASE_PROJECT_ID="your-project-id"
$env:SUPABASE_ACCESS_TOKEN="your-token"

# 永久设置：系统属性 → 环境变量

# 3. 重新启动 Cursor
```

### Q2: "Package not found" 错误

**原因：** MCP 服务器包未安装或包名错误

**解决：**
```bash
# 全局安装（可能需要管理员权限）
npm install -g @supabase/mcp-server
npm install -g @modelcontextprotocol/server-filesystem
npm install -g @modelcontextprotocol/server-sqlite
npm install -g @modelcontextprotocol/server-postgres

# 或使用本地安装（推荐）
cd E:\ReadFlow
npm install --save-dev @supabase/mcp-server @modelcontextprotocol/server-filesystem @modelcontextprotocol/server-sqlite @modelcontextprotocol/server-postgres
```

### Q3: Git MCP 不存在

**说明：** `@modelcontextprotocol/server-git` 包不存在，已从配置中移除。

**替代方案：**
- 使用 Cursor 内置的 Git 功能
- 在终端中使用 Git 命令
- 未来可能有独立的 Git MCP 服务器

### Q4: 权限错误 (EACCES)

**原因：** npm 全局安装权限不足

**解决（Windows）：**
```powershell
# 以管理员身份运行 PowerShell
npm install -g --force @supabase/mcp-server
```

**解决（macOS/Linux）：**
```bash
# 修复 npm 权限
sudo chown -R $(whoami) $(npm config get prefix)/{lib,node_modules,bin}
```

### Q5: Supabase MCP 无法连接数据库

**检查清单：**
- [ ] Supabase 项目是否已创建
- [ ] 是否生成了 Access Token（需 `project:admin` 权限）
- [ ] 连接字符串是否正确
- [ ] 数据库是否启用（Supabase 项目状态为 "Active"）

**获取 Access Token：**
1. 登录 Supabase Dashboard
2. 点击右上角头像 → "Access Tokens"
3. 生成新 Token（选择 `project:admin` 角色）
4. 复制并保存到 `.env` 文件

## 生产环境建议

### 安全性
- ❌ **不要** 将 `SUPABASE_ACCESS_TOKEN` 提交到 Git
- ✅ **必须** 在 `.gitignore` 中包含 `.env`
- ✅ **建议** 使用服务角色密钥仅用于后端，MCP 使用受限令牌

### 性能
- ✅ 使用本地 SQLite 进行快速原型开发
- ✅ 生产环境切换到 Postgres MCP 或直接连接 Supabase

### 可维护性
- ✅ 将 MCP 配置纳入版本控制（`.cursor/mcp.json`）
- ✅ 环境变量使用 `${VAR}` 占位符，不硬编码敏感信息
- ✅ 团队成员共享相同的 MCP 配置

## 快速测试命令

复制以下命令到 Cursor 聊天中测试每个 MCP：

```
# 测试 Filesystem
列出项目根目录文件

# 测试 Supabase（需配置环境变量）
查询数据库：SELECT version();

# 测试 SQLite
SQLite 查询：SELECT sqlite_version();

# 测试 Postgres（需连接字符串）
Postgres 查询：SELECT current_database();
```

---

**配置完成？** 运行 `INITIALIZATION_CHECKLIST.md` 中的验证步骤，或直接开始开发！

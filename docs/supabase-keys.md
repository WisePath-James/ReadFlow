# Supabase 密钥完全指南

## 🔐 密钥类型对比

| 密钥类型 | 变量名 | 用途 | 权限 | 安全性 | 位置 |
|---------|--------|------|------|--------|------|
| **Anon Key** | `SUPABASE_ANON_KEY` | 客户端（iOS/Web） | 受 RLS 限制 | ✅ 可公开 | API 设置 |
| **Service Role** | `SUPABASE_SERVICE_ROLE_KEY` | 后端服务器 | 完全权限 | 🔒 **绝密** | API 设置 |
| **Personal Access Token** | `SUPABASE_ACCESS_TOKEN` | 管理操作（MCP/CLI） | 基于用户角色 | 🔒 保密 | 用户设置 |

---

## 📍 在哪里找到这些密钥

### 1. Anon Key（客户端用）

**路径：**
```
Supabase Dashboard → Project Settings (齿轮图标) → API → "anon public"
```

**用途：**
- iOS 应用直接连接数据库（受 RLS 保护）
- 读取公开数据
- 写入用户自己的数据

**示例值：**
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVz...
```

### 2. Service Role Key（服务���用）

**路径：**
```
Supabase Dashboard → Project Settings (齿轮图标) → API → "service_role secret"
```

**用途：**
- 后端服务器（Node.js）
- 绕过 RLS 策略
- 执行管理操作（如批量导入、清理）
- **绝对不能暴露给客户端！**

**示例值：**
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNl...
```

### 3. Personal Access Token（MCP/CLI 用）

**路径：**
```
Supabase Dashboard → User Settings (右上角头像) → Access Tokens
```

**创建步骤：**
1. 点击 "Generate new token"
2. 输入名称（如 "ReadFlow MCP"）
3. **选择角色：** `project:admin`（管理员）或 `project:read-only`（只读）
4. 点击 "Generate"
5. **复制令牌（只显示一次！）**

**用途：**
- Supabase CLI 操作
- MCP 服务器连接
- 数据库迁移
- 管理脚本

**示例值：**
```
sbp_1234567890abcdefghijklmnopqrstuv
```

---

## 🎯 ReadFlow 项目中的使用

### **后端服务（backend/src/index.ts）**

```typescript
import { createClient } from '@supabase/supabase-js';

// 客户端连接（用于用户操作，受 RLS 保护）
export const supabase = createClient(
  process.env.SUPABASE_URL!,
  process.env.SUPABASE_ANON_KEY!  // ← 使用 anon key
);

// 管理员连接（用于后台任务，绕过 RLS）
export const supabaseAdmin = createClient(
  process.env.SUPABASE_URL!,
  process.env.SUPABASE_SERVICE_ROLE_KEY!  // ← 使用 service_role key
);
```

### **MCP 服务器（.cursor/mcp.json）**

```json
{
  "mcpServers": {
    "supabase": {
      "command": "npx",
      "args": ["-y", "@supabase/mcp-server@latest"],
      "env": {
        "SUPABASE_PROJECT_ID": "${SUPABASE_PROJECT_ID}",
        "SUPABASE_ACCESS_TOKEN": "${SUPABASE_ACCESS_TOKEN}"  // ← 使用 Personal Access Token
      }
    }
  }
}
```

### **Supabase CLI**

```bash
# 登录（使用 Personal Access Token）
supabase login --token sbp_xxx...

# 链接项目
supabase link --project-ref your-project-id
```

---

## 📝 正确的 .env 配置

```bash
# ========== Supabase 项目信息 ==========
SUPABASE_PROJECT_ID=abc123def456           # 从 Settings → General 获取
SUPABASE_URL=https://abc123def456.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIs...  # 公开的客户端密钥
SUPABASE_SERVICE_ROLE_KEY=eyJhbGciOiJI...  # 服务端密钥（保密！）

# ========== 个人访问令牌（用于 MCP/CLI）==========
SUPABASE_ACCESS_TOKEN=sbp_1234567890abc...  # 从 User Settings → Access Tokens 生成

# ========== OpenAI ==========
OPENAI_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxx

# ========== 数据库连接 ==========
POSTGRES_CONNECTION_STRING=postgres://postgres:password@db.abc123def456.supabase.co:5432/postgres

# ========== 其他配置 ==========
PORT=3000
NODE_ENV=development
JWT_SECRET=your_jwt_secret_min_32_chars
```

---

## 🔒 安全性最佳实践

### ✅ **必须做的：**

1. **`.gitignore` 包含 `.env`**
   ```gitignore
   .env
   .env.local
   .env.*.local
   ```

2. **Service Role Key 只在后端使用**
   - 永远不要放在 iOS 客户端
   - 不要提交到 Git
   - 使用环境变量注入

3. **Personal Access Token 谨慎授权**
   - 为 MCP 创建专用令牌
   - 权限设为 `project:admin`（仅需管理员操作时）
   - 定期轮换令牌

4. **Anon Key 可公开**
   - 可以放在 iOS 客户端代码中
   - 但需配置严格的 RLS 策略

### ❌ **绝对禁止的：**

1. **不要** 在代码中硬编码密钥
   ```typescript
   // ❌ 错误
   const supabase = createClient('https://xxx.supabase.co', 'eyJhbG...');
   ```

2. **不要** 提交密钥到 Git
   ```bash
   # ❌ 错误
   git add .env
   git commit -m "添加配置"
   ```

3. **不要** 在客户端使用 Service Role Key
   ```swift
   // ❌ 错误（iOS 代码中）
   let supabase = SupabaseClient(superbaseURL: "...", serviceRoleKey: "...")
   ```

4. **不要** 共享 Personal Access Token
   - 每个开发者使用自己的 PAT
   - 不要在团队聊天中发送

---

## 🎯 快速检查清单

- [ ] **已创建 Supabase 项目**
- [ ] **已获取 Project ID**
- [ ] **已复制 Anon Key**（用于后端）
- [ ] **已复制 Service Role Key**（用于后端管理操作）
- [ ] **已生成 Personal Access Token**（用于 MCP/CLI）
- [ ] **已配置 .env 文件**（包含所有密钥）
- [ ] **已验证 .gitignore 包含 .env**
- [ ] **已设置环境变量**（PowerShell: `$env:VAR=value`）

---

## 🔧 常见问题

### Q: MCP 连接失败，提示 "Unauthorized"？
**A:** 检查 `SUPABASE_ACCESS_TOKEN` 是否正确：
1. 是否使用的是 **Personal Access Token**（以 `sbp_` 开头）
2. 令牌角色是否为 `project:admin`
3. 是否已复制完整的令牌（注意不要截断）

### Q: 后端连接数据库失败？
**A:** 检查 `SUPABASE_ANON_KEY`：
1. 是否复制了 `anon public` 密钥（不是 `service_role`）
2. RLS 策略是否已启用并配置正确

### Q: 应该用哪个密钥？
**A:** 根据使用场景：
- **iOS 客户端** → `SUPABASE_ANON_KEY`（公开）
- **后端 API** → `SUPABASE_ANON_KEY`（正常操作）+ `SUPABASE_SERVICE_ROLE_KEY`（管理员操作）
- **MCP/CLI** → `SUPABASE_ACCESS_TOKEN`（Personal Access Token）
- **数据库迁移** → `SUPABASE_ACCESS_TOKEN` 或 `SUPABASE_SERVICE_ROLE_KEY`

### Q: 密钥在哪里可以重新生成？
**A:**
- **Anon Key / Service Role Key**：Project Settings → API → "Reset"（注意：重置后所有客户端需要更新）
- **Personal Access Token**：User Settings → Access Tokens → 创建新令牌（旧令牌自动失效）

---

## 📚 相关文档

- [Supabase 认证文档](https://supabase.com/docs/guides/auth)
- [RLS 策略指南](https://supabase.com/docs/guides/auth/row-level-security)
- [MCP 配置说明](docs/mcp-setup.md)
- [开发指南](docs/development.md)

---

**重要提醒：**
- `SUPABASE_ACCESS_TOKEN` ≠ `SUPABASE_SERVICE_ROLE_KEY`
- MCP 使用 **Personal Access Token**（`sbp_...` 开头）
- 后端使用 **Anon Key** 和 **Service Role Key**

请确认您已正确获取这三类密钥，并更新 `.env` 文件。

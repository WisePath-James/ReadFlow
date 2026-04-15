# ReadFlow MCP 与项目初始化清单

## ✅ 已完成的配置

### 1. MCP 服务器配置
- [x] `supabase` - Supabase 数据库与存储管理
- [x] `filesystem` - 文件系统操作
- [x] `git` - Git 版本控制
- [x] `sqlite` - 本地开发数据库
- [x] `postgres` - PostgreSQL 查询

配置文件：`.cursor/mcp.json`

### 2. 项目基础结构
- [x] 根目录结构（docs, .github, scripts, supabase, ios, backend）
- [x] `.gitignore` - Git 忽略规则
- [x] `.env.example` - 环境变量模板
- [x] `README.md` - 项目说明文档

### 3. 后端服务
- [x] `backend/package.json` - 依赖配置
- [x] `backend/tsconfig.json` - TypeScript 配置
- [x] `backend/src/index.ts` - 应用入口
- [x] `backend/src/types/index.ts` - 类型定义
- [x] API 路由：
  - [x] `documents.ts` - 文档管理
  - [x] `ai.ts` - AI 问答服务
  - [x] `annotations.ts` - 标注管理
  - [x] `notes.ts` - 笔记管理
  - [x] `handwriting.ts` - 手写批注
  - [x] `search.ts` - 全文搜索
  - [x] `progress.ts` - 阅读进度

### 4. 数据库设计（Supabase）
- [x] 初始迁移：`20240101000000_initial_schema.sql`
  - 10 个核心表
  - Row Level Security 策略
  - 索引优化
- [x] 函数迁移：`20240101000001_functions.sql`
  - 全文搜索函数
  - 向量相似度搜索
  - 触发器

### 5. iOS 客户端
- [x] `ReadFlowApp.swift` - 应用入口
- [x] `Models.swift` - 完整数据模型
- [x] `ContentView.swift` - 主布局（iPhone/iPad 适配）

### 6. 开发文档
- [x] `docs/development.md` - 完整开发指南
- [x] API 文档
- [x] 常见任务说明

### 7. 开发脚本
- [x] `scripts/setup.sh` - 项目初始化脚本

---

## 📋 待完成配置清单

### 环境变量设置（必需）
- [ ] **Supabase 配置**
  - [ ] 在 Supabase 创建项目
  - [ ] 获取 Project ID
  - [ ] 生成 Access Token
  - [ ] 获取 URL 和 Anon Key

- [ ] **OpenAI 配置**
  - [ ] 获取 API Key
  - [ ] 设置组织 ID（可选）

- [ ] **Apple 开发者配置**
  - [ ] Team ID
  - [ ] Bundle ID

### 数据库初始化
- [ ] 安装 Supabase CLI
  ```bash
  npm install -g supabase
  ```
- [ ] 登录 Supabase
  ```bash
  supabase login
  ```
- [ ] 链接项目
  ```bash
  supabase link --project-ref YOUR_PROJECT_ID
  ```
- [ ] 执行数据库迁移
  ```bash
  supabase db push
  ```
- [ ] 验证函数创建
  ```sql
  SELECT * FROM pg_policies WHERE schemaname = 'public';
  ```

### 后端启动
- [ ] 安装依赖
  ```bash
  cd backend && npm install
  ```
- [ ] 运行类型生成
  ```bash
  npm run gen:types
  ```
- [ ] 启动开发服务器
  ```bash
  npm run dev
  ```
- [ ] 验证健康检查
  ```
  GET http://localhost:3000/health
  ```

### iOS 开发环境
- [ ] 安装 Xcode（15.0+）
- [ ] 打开项目
  ```bash
  cd ios
  pod install
  open ReadFlow.xcworkspace
  ```
- [ ] 配置签名（Apple 开发者账号）
- [ ] 编译运行（模拟器或真机）

### 功能测试清单
- [ ] 文档上传
- [ ] Library 列表显示
- [ ] PDF 阅读（原页模式）
- [ ] PDF 阅读（Reflow 模式）
- [ ] 文本选择与高亮
- [ ] 创建笔记
- [ ] AI 快答（流式）
- [ ] 翻译功能
- [ ] 总结提炼
- [ ] 深度分析（向量搜���）
- [ ] 归档功能
- [ ] 阅读进度保存
- [ ] iPad 分栏布局
- [ ] Apple Pencil 批注（仅 iPad）

### 性能优化
- [ ] 启用 Redis 缓存
- [ ] 配置文档处理队列（BullMQ）
- [ ] PDF 预加载优化
- [ ] ��片缩略图缓存

### 部署准备
- [ ] 后端生产配置
- [ ] 数据库备份策略
- [ ] 文件存储策略（Supabase Storage）
- [ ] iOS App Store 配置
- [ ] CI/CD 流水线（可选）

---

## 🔧 MCP 服务器使用说明

### 已配置的 MCP 能力

#### 1. Supabase MCP
**功能：** 数据库查询、表结构查看、存储管理

**使用示例：**
```
查询用户文档：
SELECT * FROM documents WHERE owner_id = 'user-id';

查看表结构：
\d documents

插入测试数据：
INSERT INTO folders (name, color) VALUES ('测试文件夹', '#3B82F6');
```

#### 2. Filesystem MCP
**功能：** 文件系统操作、文件读写

**使用示例：**
```
列出项目文件：
ls -la

读取配置文件：
cat .env.example

创建新文件：
touch backend/src/services/newService.ts
```

#### 3. Git MCP
**功能：** Git 操作、提交、分支管理

#### 4. SQLite MCP
**功能：** 本地轻量数据库（开发调试用）

#### 5. Postgres MCP
**功能：** 直接 PostgreSQL 查询

---

## 🚀 快速启动命令

### Windows PowerShell
```powershell
# 1. 环境配置
cp .env.example .env
notepad .env  # 编辑配置

# 2. 后端
cd backend
npm install
npm run dev

# 3. iOS（在新终端）
cd ios
pod install
```

### macOS / Linux
```bash
# 一键初始化
./scripts/setup.sh

# 启动后端
cd backend && npm run dev

# 打开 iOS 项目
cd ios && pod install && open ReadFlow.xcworkspace
```

---

## 📊 项目进度

| 模块 | 进度 | 说明 |
|------|------|------|
| 后端 API | 70% | 核心路由已完成，需完善业务逻辑 |
| 数据库设计 | 90% | 表结构完成，需补充索引优化 |
| iOS 客户端 | 20% | 基础架构完成，需实现具体视图 |
| AI 集成 | 40% | API 路由完成，需优化 prompt |
| Apple Pencil | 0% | 待实现 |
| 向量检索 | 70% | 函数完成，需测试 |
| 文档处理 | 0% | 待实现（PDF 解析等） |
| 部署配置 | 0% | 待完成 |

---

## 🎯 下一步优先事项

1. **立即执行**
   - [ ] 创建 Supabase 项目
   - [ ] 配置环境变量
   - [ ] 执行数据库迁移

2. **本周目标**
   - [ ] 完成后端基础服务启动
   - [ ] 实现文档上传处理流水线
   - [ ] 实现 PDF 文本提取
   - [ ] 实现 chunk 切分与 embedding 生成

3. **下阶段**
   - [ ] iOS Library 页面完整实现
   - [ ] PDFKit 阅读器集成
   - [ ] 文本选择与自定义菜单
   - [ ] AI 快答前端集成

---

## 📚 参考资源

- [Supabase 文档](https://supabase.com/docs)
- [OpenAI API 文档](https://platform.openai.com/docs)
- [PDFKit 指南](https://developer.apple.com/documentation/pdfkit)
- [PencilKit 文档](https://developer.apple.com/documentation/pencilkit)
- [SwiftUI 教程](https://developer.apple.com/tutorials/swiftui)

---

**最后更新：** 2024-01-01
**维护者：** ReadFlow 开发团队

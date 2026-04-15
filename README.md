# ReadFlow - AI 文档阅读与问答系统

## 项目概述

ReadFlow 是一个原生 iOS 文档阅读与 AI 问答系统，集成 Library 分类管理、重型文档阅读、原生文本选区、自定义标注、Apple Pencil 手写批注、局部 AI 快答、线程化追问、全文深度分析、知识归档等功能。

### 核心特性

- 📚 **Library 首页** - 文档容器、文件夹分类、上传入口、搜索
- 📄 **多格式支持** - PDF、DOCX、EPUB、TXT、Markdown、HTML
- ✏️ **完整标注** - 高亮、下划线、删除线、笔记
- 🖊️ **Apple Pencil 批注** - Noteability 级手写体验
- 🤖 **AI 快答** - 选中文本即时问答
- 🔍 **深度分析** - 全文预处理、向量检索、带引用答案
- 💾 **进度保存** - 阅读状态自动保存与恢复
- 📱 **双端适配** - iPhone 专注阅读、iPad 大屏并行

## 技术栈

### 前端 (iOS)
- **SwiftUI** - 主界面框架
- **UIKit** - 高级菜单与交互桥接
- **PDFKit** - PDF 阅读核心
- **PencilKit** - Apple Pencil 手写批注
- **Combine** - 响应式状态管理

### 后端
- **Supabase** - PostgreSQL + 向量存储 + 身份认证
- **OpenAI API** - AI 问答与 Embedding
- **Node.js** - 文档处理服务
- **pgvector** - 向量相似度搜索

## 项目结构

```
ReadFlow/
├── ios/                    # iOS 客户端工程
│   ├── ReadFlow/          # 主 App Target
│   │   ├── App/          # 应用入口
│   │   ├── Views/        # SwiftUI 视图
│   │   ├── Models/       # 数据模型
│   │   ├── ViewModels/   # 状态管理
│   │   ├── Services/     # 业务服务
│   │   └── Components/   # 可复用组件
│   └── ReadFlowTests/    # 单元测试
├── backend/               # 后端服务
│   ├── src/
│   │   ├── controllers/  # API 控制器
│   │   ├── services/     # 业务逻辑
│   │   ├── middleware/   # 中间件
│   │   ├── utils/        # 工具函数
│   │   └── types/        # TypeScript 类型
│   ├── package.json
│   └── tsconfig.json
├── supabase/              # Supabase 配置
│   ├── migrations/       # 数据库迁移
│   ├── functions/        # Edge Functions
│   └── storage/         # 文件存储规则
├── docs/                 # 项目文档
│   ├── architecture.md   # 架构文档
│   ├── api.md           # API 规范
│   └── development.md   # 开发指南
├── scripts/              # 开发脚本
│   ├── setup.sh
│   ├── dev.sh
│   └── deploy.sh
└── .cursor/             # Cursor IDE 配置
    ├── mcp.json         # MCP 服务器配置
    └── settings.json    # Cursor 设置
```

## 快速开始

### 前置要求
- Node.js 18+
- Xcode 15+ (iOS 17+ SDK)
- Supabase CLI
- Cursor IDE (推荐)

### 1. 环境配置
```bash
cp .env.example .env
# 编辑 .env 文件，填入以下配置：
# - Supabase Project ID 和 Access Token
# - OpenAI API Key
# - Apple 开发者账号信息
```

### 2. 安装依赖
```bash
# 后端依赖
cd backend && npm install

# iOS 依赖
cd ios && pod install
```

### 3. 数据库初始化
```bash
# 登录 Supabase
supabase login

# 链接项目
supabase link --project-ref your-project-id

# 执行迁移
supabase db push
```

### 4. 启动开发环境
```bash
# 启动后端服务
cd backend && npm run dev

# 打开 iOS 项目
open ios/ReadFlow.xcworkspace
```

## 核心数据模型

### 文档结构
- **document** - 文档元信息
- **document_pages** - 页级文本
- **document_blocks** - 结构化块
- **document_chunks** - AI 检索块（带 embedding）

### 标注系统
- **annotations** - 高亮/下划线/删除线
- **notes** - 文本笔记
- **handwriting_annotations** - 手写批注
- **archives** - 知识归档

### 阅读进度
- **reading_progress** - 用户阅读状态

## 开发规范

### 代码风格
- Swift: 遵循 [Swift API Design Guidelines](https://swift.org/documentation/api-design-guidelines/)
- TypeScript: ESLint + Prettier
- 提交前运行 `npm run lint`

### 分支策略
- `main` - 生产版本
- `develop` - 开发版本
- `feature/*` - 功能分支
- `bugfix/*` - 修复分支

### 提交规范
```
feat: 添加 AI 快答功能
fix: 修复 PDF 页面跳转问题
docs: 更新架构文档
refactor: 重构标注服务
test: 添加单元测试
```

## MCP 服务器配置

本项目使用 Cursor MCP 插件提供开发辅助：

- **supabase** - 数据库查询与管理
- **filesystem** - 文件系统操作
- **git** - Git 版本控制
- **sqlite** - 本地开发数据库
- **postgres** - PostgreSQL 查询

详细配置见 `.cursor/mcp.json`

## 部署

### 后端部署
```bash
cd backend
npm run build
npm start
```

### iOS 打包
```bash
cd ios
xcodebuild -workspace ReadFlow.xcworkspace \
  -scheme ReadFlow \
  -configuration Release \
  -archivePath ./build/ReadFlow.xcarchive archive
```

## 贡献指南

请阅读 [CONTRIBUTING.md](docs/CONTRIBUTING.md) 了解如何贡献代码。

## 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件

---

**ReadFlow** - 重新定义文档阅读体验

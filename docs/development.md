# 开发指南

## 快速开始

### 1. 环境准备
```bash
# 克隆项目
git clone <repository-url>
cd ReadFlow

# 安装依赖
npm install

# 复制环境配置
cp .env.example .env

# 编辑 .env 文件，填入真实配置
# - Supabase 项目信息
# - OpenAI API Key
# - Apple 开发者账号
```

### 2. 数据库初始化
```bash
# 安装 Supabase CLI
npm install -g supabase

# 登录
supabase login

# 链接项目
supabase link --project-ref YOUR_PROJECT_ID

# 执行迁移
supabase db push
```

### 3. 启动开发环境

**后端服务：**
```bash
cd backend
npm run dev
# 服务运行在 http://localhost:3000
```

**iOS 开发：**
```bash
cd ios
pod install
open ReadFlow.xcworkspace
```

## 项目结构

```
ReadFlow/
├── ios/                      # iOS 客户端
│   └── ReadFlow/
│       ├── App/             # App 入口与配置
│       ├── Models/          # 数据模型
│       ├── Views/           # SwiftUI 视图
│       ├── ViewModels/      # 状态管理
│       ├── Services/        # 业务服务
│       └── Components/      # 可复用组件
├── backend/                  # 后端 API 服务
│   ├── src/
│   │   ├── routes/         # API 路由
│   │   ├── types/          # TypeScript 类型
│   │   ├── services/       # 业务逻辑
│   │   └── utils/          # 工具函数
│   └── package.json
├── supabase/                # Supabase 配置
│   ├── migrations/         # 数据库迁移
│   ├── functions/          # Edge Functions
│   └── storage/            # 文件存储规则
├── docs/                   # 项目文档
└── scripts/                # 开发脚本
```

## API 文档

### 文档管理
- `GET    /api/documents` - 获取文档列表
- `GET    /api/documents/:id` - 获取文档详情
- `POST   /api/documents` - 上传文档
- `DELETE /api/documents/:id` - 删除文档
- `PATCH  /api/documents/:id` - 更新文档元信息

### AI 服务
- `POST /api/ai/quick-ask` - 快速问答（局部上下文）
- `POST /api/ai/translate` - 翻译选中文本
- `POST /api/ai/summarize` - 总结提炼
- `POST /api/ai/deep-analysis` - 深度分析（全文检索）
- `POST /api/ai/archive` - 保存归档

### 标注系统
- `POST   /api/annotations` - 创建标注
- `GET    /api/annotations/document/:id` - 获取文档标注
- `DELETE /api/annotations/:id` - 删除标注

### 笔记系统
- `POST   /api/notes` - 创建笔记
- `GET    /api/notes/document/:id` - 获取文档笔记
- `PATCH  /api/notes/:id` - 更新笔记
- `DELETE /api/notes/:id` - 删除笔记

### 手写批注（仅 iPad）
- `POST   /api/handwriting` - 创建手写批注
- `GET    /api/handwriting/document/:id` - 获取手写批注
- `PATCH  /api/handwriting/:id` - 更新手写批注
- `DELETE /api/handwriting/:id` - 删除手写批注

### 搜索
- `POST /api/search` - 全文搜索（关键词+语义）
- `GET  /api/search/document/:id` - 文档内搜索

### 阅读进度
- `POST   /api/progress` - 保存阅读进度
- `GET    /api/progress/document/:id` - 获取阅读进度
- `GET    /api/progress/recent` - 最近阅读列表

## 开发规范

### 代码风格
- Swift: 遵循 Apple 官方规范
- TypeScript: ESLint + Prettier
- 提交前运行 `npm run lint`

### Git 工作流
```
main (生产)
develop (开发)
feature/xxx (功能分支)
bugfix/xxx (��复分支)
```

### 提交信息格式
```
feat: 添加新功能
fix: 修复问题
docs: 文档更新
refactor: 代码重构
test: 测试相关
chore: 构建/工具链
```

## 常见任务

### 添加新的 API 端点
1. 在 `backend/src/routes/` 创建路由文件或扩展现有文件
2. 在 `backend/src/types/index.ts` 添加类型定义
3. 更新 API 文档

### 添加新的 iOS 视图
1. 在 `ios/ReadFlow/Views/` 创建 SwiftUI 视图
2. 在 `ContentView.swift` 中集成
3. 通过 `environmentObject` 注入依赖

### 数据库迁移
```bash
# 创建新迁移文件
supabase migration new migration_name

# 编辑迁移 SQL
# supabase/migrations/xxx_xxx.sql

# 执行迁移
supabase db push
```

### 生成 TypeScript 类型
```bash
cd backend
npm run gen:types
# Supabase 类型将生成到 supabase/types.ts
```

## 调试技巧

### 后端日志
```bash
# 查看开发日志
npm run dev

# 生产环境
pm2 logs readflow-backend
```

### iOS 调试
- 使用 Xcode Debugger
- 打印日志: `print("debug: \(value)")`
- 网络请求: 使用 Charles Proxy 或 Proxyman

### 数据库查询
```sql
-- 查看处理中的文档
select id, title, processing_status from documents
where processing_status = 'processing';

-- 查看用户文档统计
select * from get_user_document_stats('user-uuid-here');
```

## 性能优化建议

### 后端
- 使用 Redis 缓存热门查询
- 实现文档处理队列（BullMQ）
- 启用数据库连接池

### iOS
- 使用 `LazyVStack` 加载长列表
- PDF 分页预加载
- 图片缩略图缓存

## 故障排除

### 问题：文档上传后一直处于 "processing" 状态
**解决：** 检查 `backend/src/services/documentProcessor.ts` 中的文档处理逻辑，查看日志错误。

### 问题：向量搜索返回结果为空
**解决：** 确认文档已处理完成且 `document_chunks` 表有数据。运行 `SELECT COUNT(*) FROM document_chunks;`

### 问题：iPad 布局异常
**解决：** 检查 `ContentView.swift` 中的 `NavigationSplitView` 配置和 `horizontalSizeClass` 判断。

---

**需要帮助？** 请查阅 [项目文档](./docs/) 或提交 Issue。

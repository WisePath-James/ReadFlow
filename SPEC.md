# ReadFlow - AI 文档阅读与问答系统

## 项目定位

**原生 Android 文档阅读与 AI 问答系统**

核心定位：
- Library 分类管理
- 重型文档阅读（PDF/DOCX/EPUB/TXT/Markdown/HTML）
- 原生文本选区与自定义标注
- Android 平板手写笔批注
- 局部 AI 快答 + 线程化追问
- 全文深度分析（预处理知识库）
- 知识归档（Archive）
- 阅读进度保存与回跳原文

---

## 技术架构

### 四层架构

```
┌─────────────────────────────────────────────────────────────┐
│                    第一层：原生 Android 客户端               │
│  Kotlin + Jetpack Compose + 原生 PDF 渲染 + 手写笔层         │
├─────────────────────────────────────────────────────────────┤
│                    第二层：文档处理服务                        │
│  格式解析 → Canonical Document Model → Chunk → Embedding     │
├─────────────────────────────────────────────────────────────┤
│                    第三层：AI 检索与问答服务                   │
│  Quick Ask Engine + Deep Analysis Engine + 流式输出         │
├─────────────────────────────────────────────────────────────┤
│                    第四层：数据与缓存层                       │
│  Supabase/PostgreSQL + Vector Store + TTL Cache             │
└─────────────────────────────────────────────────────────────┘
```

---

## 支持文档类型

| 类型 | 格式 |
|------|------|
| 固定版式 | PDF |
| 富文本/电子书 | DOC, DOCX, RTF, ODT, EPUB |
| 纯文本/标记 | TXT, Markdown, HTML, XML, JSON |

---

## 核心功能模块

### 1. Library 首页
- 文档总览
- 文件夹系统（一级/多级）
- 最近阅读
- 最近上传
- 收藏/Pin
- 上传入口
- 全文搜索

### 2. 阅读器系统
- 原生 PDF 渲染
- 连续滚动 / 单页模式
- Fit Width / Fit Page
- 文本选择与复制
- 文档内搜索
- 目录导航
- 缩略图导航
- 双阅读模式：原页模式 + Reflow 重排模式

### 3. 文本选择与动作系统
第一层选区动作：
- Highlight（高亮）
- Underline（下划线）
- Strikeout（删除线）
- Note（笔记）
- AI 翻译
- AI 快答
- AI 总结提炼
- 复制
- 分享

### 4. 标注与笔记系统
- 多颜色高亮
- 下划线/删除线
- Note 可编辑/删除
- 侧边栏标注列表
- 点击跳回原文

### 5. AI Quick Ask Engine
- 局部上下文（当前页 ± 2页）
- 流式输出
- 线程隔离（防串问）
- 追问支持
- 20分钟 TTL 过期

### 6. Deep Analysis Engine
- 上传即预处理
- Semantic Search + Keyword Search
- 带页码引用答案
- 图表页图像分析

### 7. Archive 知识卡片
- quote + question + answer + tag
- 按文档聚合
- 点击跳回原文
- 不保留追问链

### 8. 阅读进度保存
- 当前页/虚拟页
- 滚动偏移
- 缩放级别
- 阅读模式
- 主题
- 手写批注层状态

### 9. Android 平板适配
- 多栏布局（双栏/三栏）
- 侧边栏常驻
- AI 面板并置
- 手写笔工具区

### 10. 手写笔批注系统
- 自由手写
- 荧光笔/圈画
- 橡皮擦/撤销/重做
- 手写内容与文档锚点关联
- 手写转文字/AI 分析

---

## 数据库表结构

### folders
id, owner_id, parent_folder_id, name, color, icon, created_at, updated_at

### documents
id, owner_id, folder_id, title, file_path, file_type, page_count, outline_json, processing_status, created_at, updated_at

### document_pages
id, document_id, page_index, extracted_text, text_confidence, thumbnail_path, created_at

### document_chunks
id, document_id, page_start, page_end, chapter_title, chunk_text, embedding, created_at

### annotations
id, document_id, page_index, type, color, quote, anchor_meta, created_at, updated_at

### notes
id, document_id, page_index, quote, anchor_meta, note_text, created_at, updated_at

### handwriting_annotations
id, document_id, page_index, drawing_data, anchor_meta, tool_type, color, width, created_at, updated_at

### archives
id, document_id, page_index, quote, anchor_meta, question, answer, tag, created_at

### reading_progress
user_id, document_id, current_page, scroll_offset, zoom_level, reading_mode, theme, last_read_at, updated_at

---

## 性能目标

- 首屏打开不阻塞在全文 AI 处理后
- 仅渲染当前必要视区
- 快答首 token 尽可能快
- 流式输出
- 大文档滚动稳定

---

## 主题支持

- 浅色主题
- 深色主题
- 护眼暖色主题

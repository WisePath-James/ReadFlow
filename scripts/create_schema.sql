-- ============================================
-- ReadFlow 数据库 Schema
-- 基于 backend/src/types/index.ts
-- 创建日期: 2026-04-16
-- ============================================

-- 启用扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "vector";

-- ============================================
-- 1. folders - 文件夹表
-- ============================================
CREATE TABLE IF NOT EXISTS folders (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  owner_id UUID NOT NULL,
  parent_folder_id UUID REFERENCES folders(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  color TEXT DEFAULT '#3B82F6',
  icon TEXT DEFAULT 'folder',
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_folders_owner ON folders(owner_id);
CREATE INDEX IF NOT EXISTS idx_folders_parent ON folders(parent_folder_id);

-- ============================================
-- 2. documents - 文档表
-- ============================================
CREATE TABLE IF NOT EXISTS documents (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  owner_id UUID NOT NULL,
  folder_id UUID REFERENCES folders(id) ON DELETE SET NULL,
  title TEXT NOT NULL,
  file_path TEXT NOT NULL,
  file_type TEXT NOT NULL,
  page_count_or_virtual_page_count INTEGER DEFAULT 0,
  outline_json JSONB DEFAULT '{}',
  processing_status TEXT DEFAULT 'pending' CHECK (processing_status IN ('pending', 'processing', 'completed', 'failed')),
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_documents_owner ON documents(owner_id);
CREATE INDEX IF NOT EXISTS idx_documents_folder ON documents(folder_id);
CREATE INDEX IF NOT EXISTS idx_documents_status ON documents(processing_status);
CREATE INDEX IF NOT EXISTS idx_documents_created ON documents(created_at DESC);

-- ============================================
-- 3. document_pages - 文档页面表
-- ============================================
CREATE TABLE IF NOT EXISTS document_pages (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
  page_index INTEGER NOT NULL,
  extracted_text TEXT,
  text_confidence REAL DEFAULT 0.0,
  thumbnail_path TEXT,
  image_meta JSONB DEFAULT '{}',
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_document_pages_doc ON document_pages(document_id);
CREATE INDEX IF NOT EXISTS idx_document_pages_index ON document_pages(document_id, page_index);

-- ============================================
-- 4. document_blocks - 文档块表（结构化内容）
-- ============================================
CREATE TABLE IF NOT EXISTS document_blocks (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
  block_index INTEGER NOT NULL,
  section_id UUID,
  virtual_page_index INTEGER DEFAULT 0,
  block_text TEXT NOT NULL,
  anchor_meta JSONB DEFAULT '{}',
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_document_blocks_doc ON document_blocks(document_id);
CREATE INDEX IF NOT EXISTS idx_document_blocks_section ON document_blocks(section_id);

-- ============================================
-- 5. document_chunks - 文档分块（用于向量搜索）
-- ============================================
CREATE TABLE IF NOT EXISTS document_chunks (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
  page_start INTEGER,
  page_end INTEGER,
  chapter_title TEXT,
  chunk_text TEXT NOT NULL,
  embedding vector(1536),
  keyword_tsv TSVECTOR,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_document_chunks_doc ON document_chunks(document_id);
CREATE INDEX IF NOT EXISTS idx_document_chunks_embedding ON document_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
CREATE INDEX IF NOT EXISTS idx_document_chunks_keywords ON document_chunks USING GIN (keyword_tsv);

-- ============================================
-- 6. annotations - 标注表
-- ============================================
CREATE TABLE IF NOT EXISTS annotations (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
  page_index_or_virtual_page_index INTEGER NOT NULL,
  type TEXT NOT NULL CHECK (type IN ('highlight', 'underline', 'strikeout')),
  color TEXT NOT NULL DEFAULT '#FFD700',
  quote TEXT NOT NULL,
  highlight_areas_or_anchor JSONB DEFAULT '{}',
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_annotations_doc ON annotations(document_id);
CREATE INDEX IF NOT EXISTS idx_annotations_page ON annotations(document_id, page_index_or_virtual_page_index);

-- ============================================
-- 7. notes - 笔记表
-- ============================================
CREATE TABLE IF NOT EXISTS notes (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
  page_index_or_virtual_page_index INTEGER NOT NULL,
  quote TEXT,
  highlight_areas_or_anchor JSONB DEFAULT '{}',
  note_text TEXT NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notes_doc ON notes(document_id);
CREATE INDEX IF NOT EXISTS idx_notes_page ON notes(document_id, page_index_or_virtual_page_index);

-- ============================================
-- 8. handwriting_annotations - 手写标注表
-- ============================================
CREATE TABLE IF NOT EXISTS handwriting_annotations (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
  page_index_or_virtual_page_index INTEGER NOT NULL,
  drawing_data JSONB NOT NULL,
  anchor_meta JSONB DEFAULT '{}',
  tool_type TEXT DEFAULT 'pen',
  color TEXT DEFAULT '#000000',
  width REAL DEFAULT 2.0,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_handwriting_doc ON handwriting_annotations(document_id);
CREATE INDEX IF NOT EXISTS idx_handwriting_page ON handwriting_annotations(document_id, page_index_or_virtual_page_index);

-- ============================================
-- 9. archives - 卡片（归档）表
-- ============================================
CREATE TABLE IF NOT EXISTS archives (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
  page_index_or_virtual_page_index INTEGER NOT NULL,
  quote TEXT NOT NULL,
  highlight_areas_or_anchor JSONB DEFAULT '{}',
  question TEXT,
  answer TEXT,
  tag TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_archives_doc ON archives(document_id);
CREATE INDEX IF NOT EXISTS idx_archives_tag ON archives(tag);

-- ============================================
-- 10. reading_progress - 阅读进度表
-- ============================================
CREATE TABLE IF NOT EXISTS reading_progress (
  user_id UUID NOT NULL,
  document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
  current_page_or_virtual_page INTEGER NOT NULL DEFAULT 0,
  scroll_offset REAL DEFAULT 0.0,
  zoom_level REAL DEFAULT 1.0,
  reading_mode TEXT DEFAULT 'original' CHECK (reading_mode IN ('original', 'reflow')),
  theme TEXT DEFAULT 'light' CHECK (theme IN ('light', 'dark', 'sepia')),
  last_read_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  PRIMARY KEY (user_id, document_id)
);

CREATE INDEX IF NOT EXISTS idx_reading_progress_doc ON reading_progress(document_id);
CREATE INDEX IF NOT EXISTS idx_reading_progress_user ON reading_progress(user_id);

-- ============================================
-- 更新触发器函数
-- ============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 为需要 updated_at 的表添加触发器
CREATE TRIGGER update_folders_updated_at BEFORE UPDATE ON folders
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_documents_updated_at BEFORE UPDATE ON documents
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_annotations_updated_at BEFORE UPDATE ON annotations
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_notes_updated_at BEFORE UPDATE ON notes
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_handwriting_updated_at BEFORE UPDATE ON handwriting_annotations
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_reading_progress_updated_at BEFORE UPDATE ON reading_progress
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- 行级安全策略 (RLS)
-- ============================================
ALTER TABLE folders ENABLE ROW LEVEL SECURITY;
ALTER TABLE documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE document_pages ENABLE ROW LEVEL SECURITY;
ALTER TABLE document_blocks ENABLE ROW LEVEL SECURITY;
ALTER TABLE document_chunks ENABLE ROW LEVEL SECURITY;
ALTER TABLE annotations ENABLE ROW LEVEL SECURITY;
ALTER TABLE notes ENABLE ROW LEVEL SECURITY;
ALTER TABLE handwriting_annotations ENABLE ROW LEVEL SECURITY;
ALTER TABLE archives ENABLE ROW LEVEL SECURITY;
ALTER TABLE reading_progress ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage own folders" ON folders
  FOR ALL USING (owner_id = auth.uid());

CREATE POLICY "Users can manage own documents" ON documents
  FOR ALL USING (owner_id = auth.uid());

CREATE POLICY "Users can view own document pages" ON document_pages
  FOR ALL USING (EXISTS (SELECT 1 FROM documents WHERE documents.id = document_pages.document_id AND documents.owner_id = auth.uid()));

CREATE POLICY "Users can view own document blocks" ON document_blocks
  FOR ALL USING (EXISTS (SELECT 1 FROM documents WHERE documents.id = document_blocks.document_id AND documents.owner_id = auth.uid()));

CREATE POLICY "Users can view own document chunks" ON document_chunks
  FOR ALL USING (EXISTS (SELECT 1 FROM documents WHERE documents.id = document_chunks.document_id AND documents.owner_id = auth.uid()));

CREATE POLICY "Users can manage own annotations" ON annotations
  FOR ALL USING (EXISTS (SELECT 1 FROM documents WHERE documents.id = annotations.document_id AND documents.owner_id = auth.uid()));

CREATE POLICY "Users can manage own notes" ON notes
  FOR ALL USING (EXISTS (SELECT 1 FROM documents WHERE documents.id = notes.document_id AND documents.owner_id = auth.uid()));

CREATE POLICY "Users can manage own handwriting" ON handwriting_annotations
  FOR ALL USING (EXISTS (SELECT 1 FROM documents WHERE documents.id = handwriting_annotations.document_id AND documents.owner_id = auth.uid()));

CREATE POLICY "Users can manage own archives" ON archives
  FOR ALL USING (EXISTS (SELECT 1 FROM documents WHERE documents.id = archives.document_id AND documents.owner_id = auth.uid()));

CREATE POLICY "Users can manage own reading progress" ON reading_progress
  FOR ALL USING (user_id = auth.uid());

-- ============================================
-- 完成
-- ============================================
SELECT '✅ Schema created successfully! Tables: 10, Indexes: 21, RLS Policies: 10' as status;

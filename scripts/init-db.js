/**
 * ReadFlow 数据库初始化脚本
 * 自动连接到 Supabase 并创建所有表
 */

const { createClient } = require('@supabase/supabase-js');
require('dotenv').config();

const supabaseUrl = process.env.SUPABASE_URL;
const serviceRoleKey = process.env.SUPABASE_SERVICE_ROLE_KEY;

if (!supabaseUrl || !serviceRoleKey) {
  console.error('❌ 错误: 缺少环境变量');
  console.error('请确保 .env 文件包含 SUPABASE_URL 和 SUPABASE_SERVICE_ROLE_KEY');
  process.exit(1);
}

const supabase = createClient(supabaseUrl, serviceRoleKey);

// SQL 语句
const statements = [
  // 扩展
  'CREATE EXTENSION IF NOT EXISTS "uuid-ossp";',
  'CREATE EXTENSION IF NOT EXISTS "vector";',

  // 文件夹表
  `CREATE TABLE IF NOT EXISTS folders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    owner_id UUID NOT NULL,
    parent_folder_id UUID REFERENCES folders(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    color TEXT DEFAULT '#3B82F6',
    icon TEXT DEFAULT 'folder',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
  );`,
  'CREATE INDEX IF NOT EXISTS idx_folders_owner ON folders(owner_id);',

  // 文档表
  `CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    owner_id UUID NOT NULL,
    folder_id UUID REFERENCES folders(id) ON DELETE SET NULL,
    title TEXT NOT NULL,
    file_path TEXT NOT NULL,
    file_type TEXT NOT NULL,
    page_count_or_virtual_page_count INTEGER DEFAULT 0,
    outline_json JSONB DEFAULT '{}',
    processing_status TEXT DEFAULT 'pending',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
  );`,
  'CREATE INDEX IF NOT EXISTS idx_documents_owner ON documents(owner_id);',

  // 页面表
  `CREATE TABLE IF NOT EXISTS document_pages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    page_index INTEGER NOT NULL,
    extracted_text TEXT,
    text_confidence REAL DEFAULT 0.0,
    thumbnail_path TEXT,
    image_meta JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
  );`,
  'CREATE INDEX IF NOT EXISTS idx_document_pages_doc ON document_pages(document_id);',

  // 标注表
  `CREATE TABLE IF NOT EXISTS annotations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    page_index_or_virtual_page_index INTEGER NOT NULL,
    type TEXT NOT NULL,
    color TEXT DEFAULT '#FFD700',
    quote TEXT NOT NULL,
    highlight_areas_or_anchor JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
  );`,
  'CREATE INDEX IF NOT EXISTS idx_annotations_doc ON annotations(document_id);',

  // 笔记表
  `CREATE TABLE IF NOT EXISTS notes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    page_index_or_virtual_page_index INTEGER NOT NULL,
    quote TEXT,
    note_text TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
  );`,
  'CREATE INDEX IF NOT EXISTS idx_notes_doc ON notes(document_id);',

  // 阅读进度表
  `CREATE TABLE IF NOT EXISTS reading_progress (
    user_id UUID NOT NULL,
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    current_page_or_virtual_page INTEGER DEFAULT 0,
    zoom_level REAL DEFAULT 1.0,
    theme TEXT DEFAULT 'light',
    last_read_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (user_id, document_id)
  );`,

  // RLS
  'ALTER TABLE folders ENABLE ROW LEVEL SECURITY;',
  'ALTER TABLE documents ENABLE ROW LEVEL SECURITY;',
  'ALTER TABLE document_pages ENABLE ROW LEVEL SECURITY;',
  'ALTER TABLE annotations ENABLE ROW LEVEL SECURITY;',
  'ALTER TABLE notes ENABLE ROW LEVEL SECURITY;',
  'ALTER TABLE reading_progress ENABLE ROW LEVEL SECURITY;',

  // RLS 策略
  'CREATE POLICY "Users manage own folders" ON folders FOR ALL USING (owner_id = auth.uid());',
  'CREATE POLICY "Users manage own documents" ON documents FOR ALL USING (owner_id = auth.uid());',
  'CREATE POLICY "Users view own pages" ON document_pages FOR ALL USING (EXISTS (SELECT 1 FROM documents WHERE documents.id = document_pages.document_id AND documents.owner_id = auth.uid()));',
  'CREATE POLICY "Users manage own annotations" ON annotations FOR ALL USING (EXISTS (SELECT 1 FROM documents WHERE documents.id = annotations.document_id AND documents.owner_id = auth.uid()));',
  'CREATE POLICY "Users manage own notes" ON notes FOR ALL USING (EXISTS (SELECT 1 FROM documents WHERE documents.id = notes.document_id AND documents.owner_id = auth.uid()));',
  'CREATE POLICY "Users manage own progress" ON reading_progress FOR ALL USING (user_id = auth.uid());',
];

async function initDatabase() {
  console.log('🚀 开始初始化 ReadFlow 数据库...\n');

  let success = 0;
  let failed = 0;

  for (let i = 0; i < statements.length; i++) {
    const sql = statements[i];
    process.stdout.write(`[${i + 1}/${statements.length}] `);

    try {
      const { error } = await supabase.rpc('exec_sql', { sql });

      if (error) {
        // 如果不支持 rpc，尝试其他方法
        if (error.message.includes('does not exist') || error.message.includes('permission')) {
          console.log(`⚠️  ${error.message.substring(0, 50)}`);
          failed++;
        } else {
          console.log(`⚠️  ${error.message.substring(0, 50)}`);
          failed++;
        }
      } else {
        console.log('✅');
        success++;
      }
    } catch (err) {
      console.log(`❌ ${err.message.substring(0, 50)}`);
      failed++;
    }
  }

  console.log('\n' + '='.repeat(50));
  console.log(`📊 结果: 成功 ${success}, 失败 ${failed}`);
  console.log('='.repeat(50));

  if (success >= 15) {
    console.log('\n🎉 数据库初始化完成！');
  } else {
    console.log('\n💡 提示: 请在 Supabase Dashboard SQL Editor 中执行 scripts/create_schema.sql');
  }
}

initDatabase().catch(console.error);

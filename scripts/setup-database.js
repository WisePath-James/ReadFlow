const { createClient } = require('@supabase/supabase-js');
require('dotenv').config();

// 从 .env 加载配置
const supabaseUrl = process.env.SUPABASE_URL;
const serviceRoleKey = process.env.SUPABASE_SERVICE_ROLE_KEY;

if (!supabaseUrl || !serviceRoleKey) {
  console.error('❌ 错误: 请确保 .env 文件中配置了 SUPABASE_URL 和 SUPABASE_SERVICE_ROLE_KEY');
  process.exit(1);
}

const supabase = createClient(supabaseUrl, serviceRoleKey);

// SQL 语句（分批执行，避免超时）
const sqlBatches = [
  // Batch 1: 扩展
  [
    'CREATE EXTENSION IF NOT EXISTS "uuid-ossp";',
    'CREATE EXTENSION IF NOT EXISTS "vector";'
  ],

  // Batch 2: folders 表
  [
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
    'CREATE INDEX IF NOT EXISTS idx_folders_parent ON folders(parent_folder_id);'
  ],

  // Batch 3: documents 表
  [
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
    'CREATE INDEX IF NOT EXISTS idx_documents_folder ON documents(folder_id);',
    'CREATE INDEX IF NOT EXISTS idx_documents_status ON documents(processing_status);',
    'CREATE INDEX IF NOT EXISTS idx_documents_created ON documents(created_at DESC);'
  ],

  // Batch 4: document_pages 表
  [
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
    'CREATE INDEX IF NOT EXISTS idx_document_pages_index ON document_pages(document_id, page_index);'
  ],

  // Batch 5: document_blocks 表
  [
    `CREATE TABLE IF NOT EXISTS document_blocks (
      id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
      document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
      block_index INTEGER NOT NULL,
      section_id UUID,
      virtual_page_index INTEGER DEFAULT 0,
      block_text TEXT NOT NULL,
      anchor_meta JSONB DEFAULT '{}',
      created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
    );`,
    'CREATE INDEX IF NOT EXISTS idx_document_blocks_doc ON document_blocks(document_id);',
    'CREATE INDEX IF NOT EXISTS idx_document_blocks_section ON document_blocks(section_id);'
  ],

  // Batch 6: document_chunks 表
  [
    `CREATE TABLE IF NOT EXISTS document_chunks (
      id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
      document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
      page_start INTEGER,
      page_end INTEGER,
      chapter_title TEXT,
      chunk_text TEXT NOT NULL,
      embedding vector(1536),
      keyword_tsv TSVECTOR,
      created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
    );`,
    'CREATE INDEX IF NOT EXISTS idx_document_chunks_doc ON document_chunks(document_id);',
    'CREATE INDEX IF NOT EXISTS idx_document_chunks_keywords ON document_chunks USING GIN (keyword_tsv);'
  ],

  // Batch 7: annotations 表
  [
    `CREATE TABLE IF NOT EXISTS annotations (
      id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
      document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
      page_index_or_virtual_page_index INTEGER NOT NULL,
      type TEXT NOT NULL,
      color TEXT NOT NULL DEFAULT '#FFD700',
      quote TEXT NOT NULL,
      highlight_areas_or_anchor JSONB DEFAULT '{}',
      created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
      updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
    );`,
    'CREATE INDEX IF NOT EXISTS idx_annotations_doc ON annotations(document_id);',
    'CREATE INDEX IF NOT EXISTS idx_annotations_page ON annotations(document_id, page_index_or_virtual_page_index);'
  ],

  // Batch 8: notes 表
  [
    `CREATE TABLE IF NOT EXISTS notes (
      id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
      document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
      page_index_or_virtual_page_index INTEGER NOT NULL,
      quote TEXT,
      highlight_areas_or_anchor JSONB DEFAULT '{}',
      note_text TEXT NOT NULL,
      created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
      updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
    );`,
    'CREATE INDEX IF NOT EXISTS idx_notes_doc ON notes(document_id);',
    'CREATE INDEX IF NOT EXISTS idx_notes_page ON notes(document_id, page_index_or_virtual_page_index);'
  ],

  // Batch 9: handwriting_annotations 表
  [
    `CREATE TABLE IF NOT EXISTS handwriting_annotations (
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
    );`,
    'CREATE INDEX IF NOT EXISTS idx_handwriting_doc ON handwriting_annotations(document_id);',
    'CREATE INDEX IF NOT EXISTS idx_handwriting_page ON handwriting_annotations(document_id, page_index_or_virtual_page_index);'
  ],

  // Batch 10: archives 表
  [
    `CREATE TABLE IF NOT EXISTS archives (
      id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
      document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
      page_index_or_virtual_page_index INTEGER NOT NULL,
      quote TEXT NOT NULL,
      highlight_areas_or_anchor JSONB DEFAULT '{}',
      question TEXT,
      answer TEXT,
      tag TEXT,
      created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
    );`,
    'CREATE INDEX IF NOT EXISTS idx_archives_doc ON archives(document_id);',
    'CREATE INDEX IF NOT EXISTS idx_archives_tag ON archives(tag);'
  ],

  // Batch 11: reading_progress 表
  [
    `CREATE TABLE IF NOT EXISTS reading_progress (
      user_id UUID NOT NULL,
      document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
      current_page_or_virtual_page INTEGER NOT NULL DEFAULT 0,
      scroll_offset REAL DEFAULT 0.0,
      zoom_level REAL DEFAULT 1.0,
      reading_mode TEXT DEFAULT 'original',
      theme TEXT DEFAULT 'light',
      last_read_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
      updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
      PRIMARY KEY (user_id, document_id)
    );`,
    'CREATE INDEX IF NOT EXISTS idx_reading_progress_doc ON reading_progress(document_id);',
    'CREATE INDEX IF NOT EXISTS idx_reading_progress_user ON reading_progress(user_id);'
  ],

  // Batch 12: 触发器函数和触发器
  [
    `CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS $$
    BEGIN
      NEW.updated_at = NOW();
      RETURN NEW;
    END;
    $$ LANGUAGE plpgsql;`,
    'CREATE TRIGGER update_folders_updated_at BEFORE UPDATE ON folders FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();',
    'CREATE TRIGGER update_documents_updated_at BEFORE UPDATE ON documents FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();',
    'CREATE TRIGGER update_annotations_updated_at BEFORE UPDATE ON annotations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();',
    'CREATE TRIGGER update_notes_updated_at BEFORE UPDATE ON notes FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();',
    'CREATE TRIGGER update_handwriting_updated_at BEFORE UPDATE ON handwriting_annotations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();',
    'CREATE TRIGGER update_reading_progress_updated_at BEFORE UPDATE ON reading_progress FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();'
  ],

  // Batch 13: RLS 策略
  [
    'ALTER TABLE folders ENABLE ROW LEVEL SECURITY;',
    'ALTER TABLE documents ENABLE ROW LEVEL SECURITY;',
    'ALTER TABLE document_pages ENABLE ROW LEVEL SECURITY;',
    'ALTER TABLE document_blocks ENABLE ROW LEVEL SECURITY;',
    'ALTER TABLE document_chunks ENABLE ROW LEVEL SECURITY;',
    'ALTER TABLE annotations ENABLE ROW LEVEL SECURITY;',
    'ALTER TABLE notes ENABLE ROW LEVEL SECURITY;',
    'ALTER TABLE handwriting_annotations ENABLE ROW LEVEL SECURITY;',
    'ALTER TABLE archives ENABLE ROW LEVEL SECURITY;',
    'ALTER TABLE reading_progress ENABLE ROW LEVEL SECURITY;',
    'CREATE POLICY "Users can manage own folders" ON folders FOR ALL USING (owner_id = auth.uid());',
    'CREATE POLICY "Users can manage own documents" ON documents FOR ALL USING (owner_id = auth.uid());',
    'CREATE POLICY "Users can view own document pages" ON document_pages FOR ALL USING (EXISTS (SELECT 1 FROM documents WHERE documents.id = document_pages.document_id AND documents.owner_id = auth.uid()));',
    'CREATE POLICY "Users can view own document blocks" ON document_blocks FOR ALL USING (EXISTS (SELECT 1 FROM documents WHERE documents.id = document_blocks.document_id AND documents.owner_id = auth.uid()));',
    'CREATE POLICY "Users can view own document chunks" ON document_chunks FOR ALL USING (EXISTS (SELECT 1 FROM documents WHERE documents.id = document_chunks.document_id AND documents.owner_id = auth.uid()));',
    'CREATE POLICY "Users can manage own annotations" ON annotations FOR ALL USING (EXISTS (SELECT 1 FROM documents WHERE documents.id = annotations.document_id AND documents.owner_id = auth.uid()));',
    'CREATE POLICY "Users can manage own notes" ON notes FOR ALL USING (EXISTS (SELECT 1 FROM documents WHERE documents.id = notes.document_id AND documents.owner_id = auth.uid()));',
    'CREATE POLICY "Users can manage own handwriting" ON handwriting_annotations FOR ALL USING (EXISTS (SELECT 1 FROM documents WHERE documents.id = handwriting_annotations.document_id AND documents.owner_id = auth.uid()));',
    'CREATE POLICY "Users can manage own archives" ON archives FOR ALL USING (EXISTS (SELECT 1 FROM documents WHERE documents.id = archives.document_id AND documents.owner_id = auth.uid()));',
    'CREATE POLICY "Users can manage own reading progress" ON reading_progress FOR ALL USING (user_id = auth.uid());'
  ]
];

async function executeBatch(batch, batchNum) {
  console.log(`\n📦 执行批次 ${batchNum}/${sqlBatches.length} (${batch.length} 条语句)`);

  for (let i = 0; i < batch.length; i++) {
    const stmt = batch[i];
    try {
      // 使用 Supabase 的 RPC 执行原始 SQL
      const { error } = await supabase.rpc('exec_sql', { sql: stmt });

      if (error) {
        console.log(`   ⚠️  语句 ${i + 1}: ${error.message.substring(0, 50)}...`);
        // 忽略某些错误（如扩展已存在）
      } else {
        console.log(`   ✅ 语句 ${i + 1}/${batch.length} 成功`);
      }
    } catch (error) {
      console.log(`   ❌ 语句 ${i + 1}: ${error.message.substring(0, 50)}...`);
    }
  }
}

async function main() {
  console.log('🚀 开始创建 ReadFlow 数据库 Schema...\n');
  console.log(`📡 连接到: ${supabaseUrl}`);
  console.log(`👤 使用 Service Role Key\n`);

  for (let i = 0; i < sqlBatches.length; i++) {
    await executeBatch(sqlBatches[i], i + 1);
  }

  console.log('\n' + '='.repeat(60));
  console.log('🎉 Schema 创建完成！');
  console.log('='.repeat(60));

  // 验证表是否创建成功
  console.log('\n🔍 验证数据库表...\n');

  const { data, error } = await supabase
    .from('information_schema.tables')
    .select('table_name')
    .eq('table_schema', 'public')
    .order('table_name');

  if (error) {
    console.error('❌ 验证失败:', error.message);
  } else {
    const tables = data.map(t => t.table_name).sort();
    console.log(`✅ 已创建 ${tables.length} 个表:`);
    tables.forEach(t => console.log(`   • ${t}`));

    const expectedTables = [
      'folders', 'documents', 'document_pages', 'document_blocks',
      'document_chunks', 'annotations', 'notes', 'handwriting_annotations',
      'archives', 'reading_progress'
    ];

    const missing = expectedTables.filter(t => !tables.includes(t));
    if (missing.length === 0) {
      console.log('\n🎯 所有 10 个表创建成功！');
    } else {
      console.log(`\n⚠️  缺失的表: ${missing.join(', ')}`);
    }
  }

  console.log('\n💡 下一步:');
  console.log('   1. 运行: npm run gen:types  (生成 TypeScript 类型)');
  console.log('   2. 运行: npm run dev        (启动后端服务)');
  console.log('   3. 开发前端并打包 APK');
}

main().catch(console.error);

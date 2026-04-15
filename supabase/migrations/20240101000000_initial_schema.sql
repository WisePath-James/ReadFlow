-- 创建文件夹表
create table if not exists folders (
  id uuid default gen_random_uuid() primary key,
  owner_id uuid not null references auth.users(id) on delete cascade,
  parent_folder_id uuid references folders(id) on delete cascade,
  name text not null,
  color text default '#3B82F6',
  icon text default 'folder',
  created_at timestamp with time zone default timezone('utc'::text, now()) not null,
  updated_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- 创建文档表
create table if not exists documents (
  id uuid default gen_random_uuid() primary key,
  owner_id uuid not null references auth.users(id) on delete cascade,
  folder_id uuid references folders(id) on delete set null,
  title text not null,
  file_path text not null,
  file_type text not null,
  page_count_or_virtual_page_count integer default 0,
  outline_json jsonb default '{}',
  processing_status text default 'pending',
  created_at timestamp with time zone default timezone('utc'::text, now()) not null,
  updated_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- 创建文档页表（PDF 等固定版式）
create table if not exists document_pages (
  id uuid default gen_random_uuid() primary key,
  document_id uuid not null references documents(id) on delete cascade,
  page_index integer not null,
  extracted_text text,
  text_confidence float default 1.0,
  thumbnail_path text,
  image_meta jsonb default '{}',
  created_at timestamp with time zone default timezone('utc'::text, now()) not null,
  unique(document_id, page_index)
);

-- 创建文档块表（结构化文档）
create table if not exists document_blocks (
  id uuid default gen_random_uuid() primary key,
  document_id uuid not null references documents(id) on delete cascade,
  block_index integer not null,
  section_id text,
  virtual_page_index integer not null,
  block_text text not null,
  anchor_meta jsonb default '{}',
  created_at timestamp with time zone default timezone('utc'::text, now()) not null,
  unique(document_id, block_index)
);

-- 创建文档块向量表（用于全文检索）
create table if not exists document_chunks (
  id uuid default gen_random_uuid() primary key,
  document_id uuid not null references documents(id) on delete cascade,
  page_start integer,
  page_end integer,
  chapter_title text,
  chunk_text text not null,
  embedding vector(1536),
  keyword_tsv tsvector,
  created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- 创建标注表（高亮/下划线/删除线）
create table if not exists annotations (
  id uuid default gen_random_uuid() primary key,
  document_id uuid not null references documents(id) on delete cascade,
  page_index_or_virtual_page_index integer not null,
  type text not null check (type in ('highlight', 'underline', 'strikeout')),
  color text not null,
  quote text not null,
  highlight_areas_or_anchor jsonb not null,
  created_at timestamp with time zone default timezone('utc'::text, now()) not null,
  updated_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- 创建笔记表
create table if not exists notes (
  id uuid default gen_random_uuid() primary key,
  document_id uuid not null references documents(id) on delete cascade,
  page_index_or_virtual_page_index integer not null,
  quote text not null,
  highlight_areas_or_anchor jsonb not null,
  note_text text not null,
  created_at timestamp with time zone default timezone('utc'::text, now()) not null,
  updated_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- 创建手写批注表
create table if not exists handwriting_annotations (
  id uuid default gen_random_uuid() primary key,
  document_id uuid not null references documents(id) on delete cascade,
  page_index_or_virtual_page_index integer not null,
  drawing_data jsonb not null,
  anchor_meta jsonb default '{}',
  tool_type text not null,
  color text not null,
  width float not null,
  created_at timestamp with time zone default timezone('utc'::text, now()) not null,
  updated_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- 创建知识归档表
create table if not exists archives (
  id uuid default gen_random_uuid() primary key,
  document_id uuid not null references documents(id) on delete cascade,
  page_index_or_virtual_page_index integer not null,
  quote text not null,
  highlight_areas_or_anchor jsonb not null,
  question text not null,
  answer text not null,
  tag text,
  created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- 创建阅读进度表
create table if not exists reading_progress (
  user_id uuid not null references auth.users(id) on delete cascade,
  document_id uuid not null references documents(id) on delete cascade,
  current_page_or_virtual_page integer not null default 0,
  scroll_offset float default 0,
  zoom_level float default 1.0,
  reading_mode text default 'original',
  theme text default 'light',
  last_read_at timestamp with time zone default timezone('utc'::text, now()) not null,
  updated_at timestamp with time zone default timezone('utc'::text, now()) not null,
  primary key (user_id, document_id)
);

-- 创建索引以提升查询性能
create index if not exists idx_documents_owner_id on documents(owner_id);
create index if not exists idx_documents_folder_id on documents(folder_id);
create index if not exists idx_documents_processing_status on documents(processing_status);
create index if not exists idx_document_pages_document_id on document_pages(document_id);
create index if not exists idx_document_blocks_document_id on document_blocks(document_id);
create index if not exists idx_document_chunks_document_id on document_chunks(document_id);
create index if not exists idx_document_chunks_embedding on document_chunks using ivfflat (embedding vector_cosine_ops) with (lists = 100);
create index if not exists idx_annotations_document_id on annotations(document_id);
create index if not exists idx_notes_document_id on notes(document_id);
create index if not exists idx_handwriting_document_id on handwriting_annotations(document_id);
create index if not exists idx_archives_document_id on archives(document_id);
create index if not exists idx_reading_progress_user_id on reading_progress(user_id);

-- 启用 Row Level Security
alter table folders enable row level security;
alter table documents enable row level security;
alter table document_pages enable row level security;
alter table document_blocks enable row level security;
alter table document_chunks enable row level security;
alter table annotations enable row level security;
alter table notes enable row level security;
alter table handwriting_annotations enable row level security;
alter table archives enable row level security;
alter table reading_progress enable row level security;

-- 创建策略
create policy "用户可管理自己的文件夹" on folders
  for all using (auth.uid() = owner_id);

create policy "用户可管理自己的文档" on documents
  for all using (auth.uid() = owner_id);

create policy "用户可访问自己的文档页" on document_pages
  for all using (exists (
    select 1 from documents where documents.id = document_pages.document_id
    and documents.owner_id = auth.uid()
  ));

create policy "用户可访问自己的文档块" on document_blocks
  for all using (exists (
    select 1 from documents where documents.id = document_blocks.document_id
    and documents.owner_id = auth.uid()
  ));

create policy "用户可访问自己的文档块向量" on document_chunks
  for all using (exists (
    select 1 from documents where documents.id = document_chunks.document_id
    and documents.owner_id = auth.uid()
  ));

create policy "用户可管理自己的标注" on annotations
  for all using (auth.uid() = (
    select owner_id from documents where documents.id = annotations.document_id
  ));

create policy "用户可管理自己的笔记" on notes
  for all using (auth.uid() = (
    select owner_id from documents where documents.id = notes.document_id
  ));

create policy "用户可管理自己的手写批注" on handwriting_annotations
  for all using (auth.uid() = (
    select owner_id from documents where documents.id = handwriting_annotations.document_id
  ));

create policy "用户可管理自己的归档" on archives
  for all using (auth.uid() = (
    select owner_id from documents where documents.id = archives.document_id
  ));

create policy "用户可管理自己的阅读进度" on reading_progress
  for all using (auth.uid() = user_id);

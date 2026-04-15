-- 全文搜索函数（关键词匹配）
create or replace function search_documents(
  p_user_id uuid,
  p_query text,
  p_limit integer default 20
)
returns table (
  id uuid,
  document_id uuid,
  page_index integer,
  snippet text,
  score float
)
language sql
security definer
as $$
  -- 搜索文档页文本
  select
    dp.id,
    dp.document_id,
    dp.page_index,
    ts_headline('chinese', dp.extracted_text, websearch_to_tsquery('chinese', p_query)) as snippet,
    ts_rank(to_tsvector('chinese', dp.extracted_text), websearch_to_tsquery('chinese', p_query)) as score
  from document_pages dp
  join documents d on d.id = dp.document_id
  where d.owner_id = p_user_id
    and to_tsvector('chinese', dp.extracted_text) @@ websearch_to_tsquery('chinese', p_query)
  union
  -- 搜索文档块文本
  select
    db.id,
    db.document_id,
    db.virtual_page_index as page_index,
    ts_headline('chinese', db.block_text, websearch_to_tsquery('chinese', p_query)) as snippet,
    ts_rank(to_tsvector('chinese', db.block_text), websearch_to_tsquery('chinese', p_query)) as score
  from document_blocks db
  join documents d on d.id = db.document_id
  where d.owner_id = p_user_id
    and to_tsvector('chinese', db.block_text) @@ websearch_to_tsquery('chinese', p_query)
  order by score desc
  limit p_limit;
$$;

-- 向量相似度搜索函数
create or replace function match_document_chunks(
  query_embedding vector(1536),
  match_threshold float default 0.7,
  match_count int default 10,
  p_document_id uuid default null,
  p_user_id uuid default null
)
returns table (
  id uuid,
  document_id uuid,
  page_start integer,
  page_end integer,
  chapter_title text,
  chunk_text text,
  similarity float
)
language sql
security definer
as $$
  select
    dc.id,
    dc.document_id,
    dc.page_start,
    dc.page_end,
    dc.chapter_title,
    dc.chunk_text,
    1 - (dc.embedding <=> query_embedding) as similarity
  from document_chunks dc
  join documents d on d.id = dc.document_id
  where
    (1 - (dc.embedding <=> query_embedding)) > match_threshold
    and (
      p_document_id is null or dc.document_id = p_document_id
    )
    and (
      p_user_id is null or d.owner_id = p_user_id
    )
  order by dc.embedding <=> query_embedding
  limit match_count;
$$;

-- 搜索文档块（结合用户权限）
create or replace function search_user_document_chunks(
  p_user_id uuid,
  p_query text,
  p_limit integer default 20
)
returns table (
  id uuid,
  document_id uuid,
  page_start integer,
  page_end integer,
  chunk_text text,
  score float
)
language sql
security definer
as $$
  select
    dc.id,
    dc.document_id,
    dc.page_start,
    dc.page_end,
    dc.chunk_text,
    ts_rank(dc.keyword_tsv, websearch_to_tsquery('chinese', p_query)) as score
  from document_chunks dc
  join documents d on d.id = dc.document_id
  where d.owner_id = p_user_id
    and dc.keyword_tsv @@ websearch_to_tsquery('chinese', p_query)
  order by score desc
  limit p_limit;
$$;

-- 获取用户文档统计
create or replace function get_user_document_stats(
  p_user_id uuid
)
returns json
language sql
security definer
as $$
  select json_build_object(
    'total_documents', count(distinct d.id),
    'total_pages', count(distinct dp.page_index),
    'total_annotations', count(distinct a.id),
    'total_notes', count(distinct n.id),
    'total_archives', count(distinct ar.id),
    'processing', count(distinct case when d.processing_status = 'processing' then d.id end),
    'completed', count(distinct case when d.processing_status = 'completed' then d.id end),
    'failed', count(distinct case when d.processing_status = 'failed' then d.id end)
  )
  from documents d
  left join document_pages dp on dp.document_id = d.id
  left join annotations a on a.document_id = d.id
  left join notes n on n.document_id = d.id
  left join archives ar on ar.document_id = d.id
  where d.owner_id = p_user_id;
$$;

-- 更新文档更新时间触发器
create or replace function update_updated_at_column()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = timezone('utc'::text, now());
  return new;
end;
$$;

-- 应用触发器到相关表
create trigger update_folders_updated_at
  before update on folders
  for each row
  execute function update_updated_at_column();

create trigger update_documents_updated_at
  before update on documents
  for each row
  execute function update_updated_at_column();

create trigger update_annotations_updated_at
  before update on annotations
  for each row
  execute function update_updated_at_column();

create trigger update_notes_updated_at
  before update on notes
  for each row
  execute function update_updated_at_column();

create trigger update_handwriting_updated_at
  before update on handwriting_annotations
  for each row
  execute function update_updated_at_column();

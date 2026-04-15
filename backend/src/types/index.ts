// Supabase 数据库类型定义
export type Json = string | number | boolean | null | { [key: string]: Json | undefined } | Json[];

export interface Folder {
  id: string;
  owner_id: string;
  parent_folder_id: string | null;
  name: string;
  color: string;
  icon: string;
  created_at: string;
  updated_at: string;
}

export interface Document {
  id: string;
  owner_id: string;
  folder_id: string | null;
  title: string;
  file_path: string;
  file_type: string;
  page_count_or_virtual_page_count: number;
  outline_json: Json;
  processing_status: 'pending' | 'processing' | 'completed' | 'failed';
  created_at: string;
  updated_at: string;
}

export interface DocumentPage {
  id: string;
  document_id: string;
  page_index: number;
  extracted_text: string | null;
  text_confidence: number;
  thumbnail_path: string | null;
  image_meta: Json;
  created_at: string;
}

export interface DocumentBlock {
  id: string;
  document_id: string;
  block_index: number;
  section_id: string | null;
  virtual_page_index: number;
  block_text: string;
  anchor_meta: Json;
  created_at: string;
}

export interface DocumentChunk {
  id: string;
  document_id: string;
  page_start: number | null;
  page_end: number | null;
  chapter_title: string | null;
  chunk_text: string;
  embedding: number[] | null;
  keyword_tsv: string | null;
  created_at: string;
}

export interface Annotation {
  id: string;
  document_id: string;
  page_index_or_virtual_page_index: number;
  type: 'highlight' | 'underline' | 'strikeout';
  color: string;
  quote: string;
  highlight_areas_or_anchor: Json;
  created_at: string;
  updated_at: string;
}

export interface Note {
  id: string;
  document_id: string;
  page_index_or_virtual_page_index: number;
  quote: string;
  highlight_areas_or_anchor: Json;
  note_text: string;
  created_at: string;
  updated_at: string;
}

export interface HandwritingAnnotation {
  id: string;
  document_id: string;
  page_index_or_virtual_page_index: number;
  drawing_data: Json;
  anchor_meta: Json;
  tool_type: string;
  color: string;
  width: number;
  created_at: string;
  updated_at: string;
}

export interface Archive {
  id: string;
  document_id: string;
  page_index_or_virtual_page_index: number;
  quote: string;
  highlight_areas_or_anchor: Json;
  question: string;
  answer: string;
  tag: string | null;
  created_at: string;
}

export interface ReadingProgress {
  user_id: string;
  document_id: string;
  current_page_or_virtual_page: number;
  scroll_offset: number;
  zoom_level: number;
  reading_mode: 'original' | 'reflow';
  theme: 'light' | 'dark' | 'sepia';
  last_read_at: string;
  updated_at: string;
}

// API 请求/响应类型
export interface DocumentUploadRequest {
  file: Express.Multer.File;
  folder_id?: string;
}

export interface DocumentUploadResponse {
  id: string;
  title: string;
  status: 'pending' | 'processing' | 'completed' | 'failed';
  message: string;
}

export interface AIQuickAskRequest {
  document_id: string;
  selection_text: string;
  page_index: number;
  question: string;
  context_pages?: {
    prev_pages: number[];
    next_pages: number[];
  };
}

export interface AIQuickAskResponse {
  thread_id: string;
  answer: string;
  sources: Array<{
    page_index: number;
    quote?: string;
  }>;
}

export interface AIDeepAnalysisRequest {
  document_id: string;
  question: string;
  max_results?: number;
}

export interface AIDeepAnalysisResponse {
  answer: string;
  sources: Array<{
    page_index: number;
    block_index?: number;
    quote: string;
    relevance: number;
  }>;
}

export interface TranslationRequest {
  text: string;
  target_language: 'zh' | 'en';
  context?: string;
}

export interface TranslationResponse {
  translated_text: string;
  original_terms?: Array<{ original: string; translated: string }>;
}

export interface SummaryRequest {
  text: string;
  style: 'brief' | 'detailed' | 'notes' | 'exam';
}

export interface SummaryResponse {
  summary: string;
  key_points: string[];
}

// 向量搜索响应
export interface VectorSearchResult {
  id: string;
  document_id: string;
  page_start: number;
  page_end: number;
  chunk_text: string;
  similarity: number;
}

// 文件处理状态
export type ProcessingStatus = 'pending' | 'processing' | 'completed' | 'failed';

export interface ProcessingProgress {
  document_id: string;
  status: ProcessingStatus;
  progress: number; // 0-100
  stage: string;
  error?: string;
}

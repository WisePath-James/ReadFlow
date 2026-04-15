import { createClient } from '@supabase/supabase-js';

const supabaseUrl = 'https://myetilelfmmhrbohfwwp.supabase.co';
const supabaseAnonKey = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im15ZXRpbGVsZm1taHJib2hmd3dwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDQ1NzU4ODQsImV4cCI6MjA2MDEzMTg4NH0.GJBHciHja2lJuYd2XJgCxmBMb3M6W6Z3xqThCdK0e9k';

export const supabase = createClient(supabaseUrl, supabaseAnonKey);

export interface Document {
  id: string;
  owner_id: string;
  title: string;
  file_path: string;
  file_type: string;
  page_count_or_virtual_page_count: number;
  processing_status: 'pending' | 'processing' | 'completed' | 'failed';
  created_at: string;
}

export interface Folder {
  id: string;
  owner_id: string;
  name: string;
  color: string;
  icon: string;
  created_at: string;
}

export interface Note {
  id: string;
  document_id: string;
  page_index_or_virtual_page_index: number;
  quote: string;
  note_text: string;
  created_at: string;
}

export interface Annotation {
  id: string;
  document_id: string;
  page_index_or_virtual_page_index: number;
  type: 'highlight' | 'underline' | 'strikeout';
  color: string;
  quote: string;
  created_at: string;
}

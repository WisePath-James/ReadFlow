import { Router } from 'express';
import { supabase } from '../index';

const router = Router();

// 全文搜索（关键词 + 语义）
router.post('/', async (req, res) => {
  try {
    const userId = req.headers['x-user-id'] as string;
    const { document_id, query, mode = 'hybrid', limit = 20 } = req.body;

    if (!userId || !query) {
      return res.status(400).json({ error: '缺少必要参数' });
    }

    let results: any[] = [];

    if (mode === 'keyword' || mode === 'hybrid') {
      // 关键词���索（使用 PostgreSQL 全文检索）
      const { data: keywordResults, error } = await supabase.rpc('search_documents', {
        p_user_id: userId,
        p_query: query,
        p_limit: limit
      });

      if (error) throw error;
      results = results.concat(keywordResults || []);
    }

    if (mode === 'semantic' || mode === 'hybrid') {
      // 语义搜索
      const { openai } = require('../index');
      const embeddingResponse = await openai.embeddings.create({
        model: 'text-embedding-3-small',
        input: query
      });

      const queryEmbedding = embeddingResponse.data[0].embedding;

      // 向量搜索
      const { data: vectorResults, error: vectorError } = await supabase.rpc('match_document_chunks', {
        query_embedding: queryEmbedding,
        match_threshold: 0.7,
        match_count: limit,
        p_user_id: userId
      });

      if (vectorError) throw vectorError;
      results = results.concat(vectorResults || []);
    }

    // 去重和排序
    const seen = new Set<string>();
    const uniqueResults = results.filter(item => {
      if (seen.has(item.id)) return false;
      seen.add(item.id);
      return true;
    });

    res.json({
      query,
      mode,
      total: uniqueResults.length,
      results: uniqueResults.slice(0, limit)
    });

  } catch (error: any) {
    console.error('搜索失败:', error);
    res.status(500).json({ error: '搜索失败' });
  }
});

// 获取文档内搜索（指定文档内）
router.get('/document/:documentId', async (req, res) => {
  try {
    const userId = req.headers['x-user-id'] as string;
    const { documentId } = req.params;
    const { q, page } = req.query;

    if (!q) {
      return res.status(400).json({ error: '缺少搜索关键词' });
    }

    // 验证权限
    const { data: doc, error: docError } = await supabase
      .from('documents')
      .select('owner_id, page_count_or_virtual_page_count')
      .eq('id', documentId)
      .single();

    if (docError) throw docError;
    if (doc.owner_id !== userId) {
      return res.status(403).json({ error: '无权访问此文档' });
    }

    // 在页面文本中搜索
    const { data: pages, error: pagesError } = await supabase
      .from('document_pages')
      .select('*')
      .eq('document_id', documentId);

    if (pagesError) throw pagesError;

    const matches: Array<{
      page_index: number;
      snippet: string;
      positions: number[];
    }> = [];

    pages?.forEach(page => {
      const text = page.extracted_text || '';
      const regex = new RegExp(q.toString(), 'gi');
      let match;

      while ((match = regex.exec(text)) !== null) {
        const start = Math.max(0, match.index - 50);
        const end = Math.min(text.length, match.index + q.toString().length + 50);
        matches.push({
          page_index: page.page_index,
          snippet: '...' + text.substring(start, end) + '...',
          positions: [match.index]
        });
      }
    });

    res.json({
      document_id: documentId,
      query: q.toString(),
      total_matches: matches.length,
      matches: matches.slice(0, 50) // 限制返回数量
    });

  } catch (error: any) {
    console.error('文档内搜索失败:', error);
    res.status(500).json({ error: '搜索失败' });
  }
});

export default router;

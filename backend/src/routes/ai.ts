import { Router } from 'express';
import { openai, supabase } from '../index';
import { v4 as uuidv4 } from 'uuid';

const router = Router();

// AI 快速问答（局部上下文）
router.post('/quick-ask', async (req, res) => {
  try {
    const { document_id, selection_text, page_index, question, context_pages } = req.body;
    const userId = req.headers['x-user-id'] as string;

    if (!userId || !selection_text || !question) {
      return res.status(400).json({ error: '缺少必要参数' });
    }

    // 生成线程 ID
    const threadId = uuidv4();

    // 获取当前页及上下文页文本
    const { data: pages, error: pagesError } = await supabase
      .from('document_pages')
      .select('*')
      .eq('document_id', document_id)
      .in('page_index', [
        page_index - 2,
        page_index - 1,
        page_index,
        page_index + 1,
        page_index + 2
      ])
      .order('page_index');

    if (pagesError) throw pagesError;

    // 构建上下文
    let context = `当前文档：${selection_text}\n\n`;
    context += `用户问题：${question}\n\n`;
    context += `相关页面内容：\n`;

    pages.forEach(page => {
      context += `\n--- 第 ${page.page_index} 页 ---\n`;
      context += page.extracted_text || '';
    });

    // 调用 OpenAI API（流式响应）
    const completion = await openai.chat.completions.create({
      model: 'gpt-4-turbo-preview',
      messages: [
        {
          role: 'system',
          content: '你是一个专业的文档阅读助手。基于提供的文档上下文回答用户问题。回答要简洁、准确，并尽可能引用原文。'
        },
        {
          role: 'user',
          content: context
        }
      ],
      stream: true,
      max_tokens: 1000,
      temperature: 0.7
    });

    // 流式输出
    res.setHeader('Content-Type', 'text/event-stream');
    res.setHeader('Cache-Control', 'no-cache');
    res.setHeader('Connection', 'keep-alive');

    for await (const chunk of completion) {
      const content = chunk.choices[0]?.delta?.content || '';
      if (content) {
        res.write(`data: ${JSON.stringify({ thread_id: threadId, content })}\n\n`);
      }
    }

    res.write(`data: ${JSON.stringify({ thread_id: threadId, done: true })}\n\n`);
    res.end();

    // 异步保存线程信息（不阻塞响应）
    saveThreadInfo(threadId, userId, document_id, selection_text, page_index);

  } catch (error: any) {
    console.error('AI 快答失败:', error);
    res.status(500).json({ error: 'AI 问答失败' });
  }
});

// 翻译选中文本
router.post('/translate', async (req, res) => {
  try {
    const { text, target_language = 'zh', context } = req.body;

    if (!text) {
      return res.status(400).json({ error: '缺少文本内容' });
    }

    const prompt = `请将以下文本翻译成${target_language === 'zh' ? '中文' : '英文'}。
如果文本包含专业术语，请保留原文并给出解释性翻译。

${context ? `上下文：${context}\n\n` : ''}
待翻译文本：
${text}

请输出：
1. 翻译结果
2. 关键术语对照（如有）`;

    const completion = await openai.chat.completions.create({
      model: 'gpt-4-turbo-preview',
      messages: [
        {
          role: 'system',
          content: '你是一个专业的翻译助手，擅长学术和技术文档翻译。'
        },
        {
          role: 'user',
          content: prompt
        }
      ],
      temperature: 0.3
    });

    const result = completion.choices[0].message.content;

    res.json({
      translated_text: result,
      original_text: text
    });

  } catch (error: any) {
    console.error('翻译失败:', error);
    res.status(500).json({ error: '翻译失败' });
  }
});

// 总结提炼
router.post('/summarize', async (req, res) => {
  try {
    const { text, style = 'brief' } = req.body;

    if (!text) {
      return res.status(400).json({ error: '缺少文本内容' });
    }

    const stylePrompts = {
      brief: '用一句话总结核心内容',
      detailed: '提供详细的结构化总结',
      notes: '整理成学习笔记格式，包含关键点',
      exam: '提炼可能的重要考点和重点'
    };

    const completion = await openai.chat.completions.create({
      model: 'gpt-4-turbo-preview',
      messages: [
        {
          role: 'system',
          content: `你是一个专业的文档分析助手。${stylePrompts[style] || stylePrompts.brief}。`
        },
        {
          role: 'user',
          content: text
        }
      ],
      temperature: 0.5
    });

    const summary = completion.choices[0].message.content;

    // 提取关键点（简单处理）
    const keyPoints = summary.split('\n').filter(line =>
      line.trim().startsWith('•') ||
      line.trim().startsWith('-') ||
      line.trim().startsWith('*')
    );

    res.json({
      summary,
      key_points: keyPoints,
      style
    });

  } catch (error: any) {
    console.error('总结失败:', error);
    res.status(500).json({ error: '总结失败' });
  }
});

// 深度分析（全文检索）
router.post('/deep-analysis', async (req, res) => {
  try {
    const { document_id, question, max_results = 10 } = req.body;
    const userId = req.headers['x-user-id'] as string;

    if (!userId || !document_id || !question) {
      return res.status(400).json({ error: '缺少必要参数' });
    }

    // 检查文档处理状态
    const { data: doc, error: docError } = await supabase
      .from('documents')
      .select('processing_status')
      .eq('id', document_id)
      .single();

    if (docError) throw docError;

    if (doc.processing_status !== 'completed') {
      return res.status(202).json({
        error: '文档尚未处理完成',
        status: doc.processing_status
      });
    }

    // 生成问题向量
    const embeddingResponse = await openai.embeddings.create({
      model: 'text-embedding-3-small',
      input: question
    });

    const questionEmbedding = embeddingResponse.data[0].embedding;

    // 向量检索相关 chunk
    const { data: chunks, error: searchError } = await supabase.rpc('match_document_chunks', {
      query_embedding: questionEmbedding,
      match_threshold: 0.7,
      match_count: max_results,
      p_document_id: document_id
    });

    if (searchError) throw searchError;

    // 构建上下文
    let context = `基于以下文档片段回答问题：\n\n`;
    chunks.forEach((chunk: any, idx: number) => {
      context += `[片段 ${idx + 1}] 第${chunk.page_start || '?'}-${chunk.page_end || '?'}页：\n`;
      context += chunk.chunk_text.substring(0, 1000) + '\n\n';
    });

    // 生成答案
    const completion = await openai.chat.completions.create({
      model: 'gpt-4-turbo-preview',
      messages: [
        {
          role: 'system',
          content: '你是一个专业的文档分析助手。基于提供的文档片段回答问题，并标注信息来源的页码。'
        },
        {
          role: 'user',
          content: `${context}\n\n用户问题：${question}\n\n请提供详细回答，并在回答中标注信息来源（页码）。`
        }
      ],
      temperature: 0.7,
      max_tokens: 2000
    });

    const answer = completion.choices[0].message.content;

    res.json({
      answer,
      sources: chunks.map((chunk: any) => ({
        page_start: chunk.page_start,
        page_end: chunk.page_end,
        quote: chunk.chunk_text.substring(0, 200),
        relevance: chunk.similarity
      }))
    });

  } catch (error: any) {
    console.error('深度分析失败:', error);
    res.status(500).json({ error: '分析失败' });
  }
});

// 保存归档
router.post('/archive', async (req, res) => {
  try {
    const { document_id, page_index, quote, question, answer, tag } = req.body;
    const userId = req.headers['x-user-id'] as string;

    if (!userId || !document_id) {
      return res.status(401).json({ error: '未授权' });
    }

    const { data, error } = await supabase
      .from('archives')
      .insert({
        document_id,
        page_index_or_virtual_page_index: page_index,
        quote,
        highlight_areas_or_anchor: {},
        question,
        answer,
        tag
      })
      .select()
      .single();

    if (error) throw error;

    res.status(201).json(data);
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

// 获取用户归档列表
router.get('/archive/list', async (req, res) => {
  try {
    const userId = req.headers['x-user-id'] as string;
    if (!userId) {
      return res.status(401).json({ error: '未授权' });
    }

    const { data, error } = await supabase
      .from('archives')
      .select(`
        *,
        documents (id, title, file_type)
      `)
      .in('document_id', supabase
        .from('documents')
        .select('id')
        .eq('owner_id', userId)
      )
      .order('created_at', { ascending: false });

    if (error) throw error;
    res.json(data);
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

// 辅助函数：保存线程信息
async function saveThreadInfo(
  threadId: string,
  userId: string,
  documentId: string,
  selectionText: string,
  pageIndex: number
) {
  // TODO: 实现线程存储（可以使用 Redis 或数据库）
  console.log(`Thread ${threadId} created for user ${userId}, document ${documentId}, page ${pageIndex}`);
}

export default router;

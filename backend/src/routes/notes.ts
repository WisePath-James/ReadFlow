import { Router } from 'express';
import { supabase } from '../index';

const router = Router();

// 创建笔记
router.post('/', async (req, res) => {
  try {
    const userId = req.headers['x-user-id'] as string;
    const { document_id, page_index, quote, areas, note_text } = req.body;

    if (!userId || !document_id || !quote || !note_text) {
      return res.status(400).json({ error: '缺少必要参数' });
    }

    // 验证文档所有权
    const { data: doc, error: docError } = await supabase
      .from('documents')
      .select('owner_id')
      .eq('id', document_id)
      .single();

    if (docError) throw docError;
    if (doc.owner_id !== userId) {
      return res.status(403).json({ error: '无权访问此文档' });
    }

    const { data, error } = await supabase
      .from('notes')
      .insert({
        document_id,
        page_index_or_virtual_page_index: page_index,
        quote,
        highlight_areas_or_anchor: areas || {},
        note_text
      })
      .select()
      .single();

    if (error) throw error;
    res.status(201).json(data);
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

// 获取文档的所有笔记
router.get('/document/:documentId', async (req, res) => {
  try {
    const userId = req.headers['x-user-id'] as string;
    const { documentId } = req.params;

    // 验证权限
    const { data: doc, error: docError } = await supabase
      .from('documents')
      .select('owner_id')
      .eq('id', documentId)
      .single();

    if (docError) throw docError;
    if (doc.owner_id !== userId) {
      return res.status(403).json({ error: '无权访问此文档' });
    }

    const { data, error } = await supabase
      .from('notes')
      .select('*')
      .eq('document_id', documentId)
      .order('created_at', { ascending: false });

    if (error) throw error;
    res.json(data);
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

// 更新笔记
router.patch('/:id', async (req, res) => {
  try {
    const userId = req.headers['x-user-id'] as string;
    const { id } = req.params;
    const updates = req.body;

    // 验证权限
    const { data: note, error: noteError } = await supabase
      .from('notes')
      .select('document_id')
      .eq('id', id)
      .single();

    if (noteError) throw noteError;

    const { data: doc, error: docError } = await supabase
      .from('documents')
      .select('owner_id')
      .eq('id', note.document_id)
      .single();

    if (docError) throw docError;
    if (doc.owner_id !== userId) {
      return res.status(403).json({ error: '无权修改此笔记' });
    }

    const { data, error } = await supabase
      .from('notes')
      .update(updates)
      .eq('id', id)
      .select()
      .single();

    if (error) throw error;
    res.json(data);
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

// 删除笔记
router.delete('/:id', async (req, res) => {
  try {
    const userId = req.headers['x-user-id'] as string;
    const { id } = req.params;

    // ���证权限
    const { data: note, error: noteError } = await supabase
      .from('notes')
      .select('document_id')
      .eq('id', id)
      .single();

    if (noteError) throw noteError;

    const { data: doc, error: docError } = await supabase
      .from('documents')
      .select('owner_id')
      .eq('id', note.document_id)
      .single();

    if (docError) throw docError;
    if (doc.owner_id !== userId) {
      return res.status(403).json({ error: '无权删除此笔记' });
    }

    const { error: deleteError } = await supabase
      .from('notes')
      .delete()
      .eq('id', id);

    if (deleteError) throw deleteError;
    res.json({ message: '笔记已删除' });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

export default router;

import { Router } from 'express';
import { supabase } from '../index';

const router = Router();

// 创建标注
router.post('/', async (req, res) => {
  try {
    const userId = req.headers['x-user-id'] as string;
    const { document_id, page_index, type, color, quote, areas } = req.body;

    if (!userId || !document_id) {
      return res.status(401).json({ error: '未授权' });
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
      .from('annotations')
      .insert({
        document_id,
        page_index_or_virtual_page_index: page_index,
        type,
        color,
        quote,
        highlight_areas_or_anchor: areas
      })
      .select()
      .single();

    if (error) throw error;
    res.status(201).json(data);
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

// 获取文档的所有标注
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
      .from('annotations')
      .select('*')
      .eq('document_id', documentId)
      .order('page_index_or_virtual_page_index');

    if (error) throw error;
    res.json(data);
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

// 删除标注
router.delete('/:id', async (req, res) => {
  try {
    const userId = req.headers['x-user-id'] as string;
    const { id } = req.params;

    // 获取标注所属文档
    const { data: annotation, error: annoError } = await supabase
      .from('annotations')
      .select('document_id')
      .eq('id', id)
      .single();

    if (annoError) throw annoError;

    // 验证权限
    const { data: doc, error: docError } = await supabase
      .from('documents')
      .select('owner_id')
      .eq('id', annotation.document_id)
      .single();

    if (docError) throw docError;
    if (doc.owner_id !== userId) {
      return res.status(403).json({ error: '无权删除此标注' });
    }

    const { error: deleteError } = await supabase
      .from('annotations')
      .delete()
      .eq('id', id);

    if (deleteError) throw deleteError;
    res.json({ message: '标注已删除' });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

export default router;

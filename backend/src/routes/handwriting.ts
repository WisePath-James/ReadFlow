import { Router } from 'express';
import { supabase } from '../index';

const router = Router();

// 创建手写批注
router.post('/', async (req, res) => {
  try {
    const userId = req.headers['x-user-id'] as string;
    const { document_id, page_index, drawing_data, tool_type, color, width, anchor_meta } = req.body;

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
      .from('handwriting_annotations')
      .insert({
        document_id,
        page_index_or_virtual_page_index: page_index,
        drawing_data,
        anchor_meta: anchor_meta || {},
        tool_type,
        color,
        width
      })
      .select()
      .single();

    if (error) throw error;
    res.status(201).json(data);
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

// 获取文档的手写批注
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
      .from('handwriting_annotations')
      .select('*')
      .eq('document_id', documentId)
      .order('created_at', { ascending: false });

    if (error) throw error;
    res.json(data);
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

// 删除手写批注
router.delete('/:id', async (req, res) => {
  try {
    const userId = req.headers['x-user-id'] as string;
    const { id } = req.params;

    // 验证权限
    const { data: hw, error: hwError } = await supabase
      .from('handwriting_annotations')
      .select('document_id')
      .eq('id', id)
      .single();

    if (hwError) throw hwError;

    const { data: doc, error: docError } = await supabase
      .from('documents')
      .select('owner_id')
      .eq('id', hw.document_id)
      .single();

    if (docError) throw docError;
    if (doc.owner_id !== userId) {
      return res.status(403).json({ error: '无权删除此批注' });
    }

    const { error: deleteError } = await supabase
      .from('handwriting_annotations')
      .delete()
      .eq('id', id);

    if (deleteError) throw deleteError;
    res.json({ message: '手写批注已删除' });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

// 更新手写批注
router.patch('/:id', async (req, res) => {
  try {
    const userId = req.headers['x-user-id'] as string;
    const { id } = req.params;
    const updates = req.body;

    // 验证权限
    const { data: hw, error: hwError } = await supabase
      .from('handwriting_annotations')
      .select('document_id')
      .eq('id', id)
      .single();

    if (hwError) throw hwError;

    const { data: doc, error: docError } = await supabase
      .from('documents')
      .select('owner_id')
      .eq('id', hw.document_id)
      .single();

    if (docError) throw docError;
    if (doc.owner_id !== userId) {
      return res.status(403).json({ error: '无权修改此批注' });
    }

    const { data, error } = await supabase
      .from('handwriting_annotations')
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

export default router;

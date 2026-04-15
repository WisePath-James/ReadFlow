import { Router } from 'express';
import { supabase } from '../index';

const router = Router();

// 保存阅读进度
router.post('/', async (req, res) => {
  try {
    const userId = req.headers['x-user-id'] as string;
    const { document_id, page_index, scroll_offset, zoom_level, reading_mode, theme } = req.body;

    if (!userId || !document_id || page_index === undefined) {
      return res.status(400).json({ error: '缺少必要参数' });
    }

    const { data, error } = await supabase
      .from('reading_progress')
      .upsert({
        user_id: userId,
        document_id,
        current_page_or_virtual_page: page_index,
        scroll_offset: scroll_offset || 0,
        zoom_level: zoom_level || 1.0,
        reading_mode: reading_mode || 'original',
        theme: theme || 'light',
        last_read_at: new Date().toISOString()
      }, {
        onConflict: 'user_id,document_id'
      })
      .select()
      .single();

    if (error) throw error;
    res.json(data);
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

// 获取阅读进度
router.get('/document/:documentId', async (req, res) => {
  try {
    const userId = req.headers['x-user-id'] as string;
    const { documentId } = req.params;

    if (!userId) {
      return res.status(401).json({ error: '未授权' });
    }

    const { data, error } = await supabase
      .from('reading_progress')
      .select('*')
      .eq('user_id', userId)
      .eq('document_id', documentId)
      .single();

    if (error && error.code !== 'PGRST116') { // PGRST116 = not found
      throw error;
    }

    res.json(data || null);
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

// 批量获取阅读进度（最近阅读列表）
router.get('/recent', async (req, res) => {
  try {
    const userId = req.headers['x-user-id'] as string;
    const limit = parseInt(req.query.limit as string) || 10;

    if (!userId) {
      return res.status(401).json({ error: '未授权' });
    }

    const { data, error } = await supabase
      .from('reading_progress')
      .select(`
        *,
        documents (id, title, file_type, file_path)
      `)
      .eq('user_id', userId)
      .order('last_read_at', { ascending: false })
      .limit(limit);

    if (error) throw error;
    res.json(data);
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

// 删除阅读进度
router.delete('/document/:documentId', async (req, res) => {
  try {
    const userId = req.headers['x-user-id'] as string;
    const { documentId } = req.params;

    if (!userId) {
      return res.status(401).json({ error: '未授权' });
    }

    const { error } = await supabase
      .from('reading_progress')
      .delete()
      .eq('user_id', userId)
      .eq('document_id', documentId);

    if (error) throw error;
    res.json({ message: '阅读进度已删除' });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

export default router;

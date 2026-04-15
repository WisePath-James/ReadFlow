import { Router } from 'express';
import { supabase } from '../index';
import multer from 'multer';
import path from 'path';
import fs from 'fs';
import { v4 as uuidv4 } from 'uuid';
import { DocumentUploadRequest, DocumentUploadResponse, ProcessingProgress } from '../types';

const router = Router();

// 配置 multer 用于文件上传
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    const uploadDir = path.join(process.cwd(), 'uploads');
    if (!fs.existsSync(uploadDir)) {
      fs.mkdirSync(uploadDir, { recursive: true });
    }
    cb(null, uploadDir);
  },
  filename: (req, file, cb) => {
    const ext = path.extname(file.originalname);
    cb(null, `${uuidv4()}${ext}`);
  }
});

const upload = multer({
  storage,
  limits: { fileSize: 100 * 1024 * 1024 }, // 100MB
  fileFilter: (req, file, cb) => {
    const allowedTypes = [
      'application/pdf',
      'application/msword',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      'application/epub+zip',
      'text/plain',
      'text/markdown',
      'text/html',
      'application/rtf',
      'text/xml',
      'application/json'
    ];
    if (allowedTypes.includes(file.mimetype)) {
      cb(null, true);
    } else {
      cb(new Error('不支持的文件类型'));
    }
  }
});

// 获取用户文档列表
router.get('/', async (req, res) => {
  try {
    const { data, error } = await supabase
      .from('documents')
      .select(`
        *,
        folders (id, name, color, icon),
        document_pages (count)
      `)
      .order('updated_at', { ascending: false });

    if (error) throw error;
    res.json(data);
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

// 获取文档详情
router.get('/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const { data, error } = await supabase
      .from('documents')
      .select(`
        *,
        folders (id, name, color, icon),
        document_pages (*),
        document_blocks (*)
      `)
      .eq('id', id)
      .single();

    if (error) throw error;
    res.json(data);
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

// 上传文档
router.post('/', upload.single('file'), async (req, res) => {
  try {
    const userId = req.headers['x-user-id'] as string;
    if (!userId) {
      return res.status(401).json({ error: '未授权' });
    }

    const file = req.file;
    if (!file) {
      return res.status(400).json({ error: '未选择文件' });
    }

    // 解析文档元信息
    const fileExt = path.extname(file.originalname).toLowerCase();
    const fileType = getFileType(fileExt);

    // 创建文档记录
    const { data: document, error: docError } = await supabase
      .from('documents')
      .insert({
        owner_id: userId,
        title: file.originalname.replace(/\.[^/.]+$/, ''),
        file_path: file.path,
        file_type: fileType,
        processing_status: 'pending'
      })
      .select()
      .single();

    if (docError) throw docError;

    // 触发文档处理任务（异步）
    processDocument(document.id);

    const response: DocumentUploadResponse = {
      id: document.id,
      title: document.title,
      status: 'pending',
      message: '文档上传成功，正在后台处理'
    };

    res.status(201).json(response);
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

// 获取文档处理进度
router.get('/:id/progress', async (req, res) => {
  try {
    const { id } = req.params;
    const { data, error } = await supabase
      .from('documents')
      .select('processing_status')
      .eq('id', id)
      .single();

    if (error) throw error;

    res.json({
      document_id: id,
      status: data.processing_status,
      progress: data.processing_status === 'completed' ? 100 : 0
    });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

// 删除文档
router.delete('/:id', async (req, res) => {
  try {
    const { id } = req.params;

    // 获取文件路径
    const { data: doc } = await supabase
      .from('documents')
      .select('file_path')
      .eq('id', id)
      .single();

    if (doc) {
      // 删除物理文件
      if (fs.existsSync(doc.file_path)) {
        fs.unlinkSync(doc.file_path);
      }
    }

    // 删除数据库记录（级联删除所有关联数据）
    const { error } = await supabase
      .from('documents')
      .delete()
      .eq('id', id);

    if (error) throw error;
    res.json({ message: '文档已删除' });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

// 更新文档元信息
router.patch('/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const updates = req.body;

    const { data, error } = await supabase
      .from('documents')
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

// 辅助函数：获取文件类型
function getFileType(ext: string): string {
  const typeMap: { [key: string]: string } = {
    '.pdf': 'pdf',
    '.doc': 'doc',
    '.docx': 'docx',
    '.epub': 'epub',
    '.txt': 'txt',
    '.md': 'markdown',
    '.html': 'html',
    '.htm': 'html',
    '.rtf': 'rtf',
    '.xml': 'xml',
    '.json': 'json'
  };
  return typeMap[ext] || 'unknown';
}

// 文档处理任务（实际实现应使用队列系统，如 BullMQ）
async function processDocument(documentId: string) {
  try {
    // 更新为处理中状态
    await supabase
      .from('documents')
      .update({ processing_status: 'processing' })
      .eq('id', documentId);

    // TODO: 调用文档处理服务
    // 1. 解析文档内容
    // 2. 提取文本和结构
    // 3. 生成缩略图
    // 4. 切分 chunk
    // 5. 生成 embedding
    // 6. 保存到数据库

    // 模拟处理完成
    await supabase
      .from('documents')
      .update({
        processing_status: 'completed',
        page_count_or_virtual_page_count: 1
      })
      .eq('id', documentId);

  } catch (error) {
    console.error('文档处理失败:', error);
    await supabase
      .from('documents')
      .update({ processing_status: 'failed' })
      .eq('id', documentId);
  }
}

export default router;

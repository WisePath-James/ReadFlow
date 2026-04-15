import dotenv from 'dotenv';
import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import { createClient } from '@supabase/supabase-js';
import { OpenAI } from 'openai';

// 加载环境变量
dotenv.config();

// 初始化 Express
const app = express();
const PORT = process.env.PORT || 3000;

// 中间件
app.use(helmet());
app.use(cors());
app.use(morgan('combined'));
app.use(express.json());

// 初始化 Supabase
export const supabase = createClient(
  process.env.SUPABASE_URL!,
  process.env.SUPABASE_ANON_KEY!
);

// 初始化 OpenAI
export const openai = new OpenAI({
  apiKey: process.env.OPENAI_API_KEY,
});

// 健康检查
app.get('/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// API 路由
app.use('/api/documents', require('./routes/documents'));
app.use('/api/ai', require('./routes/ai'));
app.use('/api/annotations', require('./routes/annotations'));
app.use('/api/progress', require('./routes/progress'));
app.use('/api/search', require('./routes/search'));

// 错误处理中间件
app.use((err: any, req: express.Request, res: express.Response, next: express.NextFunction) => {
  console.error(err.stack);
  res.status(500).json({
    error: '内部服务器错误',
    message: process.env.NODE_ENV === 'development' ? err.message : undefined
  });
});

// 404 处理
app.use('*', (req, res) => {
  res.status(404).json({ error: '接口不存在' });
});

// 启动服务器
app.listen(PORT, () => {
  console.log(`🚀 ReadFlow 后端服务运行在 http://localhost:${PORT}`);
  console.log(`📚 环境: ${process.env.NODE_ENV || 'development'}`);
});

export default app;

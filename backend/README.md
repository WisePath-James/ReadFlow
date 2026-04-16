# ReadFlow Backend

Node.js backend service for ReadFlow AI Document Reading System.

## Quick Start

```bash
cd backend
npm install
npm start
```

Server runs on http://localhost:3000

## API Endpoints

### Document Management
- `POST /api/documents/upload` - Upload PDF document
- `GET /api/documents` - List all documents
- `GET /api/documents/:id` - Get document info and text
- `GET /api/documents/:id/page/:pageNum` - Get specific page text
- `GET /api/documents/:id/search?q=query` - Search document text
- `DELETE /api/documents/:id` - Delete document

### AI Services
- `POST /api/ai/quick-ask` - Quick ask with local context
- `POST /api/ai/deep-analysis` - Deep document analysis

### Health
- `GET /api/health` - Health check

## Environment Variables

Copy `.env.example` to `.env` and configure:
- `PORT` - Server port (default: 3000)
- `AI_PROVIDER` - AI provider (mock/openai)
- `AI_API_KEY` - OpenAI API key

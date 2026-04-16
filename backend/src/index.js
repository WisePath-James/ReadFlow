import express from 'express';
import cors from 'cors';
import multer from 'multer';
import { v4 as uuidv4 } from 'uuid';
import pdfParse from 'pdf-parse';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json());
app.use('/uploads', express.static(path.join(__dirname, 'uploads')));

// File upload configuration
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    const uploadDir = path.join(__dirname, 'uploads');
    if (!fs.existsSync(uploadDir)) {
      fs.mkdirSync(uploadDir, { recursive: true });
    }
    cb(null, uploadDir);
  },
  filename: (req, file, cb) => {
    const uniqueName = `${uuidv4()}-${file.originalname}`;
    cb(null, uniqueName);
  }
});

const upload = multer({ 
  storage,
  limits: { fileSize: 100 * 1024 * 1024 } // 100MB max
});

// In-memory document store
const documents = new Map();
const chunks = new Map();

// AI Configuration (simulated - replace with actual AI API)
const AI_CONFIG = {
  provider: process.env.AI_PROVIDER || 'mock',
  apiKey: process.env.AI_API_KEY || '',
  baseUrl: process.env.AI_BASE_URL || 'https://api.openai.com/v1'
};

// ==================== PDF Processing ====================

app.post('/api/documents/upload', upload.single('file'), async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ error: 'No file uploaded' });
    }

    const filePath = req.file.path;
    const fileBuffer = fs.readFileSync(filePath);
    
    let pdfData;
    try {
      pdfData = await pdfParse(fileBuffer);
    } catch (parseError) {
      // If PDF parsing fails, return basic document info
      pdfData = { text: '', numpages: 1 };
    }

    const document = {
      id: uuidv4(),
      filename: req.file.originalname,
      filepath: req.file.filename,
      filesize: req.file.size,
      numpages: pdfData.numpages || 1,
      text: pdfData.text || '',
      createdAt: new Date().toISOString(),
      processed: true,
      chunks: []
    };

    // Create text chunks for search
    if (document.text && document.text.length > 0) {
      const chunkSize = 1000;
      const textChunks = [];
      
      for (let i = 0; i < document.text.length; i += chunkSize) {
        const chunkText = document.text.slice(i, i + chunkSize);
        const chunk = {
          id: uuidv4(),
          documentId: document.id,
          pageStart: Math.floor(i / chunkSize) + 1,
          pageEnd: Math.floor(i / chunkSize) + 1,
          text: chunkText,
          index: textChunks.length
        };
        textChunks.push(chunk);
      }
      
      document.chunks = textChunks;
      chunks.set(document.id, textChunks);
    }

    documents.set(document.id, document);

    res.json({
      success: true,
      document: {
        id: document.id,
        filename: document.filename,
        numpages: document.numpages,
        filesize: document.filesize,
        processed: document.processed
      }
    });
  } catch (error) {
    console.error('Upload error:', error);
    res.status(500).json({ error: 'Failed to process document' });
  }
});

// Get document info and text
app.get('/api/documents/:id', async (req, res) => {
  try {
    const doc = documents.get(req.params.id);
    if (!doc) {
      return res.status(404).json({ error: 'Document not found' });
    }

    res.json({
      id: doc.id,
      filename: doc.filename,
      numpages: doc.numpages,
      text: doc.text,
      chunks: doc.chunks.map(c => ({ id: c.id, pageStart: c.pageStart, pageEnd: c.pageEnd, index: c.index }))
    });
  } catch (error) {
    res.status(500).json({ error: 'Failed to get document' });
  }
});

// Get page text
app.get('/api/documents/:id/page/:pageNum', async (req, res) => {
  try {
    const doc = documents.get(req.params.id);
    if (!doc) {
      return res.status(404).json({ error: 'Document not found' });
    }

    const pageNum = parseInt(req.params.pageNum);
    const pageChunks = doc.chunks.filter(c => c.pageStart === pageNum);

    res.json({
      pageNum,
      text: pageChunks.map(c => c.text).join('\n'),
      hasText: pageChunks.length > 0
    });
  } catch (error) {
    res.status(500).json({ error: 'Failed to get page' });
  }
});

// Search document text
app.get('/api/documents/:id/search', async (req, res) => {
  try {
    const doc = documents.get(req.params.id);
    if (!doc) {
      return res.status(404).json({ error: 'Document not found' });
    }

    const query = req.query.q?.toLowerCase() || '';
    if (!query) {
      return res.json({ results: [] });
    }

    const results = [];
    const text = doc.text.toLowerCase();
    let index = 0;

    while (true) {
      const foundIndex = text.indexOf(query, index);
      if (foundIndex === -1) break;

      // Find approximate page number
      const charPerPage = doc.text.length / doc.numpages;
      const pageNum = Math.floor(foundIndex / charPerPage) + 1;

      // Get surrounding context
      const start = Math.max(0, foundIndex - 50);
      const end = Math.min(doc.text.length, foundIndex + query.length + 50);
      const context = doc.text.slice(start, end);

      results.push({
        text: context,
        position: foundIndex,
        pageNum,
        charIndex: foundIndex
      });

      index = foundIndex + 1;

      if (results.length >= 50) break; // Limit results
    }

    res.json({ results });
  } catch (error) {
    res.status(500).json({ error: 'Search failed' });
  }
});

// Delete document
app.delete('/api/documents/:id', async (req, res) => {
  try {
    const doc = documents.get(req.params.id);
    if (doc && doc.filepath) {
      const filePath = path.join(__dirname, 'uploads', doc.filepath);
      if (fs.existsSync(filePath)) {
        fs.unlinkSync(filePath);
      }
    }
    
    documents.delete(req.params.id);
    chunks.delete(req.params.id);

    res.json({ success: true });
  } catch (error) {
    res.status(500).json({ error: 'Failed to delete document' });
  }
});

// List documents
app.get('/api/documents', async (req, res) => {
  try {
    const docs = Array.from(documents.values()).map(d => ({
      id: d.id,
      filename: d.filename,
      numpages: d.numpages,
      filesize: d.filesize,
      processed: d.processed,
      createdAt: d.createdAt
    }));

    res.json({ documents: docs });
  } catch (error) {
    res.status(500).json({ error: 'Failed to list documents' });
  }
});

// ==================== AI Quick Ask ====================

// Quick ask with local context
app.post('/api/ai/quick-ask', async (req, res) => {
  try {
    const { 
      selection, 
      currentPageText, 
      prevPagesText, 
      nextPagesText,
      pageNum,
      documentTitle,
      question,
      requestType
    } = req.body;

    // Build context for AI
    const context = buildQuickAskContext({
      selection,
      currentPageText,
      prevPagesText,
      nextPagesText,
      pageNum,
      documentTitle,
      question,
      requestType
    });

    // Call AI (simulated)
    const response = await generateAIResponse(context, requestType);

    res.json({
      success: true,
      response: response.text,
      sources: response.sources || []
    });
  } catch (error) {
    console.error('Quick ask error:', error);
    res.status(500).json({ error: 'AI request failed' });
  }
});

// Deep analysis
app.post('/api/ai/deep-analysis', async (req, res) => {
  try {
    const { documentId, question } = req.body;

    const doc = documents.get(documentId);
    if (!doc) {
      return res.status(404).json({ error: 'Document not found' });
    }

    // Semantic search simulation
    const query = question.toLowerCase();
    const relevantChunks = doc.chunks.filter(chunk => 
      chunk.text.toLowerCase().includes(query) ||
      query.split(' ').some(word => word.length > 3 && chunk.text.toLowerCase().includes(word))
    ).slice(0, 10);

    const context = relevantChunks.map(c => 
      `[Page ${c.pageStart}]: ${c.text}`
    ).join('\n\n');

    const response = await generateAIResponse({
      question,
      context: context || doc.text.slice(0, 5000),
      mode: 'deep-analysis'
    }, 'deep-analysis');

    res.json({
      success: true,
      response: response.text,
      sources: relevantChunks.map(c => ({
        page: c.pageStart,
        text: c.text.slice(0, 200) + '...'
      }))
    });
  } catch (error) {
    res.status(500).json({ error: 'Deep analysis failed' });
  }
});

// ==================== AI Helper Functions ====================

function buildQuickAskContext({ selection, currentPageText, prevPagesText, nextPagesText, pageNum, documentTitle, question, requestType }) {
  let context = '';

  if (documentTitle) {
    context += `Document: ${documentTitle}\n`;
  }
  if (pageNum) {
    context += `Current Page: ${pageNum}\n\n`;
  }

  if (selection) {
    context += `Selected Text:\n"${selection}"\n\n`;
  }

  if (currentPageText) {
    context += `Current Page Content:\n${currentPageText.slice(0, 2000)}\n\n`;
  }

  if (prevPagesText) {
    context += `Previous Pages:\n${prevPagesText.slice(0, 1500)}\n\n`;
  }

  if (nextPagesText) {
    context += `Next Pages:\n${nextPagesText.slice(0, 1500)}\n\n`;
  }

  if (question) {
    context += `Question: ${question}`;
  }

  return context;
}

async function generateAIResponse(context, requestType) {
  // Simulated AI responses for different request types
  const simulatedResponses = {
    'translate': `Based on the selected text, here is the translation:

${context.selection || context.match || 'The selected text'}`,
    'explain': `Here's an explanation of the selected text:

The content discusses an important concept that relates to the broader topic. In this context, it appears to be making a point about a key principle or mechanism.

Key points:
• This represents a fundamental approach or method
• It relates to practical applications in the field
• The context suggests it's part of a larger framework

Would you like me to elaborate on any specific aspect?`,
    'summarize': `Summary of the content:

This passage covers [main topic], which involves [key concepts]. The author explains how [important points] work together to achieve [goal/outcome].

Main takeaways:
1. [First key point]
2. [Second key point]  
3. [Third key point]

For more detailed understanding, you may want to read the surrounding context.`,
    'deep-analysis': `Deep Analysis:

${context.question}

Analysis:
This question relates to multiple aspects of the document. Let me break it down:

**Context Understanding:**
The document discusses [topic], which appears in [context]. This is significant because [reason].

**Key Relationships:**
- [Point A] relates to [Point B] through [mechanism]
- [Point C] demonstrates [principle]

**Detailed Explanation:**
[Comprehensive explanation based on available context]

**Conclusion:**
Based on the document, [answer/summary]`,
    'default': `Based on the provided context, here's my response:

${context.question || context.selection || 'I understand your query. Here is relevant information from the document.'}

The document provides information about [topic]. This appears to be part of a larger discussion on [related topic]. 

Would you like me to:
• Explain this in more detail?
• Find related information in the document?
• Translate or summarize specific sections?
• Answer questions about specific concepts?`
  };

  // Use simulated response (in production, replace with actual AI API call)
  const responseText = simulatedResponses[requestType] || simulatedResponses['default'];

  return {
    text: responseText,
    sources: []
  };
}

// ==================== Health Check ====================

app.get('/api/health', (req, res) => {
  res.json({ 
    status: 'ok', 
    documents: documents.size,
    timestamp: new Date().toISOString()
  });
});

// Start server
app.listen(PORT, () => {
  console.log(`ReadFlow Backend running on http://localhost:${PORT}`);
  console.log(`AI Provider: ${AI_CONFIG.provider}`);
});

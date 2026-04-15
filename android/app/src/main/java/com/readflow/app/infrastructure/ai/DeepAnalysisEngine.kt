package com.readflow.app.infrastructure.ai

import com.readflow.app.domain.model.AIRequest
import com.readflow.app.domain.model.AIResponse
import com.readflow.app.domain.model.AISource
import com.readflow.app.domain.model.DocumentChunk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deep Analysis Engine
 * 全文深度分析引擎
 */
@Singleton
class DeepAnalysisEngine @Inject constructor() {

    /**
     * 执行深度分析
     * 使用语义搜索 + 关键词搜索
     */
    suspend fun analyze(
        documentId: String,
        question: String,
        chunks: List<DocumentChunk>,
        onProgress: (Float) -> Unit = {}
    ): Result<AIResponse> = withContext(Dispatchers.IO) {
        try {
            onProgress(0.1f)

            // Step 1: Query rewrite
            val rewrittenQuery = rewriteQuery(question)
            onProgress(0.2f)

            // Step 2: Semantic search
            val semanticResults = semanticSearch(rewrittenQuery, chunks)
            onProgress(0.4f)

            // Step 3: Keyword search
            val keywordResults = keywordSearch(rewrittenQuery, chunks)
            onProgress(0.5f)

            // Step 4: Merge and rank results
            val mergedResults = mergeResults(semanticResults, keywordResults)
            onProgress(0.7f)

            // Step 5: Generate answer with evidence
            val answer = generateAnswer(question, mergedResults)
            onProgress(0.9f)

            val sources = mergedResults.map { chunk ->
                AISource(
                    pageIndex = chunk.pageStart,
                    quote = chunk.chunkText.take(200),
                    relevance = 0.8f
                )
            }

            onProgress(1.0f)

            Result.success(
                AIResponse(
                    answer = answer,
                    sources = sources,
                    isStreaming = false
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 流式深度分析
     */
    fun analyzeStream(
        documentId: String,
        question: String,
        chunks: List<DocumentChunk>
    ): Flow<String> = flow {
        val result = analyze(documentId, question, chunks)
        result.getOrNull()?.answer?.let { answer ->
            val words = answer.split(" ")
            for (word in words) {
                emit(word + " ")
                kotlinx.coroutines.delay(15)
            }
        }
    }

    /**
     * 查询改写
     */
    private fun rewriteQuery(query: String): String {
        // 简单实现：移除停用词，增强关键词
        val stopWords = setOf("的", "了", "是", "在", "和", "与", "这个", "那个", "什么", "怎么")
        return query.split(" ")
            .filter { it !in stopWords && it.length > 1 }
            .joinToString(" ")
    }

    /**
     * 语义搜索（简化实现）
     * 实际需要调用 embedding API
     */
    private suspend fun semanticSearch(
        query: String,
        chunks: List<DocumentChunk>
    ): List<DocumentChunk> {
        // 简化实现：基于关键词匹配
        val queryWords = query.lowercase().split(" ")
        return chunks
            .filter { chunk ->
                val text = chunk.chunkText.lowercase()
                queryWords.any { text.contains(it) }
            }
            .sortedByDescending { chunk ->
                queryWords.count { chunk.chunkText.lowercase().contains(it) }
            }
            .take(5)
    }

    /**
     * 关键词搜索
     */
    private fun keywordSearch(
        query: String,
        chunks: List<DocumentChunk>
    ): List<DocumentChunk> {
        val keywords = extractKeywords(query)
        return chunks
            .filter { chunk ->
                keywords.any { keyword ->
                    chunk.chunkText.contains(keyword, ignoreCase = true)
                }
            }
            .sortedByDescending { chunk ->
                keywords.count { chunk.chunkText.contains(it, ignoreCase = true) }
            }
            .take(5)
    }

    /**
     * 提取关键词
     */
    private fun extractKeywords(query: String): List<String> {
        return query.split(" ")
            .filter { it.length >= 2 }
            .map { it.lowercase() }
    }

    /**
     * 合并搜索结果
     */
    private fun mergeResults(
        semantic: List<DocumentChunk>,
        keyword: List<DocumentChunk>
    ): List<DocumentChunk> {
        val seen = mutableSetOf<String>()
        val merged = mutableListOf<DocumentChunk>()

        for (chunk in semantic) {
            if (chunk.id !in seen) {
                seen.add(chunk.id)
                merged.add(chunk)
            }
        }

        for (chunk in keyword) {
            if (chunk.id !in seen) {
                seen.add(chunk.id)
                merged.add(chunk)
            }
        }

        return merged.take(5)
    }

    /**
     * 生成带引用的答案
     */
    private suspend fun generateAnswer(
        question: String,
        relevantChunks: List<DocumentChunk>
    ): String {
        if (relevantChunks.isEmpty()) {
            return "抱歉，在文档中没有找到与您问题相关的内容。请尝试重新表述您的问题。"
        }

        val evidence = relevantChunks.joinToString("\n\n") { chunk ->
            "【第 ${chunk.pageStart + 1} 页】${chunk.chunkText.take(300)}..."
        }

        return """
            |**深度分析结果**
            |
            |**您的问题**：$question
            |
            |**相关证据**：
            |$evidence
            |
            |**分析结论**：
            |根据文档内容，这部分主要讨论了与您问题相关的核心内容...
            |
            |（实际接入 AI 服务后将提供更深入的分析）
        """.trimMargin()
    }
}

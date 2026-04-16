package com.readflow.app.infrastructure.ai

import com.readflow.app.domain.model.AIRequest
import com.readflow.app.domain.model.AIResponse
import com.readflow.app.domain.model.AISource
import com.readflow.app.domain.model.DocumentChunk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deep Analysis Engine
 * 全文深度分析引擎
 * 支持后端 API 对接
 */
@Singleton
class DeepAnalysisEngine @Inject constructor() {

    // Backend API base URL
    private var apiBaseUrl: String = "http://10.0.2.2:3000"

    /**
     * 配置后端 API 地址
     */
    fun setApiBaseUrl(url: String) {
        apiBaseUrl = url
    }

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

            // Try backend API first
            try {
                val response = callDeepAnalysisApi(documentId, question)
                onProgress(1.0f)
                return@withContext Result.success(response)
            } catch (e: Exception) {
                // Fallback to local processing
            }

            onProgress(0.2f)

            // Step 1: Query rewrite
            val rewrittenQuery = rewriteQuery(question)
            onProgress(0.3f)

            // Step 2: Semantic search
            val semanticResults = semanticSearch(rewrittenQuery, chunks)
            onProgress(0.5f)

            // Step 3: Keyword search
            val keywordResults = keywordSearch(rewrittenQuery, chunks)
            onProgress(0.6f)

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
     * 调用深度分析 API
     */
    private suspend fun callDeepAnalysisApi(documentId: String, question: String): AIResponse = 
        withContext(Dispatchers.IO) {
        try {
            val url = URL("$apiBaseUrl/api/ai/deep-analysis")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 60000

            val requestBody = """
                {
                    "documentId": "$documentId",
                    "question": "${escapeJson(question)}"
                }
            """.trimIndent()

            connection.outputStream.use { os ->
                os.write(requestBody.toByteArray())
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.use { inputStream ->
                    val response = inputStream.bufferedReader().readText()
                    parseDeepAnalysisResponse(response)
                }
            } else {
                throw Exception("API error: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * 解析深度分析响应
     */
    private fun parseDeepAnalysisResponse(json: String): AIResponse {
        val responseKey = "\"response\":"
        val index = json.indexOf(responseKey)
        
        val answer = if (index >= 0) {
            val start = json.indexOf("\"", index + responseKey.length) + 1
            var end = start
            var inString = true
            var escaped = false
            while (end < json.length && inString) {
                val c = json[end]
                when {
                    escaped -> escaped = false
                    c == '\\' -> escaped = true
                    c == '"' && !escaped -> inString = false
                }
                end++
            }
            if (start < end && end <= json.length) {
                json.substring(start, end - 1).replace("\\\"", "\"").replace("\\n", "\n")
            } else {
                ""
            }
        } else {
            ""
        }

        // Parse sources
        val sources = mutableListOf<AISource>()
        val sourcesKey = "\"sources\":"
        val sourcesIndex = json.indexOf(sourcesKey)
        if (sourcesIndex >= 0) {
            // Simple sources parsing
            val sourcesStart = json.indexOf("[", sourcesIndex)
            val sourcesEnd = json.indexOf("]", sourcesStart)
            if (sourcesStart >= 0 && sourcesEnd > sourcesStart) {
                val sourcesJson = json.substring(sourcesStart, sourcesEnd + 1)
                // Parse each source object
                var objStart = sourcesJson.indexOf("{")
                while (objStart >= 0) {
                    val objEnd = sourcesJson.indexOf("}", objStart)
                    if (objEnd > objStart) {
                        val obj = sourcesJson.substring(objStart, objEnd + 1)
                        // Extract page and text
                        val pageMatch = Regex("\"page\":\\s*(\\d+)").find(obj)
                        val textMatch = Regex("\"text\":\\s*\"([^\"]+)\"").find(obj)
                        
                        if (pageMatch != null && textMatch != null) {
                            sources.add(
                                AISource(
                                    pageIndex = pageMatch.groupValues[1].toInt() - 1,
                                    quote = textMatch.groupValues[1].take(200),
                                    relevance = 0.8f
                                )
                            )
                        }
                        objStart = sourcesJson.indexOf("{", objEnd)
                    } else {
                        break
                    }
                }
            }
        }

        return AIResponse(
            answer = answer,
            sources = sources,
            isStreaming = false
        )
    }

    /**
     * 转义 JSON 字符串
     */
    private fun escapeJson(str: String): String {
        return str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
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
        val stopWords = setOf("的", "了", "是", "在", "和", "与", "这个", "那个", "什么", "怎么", "the", "a", "an", "is", "are", "was", "were")
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
    ): List<DocumentChunk> = withContext(Dispatchers.Default) {
        val queryWords = query.lowercase().split(" ").filter { it.length > 2 }
        chunks
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
    ): String = withContext(Dispatchers.Default) {
        if (relevantChunks.isEmpty()) {
            return@withContext "抱歉，在文档中没有找到与您问题相关的内容。请尝试重新表述您的问题，或确保文档已正确处理。"
        }

        val evidence = relevantChunks.joinToString("\n\n") { chunk ->
            "【第 ${chunk.pageStart + 1} 页】${chunk.chunkText.take(300)}..."
        }

        """
            |**深度分析结果**
            |
            |**您的问题**：$question
            |
            |**相关证据**：
            |$evidence
            |
            |**分析结论**：
            |根据文档内容，这部分主要讨论了与您问题相关的核心内容。
            |
            |相关证据来自 ${relevantChunks.size} 个不同位置，展示了文档对该主题的讨论。
            |
            |点击上方的引用可以直接跳转到原文位置查看完整上下文。
        """.trimMargin()
    }
}

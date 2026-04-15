package com.readflow.app.infrastructure.ai

import com.readflow.app.domain.model.AIMessage
import com.readflow.app.domain.model.AIRequest
import com.readflow.app.domain.model.AIRequestType
import com.readflow.app.domain.model.AIResponse
import com.readflow.app.domain.model.AIThread
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Quick Ask Engine
 * 局部阅读问答引擎
 */
@Singleton
class QuickAskEngine @Inject constructor() {

    private val _currentThread = MutableStateFlow<AIThread?>(null)
    val currentThread = _currentThread.asStateFlow()

    private val _messages = MutableStateFlow<List<AIMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming = _isStreaming.asStateFlow()

    /**
     * 创建新的快答线程
     */
    fun createThread(
        documentId: String,
        pageIndex: Int,
        selectionText: String
    ): AIThread {
        val thread = AIThread(
            id = UUID.randomUUID().toString(),
            documentId = documentId,
            pageIndex = pageIndex,
            selectionHash = selectionText.hashCode().toString(),
            selectionText = selectionText
        )
        _currentThread.value = thread
        _messages.value = emptyList()
        return thread
    }

    /**
     * 发送快答请求
     */
    suspend fun ask(
        request: AIRequest,
        onToken: suspend (String) -> Unit
    ): Result<AIResponse> {
        _isStreaming.value = true

        return try {
            val answer = buildAnswer(request)
            val response = AIResponse(
                answer = answer,
                sources = emptyList(),
                isStreaming = true
            )

            // Simulate streaming
            val words = answer.split(" ")
            val builder = StringBuilder()
            for (word in words) {
                builder.append(word).append(" ")
                onToken(builder.toString())
                kotlinx.coroutines.delay(20) // Simulate token delay
            }

            // 保存消息
            val assistantMessage = AIMessage(
                id = UUID.randomUUID().toString(),
                threadId = _currentThread.value?.id ?: "",
                role = com.readflow.app.domain.model.AIRole.ASSISTANT,
                content = answer
            )
            _messages.value = _messages.value + assistantMessage

            Result.success(response.copy(isStreaming = false))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            _isStreaming.value = false
        }
    }

    /**
     * 追问
     */
    suspend fun followUp(
        question: String,
        onToken: suspend (String) -> Unit
    ): Result<AIResponse> {
        val thread = _currentThread.value
            ?: return Result.failure(Exception("No active thread"))

        if (System.currentTimeMillis() > thread.expiresAt) {
            return Result.failure(Exception("Thread expired"))
        }

        // 保存用户消息
        val userMessage = AIMessage(
            id = UUID.randomUUID().toString(),
            threadId = thread.id,
            role = com.readflow.app.domain.model.AIRole.USER,
            content = question
        )
        _messages.value = _messages.value + userMessage

        // 生成回答
        val response = AIResponse(
            answer = "基于当前上下文，我理解您的问题是：$question\n\n这是一个追问的回答示例。",
            isStreaming = true
        )

        return Result.success(response)
    }

    /**
     * 关闭当前线程
     */
    fun closeThread() {
        _currentThread.value = null
        _messages.value = emptyList()
    }

    /**
     * 检查线程是否过期
     */
    fun isThreadExpired(): Boolean {
        val thread = _currentThread.value ?: return true
        return System.currentTimeMillis() > thread.expiresAt
    }

    /**
     * 根据请求类型构建回答
     */
    private fun buildAnswer(request: AIRequest): String {
        val selection = request.selectionText

        return when (request.type) {
            AIRequestType.TRANSLATE -> "**翻译**\n\n原文：$selection\n\n（AI 翻译结果将根据实际接入的 AI 服务返回）"

            AIRequestType.EXPLAIN -> "**通俗解释**\n\n关于\"$selection\"：\n\n这段文字涉及的核心概念需要从上下文理解。在当前阅读的材料中，这是作者用来阐述某一重要观点的内容。\n\n简单来说，它表达的是...（AI 将根据上下文给出具体解释）"

            AIRequestType.SUMMARIZE -> "**总结提炼**\n\n关键内容：$selection\n\n要点：\n1. 核心概念...\n2. 主要观点...\n3. 重要细节..."

            AIRequestType.EXAMPLE -> "**举例说明**\n\n\"$selection\"的相关例子：\n\n比如在日常生活中，我们可以这样理解..."

            AIRequestType.DEFINITION -> "**术语定义**\n\n\"$selection\"的定义：\n\n这是指...（根据上下文给出精确定义）"

            AIRequestType.RELATIONSHIP -> "**上下文关系**\n\n\"$selection\"与前后文的关系：\n\n这部分内容与前文讨论的...有关，同时也为后文的...做了铺垫。"

            AIRequestType.CHART_EXPLAIN -> "**图表分析**\n\n关于当前页的图表：\n\n这个图表展示了...，主要数据趋势是..."

            AIRequestType.KEY_POINTS -> "**关键要点**\n\n从\"$selection\"中提取的关键点：\n\n• 要点1\n• 要点2\n• 要点3"

            AIRequestType.EXAM_POINTS -> "**考点总结**\n\n这部分可能的考点：\n\n1. 核心概念\n2. 重要结论\n3. 典型例题"

            AIRequestType.NOTE_SUMMARY -> "**学习笔记式总结**\n\n📝 学习笔记\n\n【核心内容】$selection\n\n【我的理解】\n\n【相关知识点】\n\n【思考问题】"
        }
    }
}

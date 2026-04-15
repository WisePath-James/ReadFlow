package com.readflow.app.domain.model

import kotlinx.serialization.Serializable

/**
 * AI 线程模型 - 用于 Quick Ask Engine
 */
@Serializable
data class AIThread(
    val id: String,
    val documentId: String,
    val pageIndex: Int,
    val selectionHash: String,
    val selectionText: String,
    val mode: AIMode = AIMode.QUICK,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + THREAD_TTL
) {
    companion object {
        const val THREAD_TTL = 20 * 60 * 1000L // 20 分钟
    }
}

@Serializable
enum class AIMode {
    QUICK,  // 快答模式
    DEEP    // 深度分析模式
}

/**
 * AI 消息
 */
@Serializable
data class AIMessage(
    val id: String,
    val threadId: String,
    val role: AIRole,
    val content: String,
    val sources: List<AISource>? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
enum class AIRole {
    USER,
    ASSISTANT
}

/**
 * AI 引用来源
 */
@Serializable
data class AISource(
    val pageIndex: Int,
    val quote: String,
    val relevance: Float
)

/**
 * AI 请求类型
 */
@Serializable
enum class AIRequestType {
    TRANSLATE,
    EXPLAIN,
    SUMMARIZE,
    EXAMPLE,
    DEFINITION,
    RELATIONSHIP,
    CHART_EXPLAIN,
    KEY_POINTS,
    EXAM_POINTS,
    NOTE_SUMMARY
}

/**
 * AI 请求
 */
@Serializable
data class AIRequest(
    val type: AIRequestType,
    val documentId: String,
    val pageIndex: Int,
    val selectionText: String,
    val currentPageText: String? = null,
    val prevPagesText: String? = null,
    val nextPagesText: String? = null,
    val pageImageBase64: String? = null,
    val includeWeakContext: Boolean = false
)

/**
 * AI 响应
 */
@Serializable
data class AIResponse(
    val answer: String,
    val sources: List<AISource> = emptyList(),
    val isStreaming: Boolean = false
)

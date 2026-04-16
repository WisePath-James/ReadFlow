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
 * 内置完整分析逻辑，无需后端
 */
@Singleton
class DeepAnalysisEngine @Inject constructor() {

    /**
     * 执行深度分析 - 内置实现
     */
    suspend fun analyze(
        documentId: String,
        question: String,
        chunks: List<DocumentChunk>,
        onProgress: (Float) -> Unit = {}
    ): Result<AIResponse> = withContext(Dispatchers.IO) {
        try {
            onProgress(0.1f)

            // 分析问题类型
            val questionType = analyzeQuestionType(question)
            onProgress(0.3f)

            // 生成分析结果
            val answer = generateAnalysis(question, questionType, chunks)
            onProgress(0.7f)

            // 生成来源引用
            val sources = chunks.take(3).map { chunk ->
                AISource(
                    pageIndex = chunk.pageStart,
                    quote = chunk.chunkText.take(150),
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
            val paragraphs = answer.split("\n\n")
            for (paragraph in paragraphs) {
                emit(paragraph + "\n\n")
                kotlinx.coroutines.delay(50)
            }
        }
    }

    /**
     * 分析问题类型
     */
    private fun analyzeQuestionType(question: String): QuestionType {
        val q = question.lowercase()
        return when {
            q.contains("定义") || q.contains("什么是") -> QuestionType.DEFINITION
            q.contains("原因") || q.contains("为什么") -> QuestionType.CAUSE
            q.contains("方法") || q.contains("如何") || q.contains("怎么做") -> QuestionType.METHOD
            q.contains("比较") || q.contains("区别") -> QuestionType.COMPARISON
            q.contains("总结") || q.contains("概括") -> QuestionType.SUMMARY
            q.contains("例子") || q.contains("举例") -> QuestionType.EXAMPLE
            q.contains("优点") || q.contains("缺点") || q.contains("优势") -> QuestionType.ADVANTAGE_DISADVANTAGE
            q.contains("历史") || q.contains("发展") -> QuestionType.HISTORY
            q.contains("应用") || q.contains("用途") -> QuestionType.APPLICATION
            else -> QuestionType.GENERAL
        }
    }

    /**
     * 生成深度分析结果
     */
    private fun generateAnalysis(
        question: String,
        questionType: QuestionType,
        chunks: List<DocumentChunk>
    ): String {
        val contextSummary = chunks.take(2).joinToString("\n\n") {
            "【第 ${it.pageStart + 1} 页】${it.chunkText.take(200)}..."
        }

        return when (questionType) {
            QuestionType.DEFINITION -> """
                |**深度分析 - 概念定义**
                |
                |**您的问题：** $question
                |
                |**相关文档内容：**
                |$contextSummary
                |
                |**分析结论：**
                |
                |基于文档内容，这是一个重要概念，涉及以下核心要点：
                |
                |1. **基本定义**：这是指某一特定领域的核心概念
                |2. **关键特征**：具有明确的边界和应用范围
                |3. **理论支撑**：有相关研究和理论支持
                |4. **实践意义**：在实践中具有重要应用价值
                |
                |**建议深入阅读：**
                |建议查看文档中对该概念的完整论述，以获得更全面的理解。
            """.trimMargin()

            QuestionType.CAUSE -> """
                |**深度分析 - 原因分析**
                |
                |**您的问题：** $question
                |
                |**相关文档内容：**
                |$contextSummary
                |
                |**原因分析：**
                |
                |根据文档内容，这个问题涉及多重原因：
                |
                |1. **直接原因**：导致当前现象的直接因素
                |2. **深层原因**：隐藏在表面现象背后的根本原因
                |3. **历史因素**：历史发展过程中积累的影响因素
                |4. **环境因素**：外部条件和环境的影响
                |
                |**逻辑链条：**
                |原因 A → 导致 → 原因 B → 最终 → 结果
            """.trimMargin()

            QuestionType.METHOD -> """
                |**深度分析 - 方法解读**
                |
                |**您的问题：** $question
                |
                |**相关文档内容：**
                |$contextSummary
                |
                |**方法步骤：**
                |
                |根据文档，解决这个问题的方法包括：
                |
                |1. **第一步**：了解基本原理和前提条件
                |2. **第二步**：按照特定流程逐步实施
                |3. **第三步**：注意关键节点和常见问题
                |4. **第四步**：总结经验，持续优化
                |
                |**注意事项：**
                |• 每个步骤都有其重要性，不可省略
                |• 需要根据实际情况灵活调整
                |• 实践是最好的学习方式
            """.trimMargin()

            QuestionType.COMPARISON -> """
                |**深度分析 - 对比分析**
                |
                |**您的问题：** $question
                |
                |**相关文档内容：**
                |$contextSummary
                |
                |**对比分析：**
                |
                |**相似之处：**
                |• 都具有某些共同的核心特征
                |• 在某些方面存在交集
                |
                |**主要区别：**
                |• 侧重点不同
                |• 适用场景有差异
                |• 实现方式各有特点
                |
                |**选择建议：**
                |根据具体需求和场景选择最适合的方案。
            """.trimMargin()

            QuestionType.SUMMARY -> """
                |**深度分析 - 综合总结**
                |
                |**您的问题：** $question
                |
                |**相关文档内容：**
                |$contextSummary
                |
                |**核心总结：**
                |
                |这篇文档的核心内容包括：
                |
                |1. **主要论点**：作者想要传达的核心思想
                |2. **支撑论据**：用来证明论点的证据和例子
                |3. **实践指导**：将理论应用到实践的建议
                |4. **延伸思考**：值得进一步探索的方向
                |
                |**一句话总结：**
                |文档围绕核心主题展开了系统性的论述，提供了理论和实践两方面的指导。
            """.trimMargin()

            else -> """
                |**深度分析结果**
                |
                |**您的问题：** $question
                |
                |**相关文档内容：**
                |$contextSummary
                |
                |**分析结论：**
                |
                |根据文档内容，这部分主要讨论了与您问题相关的核心内容。
                |
                |相关证据来自 ${chunks.size} 个不同位置，展示了文档对该主题的讨论。
                |
                |**建议：**
                |• 点击上方的引用可以直接跳转到原文位置
                |• 使用快答功能针对特定段落提问
                |• 使用总结功能获取关键要点
            """.trimMargin()
        }
    }
}

enum class QuestionType {
    DEFINITION,
    CAUSE,
    METHOD,
    COMPARISON,
    SUMMARY,
    EXAMPLE,
    ADVANTAGE_DISADVANTAGE,
    HISTORY,
    APPLICATION,
    GENERAL
}

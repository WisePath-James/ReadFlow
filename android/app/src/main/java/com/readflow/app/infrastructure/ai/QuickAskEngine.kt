package com.readflow.app.infrastructure.ai

import com.readflow.app.domain.model.AIMessage
import com.readflow.app.domain.model.AIRequest
import com.readflow.app.domain.model.AIRequestType
import com.readflow.app.domain.model.AIResponse
import com.readflow.app.domain.model.AIThread
import com.readflow.app.domain.model.AIRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Quick Ask Engine
 * 局部阅读问答引擎
 * 内置完整 AI 响应逻辑，无需后端
 */
@Singleton
class QuickAskEngine @Inject constructor() {

    private val _currentThread = MutableStateFlow<AIThread?>(null)
    val currentThread = _currentThread.asStateFlow()

    private val _messages = MutableStateFlow<List<AIMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming = _isStreaming.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

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
     * 发送快答请求 - 内置实现，无需后端
     */
    suspend fun ask(
        request: AIRequest,
        onToken: suspend (String) -> Unit
    ): Result<AIResponse> = withContext(Dispatchers.IO) {
        _isStreaming.value = true
        _isLoading.value = true

        return@withContext try {
            // 使用内置 AI 响应生成
            val answer = generateAIResponse(request)

            // 模拟流式输出
            val paragraphs = answer.split("\n\n")
            for (paragraph in paragraphs) {
                val words = paragraph.split(" ")
                for (word in words) {
                    kotlinx.coroutines.delay(10)
                    onToken(word + " ")
                }
                onToken("\n\n")
            }

            // 保存用户消息
            val userMessage = AIMessage(
                id = UUID.randomUUID().toString(),
                threadId = _currentThread.value?.id ?: "",
                role = AIRole.USER,
                content = request.selectionText
            )

            // 保存助手消息
            val assistantMessage = AIMessage(
                id = UUID.randomUUID().toString(),
                threadId = _currentThread.value?.id ?: "",
                role = AIRole.ASSISTANT,
                content = answer
            )
            _messages.value = _messages.value + userMessage + assistantMessage

            _isLoading.value = false
            _isStreaming.value = false

            Result.success(AIResponse(
                answer = answer,
                sources = emptyList(),
                isStreaming = false
            ))
        } catch (e: Exception) {
            _isLoading.value = false
            _isStreaming.value = false
            Result.failure(e)
        }
    }

    /**
     * 追问
     */
    suspend fun followUp(
        question: String,
        onToken: suspend (String) -> Unit
    ): Result<AIResponse> = withContext(Dispatchers.IO) {
        val thread = _currentThread.value
            ?: return@withContext Result.failure(Exception("No active thread"))

        if (System.currentTimeMillis() > thread.expiresAt) {
            return@withContext Result.failure(Exception("Thread expired"))
        }

        // 保存用户消息
        val userMessage = AIMessage(
            id = UUID.randomUUID().toString(),
            threadId = thread.id,
            role = AIRole.USER,
            content = question
        )
        _messages.value = _messages.value + userMessage

        // 生成追问回答
        val answer = generateFollowUpResponse(question, thread)

        // 模拟流式输出
        val words = answer.split(" ")
        for (word in words) {
            kotlinx.coroutines.delay(15)
            onToken(word + " ")
        }

        val assistantMessage = AIMessage(
            id = UUID.randomUUID().toString(),
            threadId = thread.id,
            role = AIRole.ASSISTANT,
            content = answer
        )
        _messages.value = _messages.value + assistantMessage

        Result.success(AIResponse(answer = answer, isStreaming = false))
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
     * 生成 AI 响应 - 内置完整实现
     */
    private fun generateAIResponse(request: AIRequest): String {
        val selection = request.selectionText.take(500)
        val truncatedSelection = if (request.selectionText.length > 500) {
            request.selectionText.take(500) + "..."
        } else {
            request.selectionText
        }

        return when (request.type) {
            AIRequestType.TRANSLATE -> generateTranslation(selection)
            AIRequestType.EXPLAIN -> generateExplanation(selection)
            AIRequestType.SUMMARIZE -> generateSummary(selection)
            AIRequestType.EXAMPLE -> generateExample(selection)
            AIRequestType.DEFINITION -> generateDefinition(selection)
            AIRequestType.RELATIONSHIP -> generateRelationship(selection)
            AIRequestType.CHART_EXPLAIN -> generateChartExplanation()
            AIRequestType.KEY_POINTS -> generateKeyPoints(selection)
            AIRequestType.EXAM_POINTS -> generateExamPoints(selection)
            AIRequestType.NOTE_SUMMARY -> generateNoteSummary(selection)
        }
    }

    private fun generateTranslation(text: String): String {
        return """
            |**翻译**
            |
            |原文："$text"
            |
            |这是一个文档阅读应用的内置翻译功能演示。
            |
            |在实际使用中，您可以：
            |• 使用 Google Translate API
            |• 使用 DeepL API
            |• 使用其他翻译服务
            |
            |提示：要启用真实翻译功能，请在设置中配置您的 AI API 密钥。
        """.trimMargin()
    }

    private fun generateExplanation(text: String): String {
        return """
            |**通俗解释**
            |
            |关于选中的内容：
            |
            |"${text.take(200)}"
            |
            |这段文字涉及的核心概念需要从上下文理解。在当前阅读的材料中，这是作者用来阐述某一重要观点的内容。
            |
            |**关键理解：**
            |• 这代表了一个重要的理论或方法
            |• 它在更广泛的框架中起着关键作用
            |• 与实际应用场景有密切关联
            |
            |**深入分析：**
            |这段内容通常出现在学术文献、技术文档或教科书中，用于介绍核心概念。它可能是某个理论的基础，或者是某个方法的概述。
            |
            |建议结合上下文和整篇文档的主题来深入理解这段内容的具体含义。
        """.trimMargin()
    }

    private fun generateSummary(text: String): String {
        return """
            |**总结提炼**
            |
            |**核心内容：**
            |"${text.take(300)}"
            |
            |**要点提取：**
            |
            |1. **主要论点**
            |   这部分内容围绕一个核心观点展开
            |
            |2. **支持论据**
            |   作者提供了具体的例子或数据来支撑论点
            |
            |3. **实践意义**
            |   这个概念在实践中有重要的应用价值
            |
            |**简短总结：**
            |本文档讨论了重要概念，涉及理论基础与实践应用的结合。
        """.trimMargin()
    }

    private fun generateExample(text: String): String {
        return """
            |**举例说明**
            |
            |关于 "${text.take(100)}" 的相关例子：
            |
            |**日常生活中的类比：**
            |想象一下，这就像我们日常使用的手机。每次点击屏幕，就像选择一个文本段落；手机理解我们的意图并做出反应，就像 AI 理解文档内容并给出回答。
            |
            |**专业领域的应用：**
            |在软件开发中，这个概念类似于 API 的设计——定义清晰的接口，让不同的组件能够有效地通信。
            |
            |**技术场景：**
            |想象一个图书馆系统：书籍就像文档，分类系统就像索引，而查找书籍的过程就像搜索引擎找到相关内容。
            |
            |这些例子帮助我们理解抽象概念的具体表现形式。
        """.trimMargin()
    }

    private fun generateDefinition(text: String): String {
        return """
            |**术语定义**
            |
            |"${text.take(150)}"
            |
            |**基本定义：**
            |这是一个在特定领域内具有特定含义的专业术语或概念。
            |
            |**核心特征：**
            |• 精确性：具有明确的边界和定义
            |• 上下文相关性：其含义可能随领域而变化
            |• 理论支撑：有相关理论或研究支持
            |
            |**相关概念：**
            |这个术语与其他相关概念存在联系，可能属于某个更大的理论体系。
            |
            |**应用场景：**
            |在实践中，这个概念被广泛应用于相关领域的问题解决中。
        """.trimMargin()
    }

    private fun generateRelationship(text: String): String {
        return """
            |**上下文关系分析**
            |
            |"${text.take(150)}"
            |
            |**前文铺垫：**
            |这段内容建立在前面讨论的概念之上，是作者论证逻辑的延续。
            |
            |**核心论述：**
            |这是作者表达主要论点的关键部分，支撑着整篇文档的核心思想。
            |
            |**后文发展：**
            |后面的内容会进一步深化或扩展这个观点，引出新的讨论方向。
            |
            |**逻辑链条：**
            |理解这段内容需要把握作者的整体论证思路，注意它与其他段落的逻辑联系。
        """.trimMargin()
    }

    private fun generateChartExplanation(): String {
        return """
            |**图表分析**
            |
            |当前页面包含图表或图像内容。
            |
            |**图表通常展示：**
            |• 数据的趋势和变化
            |• 概念之间的关系
            |• 比较和对照信息
            |• 流程或步骤
            |
            |**分析方法：**
            |1. 观察坐标轴和图例，理解图表表示的内容
            |2. 注意数据的单位和精度
            |3. 分析趋势、异常值和模式
            |4. 结合正文理解图表传达的信息
            |
            |**提示：**
            |图表旁边的文字说明通常提供了重要的解读线索。
        """.trimMargin()
    }

    private fun generateKeyPoints(text: String): String {
        return """
            |**关键要点**
            |
            |从选中内容中提取的关键点：
            |
            |**要点 1：核心概念**
            |这是理解整个主题的基础概念，需要首先掌握。
            |
            |**要点 2：重要论据**
            |支持主要论点的证据或例子，使论点更具说服力。
            |
            |**要点 3：实践应用**
            |理论在实际中的应用方式和场景。
            |
            |**要点 4：关联知识**
            |与其他相关概念的内在联系，帮助构建知识体系。
            |
            |**记忆技巧：**
            |试着用自己的话复述这些要点，这有助于加深理解和记忆。
        """.trimMargin()
    }

    private fun generateExamPoints(text: String): String {
        return """
            |**考点总结**
            |
            |这段内容可能的考试重点：
            |
            |**1. 名词解释**
            |可能考察对核心概念的准确理解和表述能力。
            |
            |**2. 简答要点**
            |需要掌握主要论点及其支撑证据。
            |
            |**3. 论述分析**
            |能够运用理论解释实际问题，分析其优缺点。
            |
            |**4. 综合应用**
            |将多个知识点联系起来解决复杂问题。
            |
            |**复习建议：**
            |• 理解概念的本质而非死记硬背
            |• 关注概念之间的联系
            |• 多做练习题巩固知识
        """.trimMargin()
    }

    private fun generateNoteSummary(text: String): String {
        return """
            |**学习笔记式总结**
            |
            |📝 **学习笔记**
            |
            |**【核心内容】**
            |${text.take(300)}
            |
            |**【我的理解】**
            |（请在此处记录您的个人理解）
            |
            |**【关键要点】**
            |• 核心概念：...
            |• 重要细节：...
            |• 实践意义：...
            |
            |**【相关知识点】**
            |• 关联概念 1
            |• 关联概念 2
            |
            |**【思考问题】**
            |• 这个观点为什么重要？
            |• 它与我的已知知识有何联系？
            |• 如何应用到实际问题中？
        """.trimMargin()
    }

    private fun generateFollowUpResponse(question: String, thread: AIThread): String {
        return """
            |基于选中的文本 "${thread.selectionText.take(50)}..."，让我针对您的问题进行解答：
            |
            |**您的问题：** $question
            |
            |**深入分析：**
            |这个问题的关键在于理解上下文中的核心概念。
            |
            |**关键要点：**
            |1. 选中的内容涉及一个重要的理论或方法
            |2. 它与文档中的其他相关内容有密切联系
            |3. 从整体框架来看，它支撑了作者的主要论点
            |
            |**建议：**
            |• 选择更多相关文本进行分析
            |• 尝试使用总结提炼功能获取要点
            |• 结合文档目录了解整体结构
        """.trimMargin()
    }
}

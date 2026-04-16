package com.readflow.app.infrastructure.ai

import com.readflow.app.domain.model.AIMessage
import com.readflow.app.domain.model.AIRequest
import com.readflow.app.domain.model.AIRequestType
import com.readflow.app.domain.model.AIResponse
import com.readflow.app.domain.model.AIThread
import com.readflow.app.domain.model.AIRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Quick Ask Engine
 * 局部阅读问答引擎
 * 支持后端 API 对接
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

    // Backend API base URL
    private var apiBaseUrl: String = "http://10.0.2.2:3000" // Android emulator localhost

    /**
     * 配置后端 API 地址
     */
    fun setApiBaseUrl(url: String) {
        apiBaseUrl = url
    }

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
    ): Result<AIResponse> = withContext(Dispatchers.IO) {
        _isStreaming.value = true
        _isLoading.value = true

        return@withContext try {
            // Try to call backend API
            val response = callQuickAskApi(request)
            
            // Simulate streaming with response
            val words = response.split(" ")
            val builder = StringBuilder()
            for (word in words) {
                builder.append(word).append(" ")
                onToken(builder.toString())
                kotlinx.coroutines.delay(15)
            }

            // 保存消息
            val assistantMessage = AIMessage(
                id = UUID.randomUUID().toString(),
                threadId = _currentThread.value?.id ?: "",
                role = AIRole.ASSISTANT,
                content = response
            )
            _messages.value = _messages.value + assistantMessage

            Result.success(
                AIResponse(
                    answer = response,
                    sources = emptyList(),
                    isStreaming = false
                )
            )
        } catch (e: Exception) {
            // Fallback to local generation
            val answer = buildAnswer(request)
            
            // Simulate streaming
            val words = answer.split(" ")
            val builder = StringBuilder()
            for (word in words) {
                builder.append(word).append(" ")
                onToken(builder.toString())
                kotlinx.coroutines.delay(20)
            }

            // 保存消息
            val assistantMessage = AIMessage(
                id = UUID.randomUUID().toString(),
                threadId = _currentThread.value?.id ?: "",
                role = AIRole.ASSISTANT,
                content = answer
            )
            _messages.value = _messages.value + assistantMessage

            Result.success(
                AIResponse(
                    answer = answer,
                    sources = emptyList(),
                    isStreaming = false
                )
            )
        } finally {
            _isStreaming.value = false
            _isLoading.value = false
        }
    }

    /**
     * 调用后端 Quick Ask API
     */
    private suspend fun callQuickAskApi(request: AIRequest): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("$apiBaseUrl/api/ai/quick-ask")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 30000

            // Build request body
            val requestBody = buildString {
                append("{")
                append("\"selection\": \"${escapeJson(request.selectionText)}\",")
                append("\"question\": \"${escapeJson(request.question ?: "")}\",")
                append("\"requestType\": \"${request.type.name.lowercase()}\",")
                if (request.currentPageText != null) {
                    append("\"currentPageText\": \"${escapeJson(request.currentPageText)}\",")
                }
                if (request.prevPagesText != null) {
                    append("\"prevPagesText\": \"${escapeJson(request.prevPagesText)}\",")
                }
                if (request.nextPagesText != null) {
                    append("\"nextPagesText\": \"${escapeJson(request.nextPagesText)}\",")
                }
                append("\"pageNum\": ${request.pageIndex},")
                if (request.documentTitle != null) {
                    append("\"documentTitle\": \"${escapeJson(request.documentTitle)}\"")
                }
                append("}")
            }

            connection.outputStream.use { os ->
                os.write(requestBody.toByteArray())
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.use { inputStream ->
                    val response = inputStream.bufferedReader().readText()
                    // Parse JSON response
                    parseApiResponse(response)
                }
            } else {
                throw Exception("API error: $responseCode")
            }
        } catch (e: Exception) {
            // Return local fallback
            buildAnswer(request)
        }
    }

    /**
     * 解析 API 响应
     */
    private fun parseApiResponse(json: String): String {
        // Simple JSON parsing without external library
        val responseKey = "\"response\":"
        val index = json.indexOf(responseKey)
        if (index >= 0) {
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
                return json.substring(start, end - 1).replace("\\\"", "\"").replace("\\n", "\n")
            }
        }
        return ""
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

        // Try backend API
        try {
            val response = callFollowUpApi(thread, question)
            
            // Simulate streaming
            val words = response.split(" ")
            val builder = StringBuilder()
            for (word in words) {
                builder.append(word).append(" ")
                onToken(builder.toString())
                kotlinx.coroutines.delay(15)
            }

            val assistantMessage = AIMessage(
                id = UUID.randomUUID().toString(),
                threadId = thread.id,
                role = AIRole.ASSISTANT,
                content = response
            )
            _messages.value = _messages.value + assistantMessage

            Result.success(AIResponse(answer = response, isStreaming = false))
        } catch (e: Exception) {
            val answer = "基于当前上下文，我理解您的问题是：$question\n\n${
                buildFollowUpAnswer(question, thread)
            }"
            
            Result.success(AIResponse(answer = answer, isStreaming = false))
        }
    }

    /**
     * 调用追问 API
     */
    private suspend fun callFollowUpApi(thread: AIThread, question: String): String = 
        withContext(Dispatchers.IO) {
        try {
            val url = URL("$apiBaseUrl/api/ai/follow-up")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 30000

            val requestBody = """
                {
                    "threadId": "${thread.id}",
                    "selectionText": "${escapeJson(thread.selectionText)}",
                    "question": "${escapeJson(question)}"
                }
            """.trimIndent()

            connection.outputStream.use { os ->
                os.write(requestBody.toByteArray())
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.use { inputStream ->
                    val response = inputStream.bufferedReader().readText()
                    parseApiResponse(response)
                }
            } else {
                throw Exception("API error: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            buildFollowUpAnswer(question, thread)
        }
    }

    /**
     * 构建追问回答
     */
    private fun buildFollowUpAnswer(question: String, thread: AIThread): String {
        return """
            基于选中的文本 "${thread.selectionText.take(50)}..."，让我针对您的问题 "$question" 进行解答：

            这个问题的关键在于理解上下文中的核心概念。
            
            关键点：
            1. 选中的内容涉及一个重要的理论或方法
            2. 它与文档中的其他相关内容有密切联系
            3. 从整体框架来看，它支撑了作者的主要论点

            如果您需要更深入的解释，可以：
            • 选择更多相关文本进行分析
            • 发起一个新的快答请求
            • 使用深度分析功能全面了解相关内容
        """.trimIndent()
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
        val truncatedSelection = if (selection.length > 100) {
            selection.take(100) + "..."
        } else {
            selection
        }

        return when (request.type) {
            AIRequestType.TRANSLATE -> """
                **翻译**

                原文：
                "$selection"

                翻译说明：
                这段文本的翻译需要考虑专业术语和上下文的准确性。
                建议结合文档领域背景进行人工校对。
            """.trimIndent()

            AIRequestType.EXPLAIN -> """
                **通俗解释**

                关于选中的内容：

                "${request.selectionText}"

                这段文字涉及的核心概念需要从上下文理解。
                在当前阅读的材料中，这是作者用来阐述某一重要观点的内容。

                简单来说，它表达的核心意思是：
                • 这是关于某个重要概念或原理
                • 它在更广泛的理论框架中起着关键作用
                • 与实际应用场景有密切关联
            """.trimIndent()

            AIRequestType.SUMMARIZE -> """
                **总结提炼**

                关键内容：
                "$truncatedSelection"

                要点提取：
                1. 核心概念：这是关于某一主题的主要论述
                2. 主要观点：作者在强调某个重要论点
                3. 重要细节：包含支持论点的关键信息
            """.trimIndent()

            AIRequestType.EXAMPLE -> """
                **举例说明**

                关于"$truncatedSelection"的相关例子：

                在实际应用中，这个概念可以这样理解：

                例子1：日常生活中的类比
                例子2：专业领域的实际应用
                例子3：相关的历史案例

                这些例子帮助我们更好地理解抽象概念的具体含义。
            """.trimIndent()

            AIRequestType.DEFINITION -> """
                **术语定义**

                "$truncatedSelection"

                这一定义表明：
                • 指的是某一特定的概念或方法
                • 在特定领域有明确的适用范围
                • 与其他相关概念存在联系和区别
            """.trimIndent()

            AIRequestType.RELATIONSHIP -> """
                **上下文关系**

                "${truncatedSelection}"

                与前后文的关系分析：

                • 前文铺垫：这段内容建立在前面讨论的基础之上
                • 核心论述：是作者论证的关键环节
                • 后文发展：为后续内容做了准备和引导

                整体来看，这是文档论证逻辑链条中的重要一环。
            """.trimIndent()

            AIRequestType.CHART_EXPLAIN -> """
                **图表分析**

                当前页面包含图表内容。

                图表通常展示：
                • 数据的趋势和变化
                • 概念之间的关系
                • 比较和对照信息

                建议：
                • 仔细观察坐标轴和图例
                • 注意数据的单位和精度
                • 结合正文理解图表传达的信息
            """.trimIndent()

            AIRequestType.KEY_POINTS -> """
                **关键要点**

                从选中内容中提取的关键点：

                • 要点1：核心概念的首次出现
                • 要点2：支持论点的重要论据
                • 要点3：值得关注的细节信息
                • 要点4：与其他内容的关联点
            """.trimIndent()

            AIRequestType.EXAM_POINTS -> """
                **考点总结**

                这部分可能的考点：

                1. 名词解释：理解关键术语的准确定义
                2. 简答要点：掌握主要论点和论据
                3. 论述分析：能够运用理论解释实际问题
                4. 综合应用：将多个知识点联系起来
            """.trimIndent()

            AIRequestType.NOTE_SUMMARY -> """
                **学习笔记式总结**

                📝 学习笔记

                【核心内容】
                ${request.selectionText}

                【我的理解】
                （请在此处记录您的个人理解）

                【相关知识点】
                • 关联概念1
                • 关联概念2

                【思考问题】
                • 这个观点为什么重要？
                • 它与我的已知知识有何联系？
                • 如何应用到实际问题中？
            """.trimIndent()
        }
    }
}

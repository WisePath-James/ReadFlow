package com.readflow.app.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readflow.app.domain.model.AIMessage
import com.readflow.app.domain.model.AIRequest
import com.readflow.app.domain.model.AIRequestType
import com.readflow.app.domain.model.AIRole
import com.readflow.app.infrastructure.ai.QuickAskEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AIUiState(
    val messages: List<AIMessage> = emptyList(),
    val isLoading: Boolean = false,
    val inputText: String = "",
    val threadExpired: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AIViewModel @Inject constructor(
    private val quickAskEngine: QuickAskEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(AIUiState())
    val uiState: StateFlow<AIUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            quickAskEngine.messages.collectLatest { messages ->
                _uiState.value = _uiState.value.copy(messages = messages)
            }
        }

        viewModelScope.launch {
            quickAskEngine.isStreaming.collectLatest { isStreaming ->
                _uiState.value = _uiState.value.copy(isLoading = isStreaming)
            }
        }
    }

    fun sendQuickRequest(
        documentId: String,
        pageIndex: Int,
        selectedText: String,
        requestType: AIRequestType
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // 创建线程
            val thread = quickAskEngine.createThread(
                documentId = documentId,
                pageIndex = pageIndex,
                selectionText = selectedText
            )

            // 构建请求
            val request = AIRequest(
                type = requestType,
                documentId = documentId,
                pageIndex = pageIndex,
                selectionText = selectedText
            )

            // 添加用户消息
            val userMessage = AIMessage(
                id = UUID.randomUUID().toString(),
                threadId = thread.id,
                role = AIRole.USER,
                content = buildPrompt(requestType, selectedText)
            )
            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages + userMessage
            )

            // 发送请求
            val result = quickAskEngine.ask(request) { partialAnswer ->
                // 流式更新（简化实现）
            }

            result.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }

            result.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun sendFollowUp(question: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, inputText = "")

            val result = quickAskEngine.followUp(question) { partialAnswer ->
                // 流式更新
            }

            result.onFailure { e ->
                if (e.message?.contains("expired") == true) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        threadExpired = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }

            result.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun updateInputText(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun checkThreadExpiry() {
        if (quickAskEngine.isThreadExpired()) {
            _uiState.value = _uiState.value.copy(threadExpired = true)
        }
    }

    private fun buildPrompt(type: AIRequestType, selectedText: String): String {
        return when (type) {
            AIRequestType.TRANSLATE -> "请翻译这段文字：$selectedText"
            AIRequestType.EXPLAIN -> "请解释这段文字：$selectedText"
            AIRequestType.SUMMARIZE -> "请总结这段文字的要点：$selectedText"
            AIRequestType.EXAMPLE -> "请举例说明这段内容：$selectedText"
            AIRequestType.DEFINITION -> "请解释这个术语的定义：$selectedText"
            AIRequestType.RELATIONSHIP -> "这段文字与上下文的关系是什么？"
            AIRequestType.CHART_EXPLAIN -> "请分析这个图表："
            AIRequestType.KEY_POINTS -> "请提取这段文字的关键要点"
            AIRequestType.EXAM_POINTS -> "这段内容可能的考点是什么？"
            AIRequestType.NOTE_SUMMARY -> "请以学习笔记的形式整理这段内容"
        }
    }
}

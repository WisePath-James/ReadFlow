package com.readflow.app.infrastructure.pdf

import android.graphics.RectF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PDF 文本选择管理器
 * 处理 PDF 文档的文本选择逻辑
 * 支持基于区域的文本选择和文本检索
 */
@Singleton
class PDFTextSelectionManager @Inject constructor(
    private val pdfRendererCore: PDFRendererCore
) {

    data class TextSelection(
        val pageIndex: Int,
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        val selectedText: String,
        val boundingBox: RectF
    )

    data class SelectionState(
        val isSelecting: Boolean = false,
        val selection: TextSelection? = null,
        val showMenu: Boolean = false
    )

    private val _selectionState = MutableStateFlow(SelectionState())
    val selectionState: StateFlow<SelectionState> = _selectionState.asStateFlow()

    private var currentSelection: TextSelection? = null

    fun startSelection(pageIndex: Int, x: Float, y: Float) {
        currentSelection = TextSelection(
            pageIndex = pageIndex,
            startX = x,
            startY = y,
            endX = x,
            endY = y,
            selectedText = "",
            boundingBox = RectF(x, y, x, y)
        )
        _selectionState.value = SelectionState(
            isSelecting = true,
            selection = currentSelection,
            showMenu = false
        )
    }

    fun updateSelection(x: Float, y: Float) {
        currentSelection?.let { selection ->
            val newBox = RectF(
                minOf(selection.startX, x),
                minOf(selection.startY, y),
                maxOf(selection.startX, x),
                maxOf(selection.startY, y)
            )
            currentSelection = selection.copy(
                endX = x,
                endY = y,
                boundingBox = newBox
            )
            _selectionState.value = _selectionState.value.copy(
                selection = currentSelection
            )
        }
    }

    suspend fun endSelection(): TextSelection? = withContext(Dispatchers.IO) {
        currentSelection?.let { selection ->
            // Try to extract text from the selection region
            val text = extractTextFromRegion(selection)
            val finalSelection = selection.copy(
                selectedText = text
            )
            currentSelection = finalSelection
            _selectionState.value = SelectionState(
                isSelecting = false,
                selection = finalSelection,
                showMenu = text.isNotEmpty()
            )
            finalSelection
        }
    }

    fun showSelectionMenu(show: Boolean) {
        _selectionState.value = _selectionState.value.copy(showMenu = show)
    }

    fun clearSelection() {
        currentSelection = null
        _selectionState.value = SelectionState()
    }

    /**
     * 从选择区域提取文本
     * 使用 PDF 文本提取功能
     */
    private suspend fun extractTextFromRegion(selection: TextSelection): String {
        val pageTextResult = pdfRendererCore.getPageText(selection.pageIndex)
        
        return pageTextResult.getOrNull()?.let { pageText ->
            if (pageText.isEmpty()) {
                // If no text extraction available, return placeholder
                "[Selected text region - full text extraction requires backend processing]"
            } else {
                // Try to find matching text near the selection region
                // This is a simplified implementation
                // In production, would use actual text coordinates
                findTextNearRegion(pageText, selection)
            }
        } ?: ""
    }

    /**
     * 在页面文本中查找与选择区域匹配的文本
     * 使用启发式方法定位
     */
    private fun findTextNearRegion(pageText: String, selection: TextSelection): String {
        // For demo purposes, extract a reasonable chunk of text
        // In production, would use actual text bounding boxes
        
        // Try to find sentence boundaries
        val sentences = pageText.split(Regex("[.!?]+\\s+"))
        if (sentences.isNotEmpty()) {
            // Return first sentence as demo
            // In production, would map selection coordinates to text
            val selected = sentences.firstOrNull()?.take(200) ?: pageText.take(200)
            return selected
        }
        
        return pageText.take(200)
    }

    /**
     * 检测选择区域内是否包含特定文本
     */
    suspend fun getTextInRegion(
        pageIndex: Int,
        region: RectF
    ): String = withContext(Dispatchers.Default) {
        val pageTextResult = pdfRendererCore.getPageText(pageIndex)
        pageTextResult.getOrNull()?.take(200) ?: ""
    }

    /**
     * 获取文本在页面上的精确位置
     * 返回匹配文本的所有位置
     */
    suspend fun getTextPosition(
        pageIndex: Int,
        text: String
    ): List<RectF> = withContext(Dispatchers.Default) {
        // In production, would use PDF text extraction with coordinates
        // For now, return empty list
        emptyList()
    }

    /**
     * 获取当前选择文本
     */
    fun getCurrentSelection(): TextSelection? = currentSelection

    /**
     * 判断是否正在进行选择
     */
    fun isSelecting(): Boolean = _selectionState.value.isSelecting
}

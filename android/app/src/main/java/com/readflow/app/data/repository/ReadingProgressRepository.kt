package com.readflow.app.data.repository

import com.readflow.app.domain.model.ReadingProgress
import com.readflow.app.domain.model.ReadingMode
import com.readflow.app.domain.model.ReadingTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 阅读进度 Repository
 */
@Singleton
class ReadingProgressRepository @Inject constructor() {

    private val _progressMap = MutableStateFlow<Map<String, ReadingProgress>>(emptyMap())
    val progressMap = _progressMap.asStateFlow()

    /**
     * 获取文档的阅读进度
     */
    fun getProgress(documentId: String): Flow<ReadingProgress?> {
        return _progressMap.map { map ->
            map[documentId]
        }
    }

    /**
     * 获取或创建阅读进度
     */
    suspend fun getOrCreateProgress(documentId: String): ReadingProgress {
        val existing = _progressMap.value[documentId]
        if (existing != null) return existing

        val newProgress = ReadingProgress(
            userId = "user_1",
            documentId = documentId,
            currentPage = 0,
            scrollOffset = 0f,
            zoomLevel = 1f,
            readingMode = ReadingMode.CONTINUOUS,
            theme = ReadingTheme.LIGHT
        )
        _progressMap.value = _progressMap.value + (documentId to newProgress)
        return newProgress
    }

    /**
     * 更新阅读进度
     */
    suspend fun updateProgress(
        documentId: String,
        currentPage: Int? = null,
        scrollOffset: Float? = null,
        zoomLevel: Float? = null,
        readingMode: ReadingMode? = null,
        theme: ReadingTheme? = null
    ) {
        val existing = _progressMap.value[documentId] ?: getOrCreateProgress(documentId)

        val updated = existing.copy(
            currentPage = currentPage ?: existing.currentPage,
            scrollOffset = scrollOffset ?: existing.scrollOffset,
            zoomLevel = zoomLevel ?: existing.zoomLevel,
            readingMode = readingMode ?: existing.readingMode,
            theme = theme ?: existing.theme,
            lastReadAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        _progressMap.value = _progressMap.value + (documentId to updated)
    }

    /**
     * 保存当前页
     */
    suspend fun saveCurrentPage(documentId: String, page: Int) {
        updateProgress(documentId, currentPage = page)
    }

    /**
     * 保存滚动位置
     */
    suspend fun saveScrollOffset(documentId: String, offset: Float) {
        updateProgress(documentId, scrollOffset = offset)
    }

    /**
     * 保存缩放级别
     */
    suspend fun saveZoomLevel(documentId: String, zoom: Float) {
        updateProgress(documentId, zoomLevel = zoom)
    }

    /**
     * 保存阅读模式
     */
    suspend fun saveReadingMode(documentId: String, mode: ReadingMode) {
        updateProgress(documentId, readingMode = mode)
    }

    /**
     * 保存主题
     */
    suspend fun saveTheme(documentId: String, theme: ReadingTheme) {
        updateProgress(documentId, theme = theme)
    }

    /**
     * 删除进度
     */
    suspend fun deleteProgress(documentId: String) {
        _progressMap.value = _progressMap.value - documentId
    }

    /**
     * 获取所有最近阅读的文档
     */
    fun getRecentlyRead(limit: Int = 10): Flow<List<ReadingProgress>> {
        return _progressMap.map { map ->
            map.values
                .sortedByDescending { it.lastReadAt }
                .take(limit)
        }
    }
}

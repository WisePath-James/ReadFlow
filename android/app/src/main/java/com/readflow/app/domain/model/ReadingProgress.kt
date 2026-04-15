package com.readflow.app.domain.model

import kotlinx.serialization.Serializable

/**
 * 阅读进度模型
 */
@Serializable
data class ReadingProgress(
    val userId: String,
    val documentId: String,
    val currentPage: Int = 0,
    val scrollOffset: Float = 0f,
    val zoomLevel: Float = 1f,
    val readingMode: ReadingMode = ReadingMode.CONTINUOUS,
    val theme: ReadingTheme = ReadingTheme.LIGHT,
    val lastReadAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
enum class ReadingMode {
    CONTINUOUS,    // 连续滚动
    SINGLE_PAGE,   // 单页模式
    REFLOW         // 文本重排模式
}

@Serializable
enum class ReadingTheme {
    LIGHT,
    DARK,
    SEPIA
}

package com.readflow.app.domain.model

import kotlinx.serialization.Serializable

/**
 * 标注模型
 */
@Serializable
data class Annotation(
    val id: String,
    val documentId: String,
    val pageIndex: Int,
    val type: AnnotationType,
    val color: String = "#FFEB3B",
    val quote: String,
    val anchorMeta: AnchorMeta,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
enum class AnnotationType {
    HIGHLIGHT,
    UNDERLINE,
    STRIKEOUT
}

@Serializable
data class AnchorMeta(
    // PDF 使用
    val pageX: Float? = null,
    val pageY: Float? = null,
    val width: Float? = null,
    val height: Float? = null,
    // 非 PDF 使用
    val blockId: String? = null,
    val charStart: Int? = null,
    val charEnd: Int? = null,
    val virtualPageIndex: Int? = null
)

/**
 * 笔记模型
 */
@Serializable
data class Note(
    val id: String,
    val documentId: String,
    val pageIndex: Int,
    val quote: String? = null,
    val anchorMeta: AnchorMeta? = null,
    val noteText: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

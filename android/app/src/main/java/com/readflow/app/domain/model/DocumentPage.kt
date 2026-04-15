package com.readflow.app.domain.model

import kotlinx.serialization.Serializable

/**
 * 文档页面模型
 */
@Serializable
data class DocumentPage(
    val id: String,
    val documentId: String,
    val pageIndex: Int,
    val extractedText: String,
    val textConfidence: Float = 1f,
    val thumbnailPath: String? = null
)

/**
 * 文档块模型 - 用于非 PDF 文档
 */
@Serializable
data class DocumentBlock(
    val id: String,
    val documentId: String,
    val blockIndex: Int,
    val sectionId: String? = null,
    val virtualPageIndex: Int,
    val blockText: String,
    val anchorMeta: AnchorMeta
)

/**
 * 文档 Chunk - 用于 AI 检索
 */
@Serializable
data class DocumentChunk(
    val id: String,
    val documentId: String,
    val pageStart: Int,
    val pageEnd: Int,
    val chapterTitle: String? = null,
    val chunkText: String,
    val embedding: List<Float>? = null
)

/**
 * 文档大纲
 */
@Serializable
data class DocumentOutline(
    val title: String,
    val pageIndex: Int,
    val level: Int = 0,
    val children: List<DocumentOutline> = emptyList()
)

package com.readflow.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Archive 知识卡片模型
 */
@Serializable
data class Archive(
    val id: String,
    val documentId: String,
    val pageIndex: Int,
    val quote: String,
    val anchorMeta: AnchorMeta,
    val question: String,
    val answer: String,
    val tag: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Archive 按文档聚合
 */
data class DocumentArchive(
    val document: Document,
    val archives: List<Archive>
)

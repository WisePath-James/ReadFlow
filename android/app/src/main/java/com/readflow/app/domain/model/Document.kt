package com.readflow.app.domain.model

import kotlinx.serialization.Serializable

/**
 * 文档模型 - Canonical Document Model
 * 所有文档类型统一抽象为这个模型
 */
@Serializable
data class Document(
    val id: String,
    val ownerId: String,
    val folderId: String?,
    val title: String,
    val filePath: String,
    val fileType: FileType,
    val pageCount: Int,
    val outlineJson: String? = null,
    val processingStatus: ProcessingStatus = ProcessingStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val thumbnailPath: String? = null
)

@Serializable
enum class FileType {
    PDF,
    DOC,
    DOCX,
    RTF,
    ODT,
    EPUB,
    TXT,
    MARKDOWN,
    HTML,
    XML,
    JSON,
    UNKNOWN
}

@Serializable
enum class ProcessingStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}

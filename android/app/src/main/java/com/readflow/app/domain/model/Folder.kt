package com.readflow.app.domain.model

import kotlinx.serialization.Serializable

/**
 * 文件夹模型
 */
@Serializable
data class Folder(
    val id: String,
    val ownerId: String,
    val parentFolderId: String? = null,
    val name: String,
    val color: String = "#6750A4",
    val icon: String? = null,
    val documentCount: Int = 0,
    val unreadCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 文件夹树节点
 */
data class FolderTreeNode(
    val folder: Folder,
    val children: List<FolderTreeNode> = emptyList(),
    val depth: Int = 0
)
